package com.psl.tabdisplay

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.psl.seuicfixedreader.MQTT.MQTTConnection
import com.psl.seuicfixedreader.MQTT.MqttConnectionCallBack
import com.psl.seuicfixedreader.MQTT.MqttPublisher
import com.psl.seuicfixedreader.MQTT.MqttResponseCallback
import com.psl.seuicfixedreader.MQTT.MqttSubscriber
import com.psl.tabdisplay.APIHelpers.APIConstants
import com.psl.tabdisplay.APIHelpers.APIService
import com.psl.tabdisplay.adapters.WorkOrderDetailsAdapter
import com.psl.tabdisplay.database.DBHandler
import com.psl.tabdisplay.databinding.ActivityWorkorderDisplayBinding
import com.psl.tabdisplay.helper.AssetUtils
import com.psl.tabdisplay.helper.ConnectionManager
import com.psl.tabdisplay.helper.SharedPreferencesManager
import com.psl.tabdisplay.models.OrderDetails
import com.psl.tabdisplay.models.dataModels
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.eclipse.paho.mqttv5.client.IMqttToken
import org.eclipse.paho.mqttv5.client.MqttCallback
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse
import org.eclipse.paho.mqttv5.common.MqttException
import org.eclipse.paho.mqttv5.common.MqttMessage
import org.eclipse.paho.mqttv5.common.packet.MqttProperties
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.UUID
import java.util.concurrent.TimeUnit

class WorkorderDisplayActivity : AppCompatActivity(), ConnectionManager.ConnectionListener {
    private var context: Context = this
    private lateinit var binding: ActivityWorkorderDisplayBinding
    private lateinit var cd: ConnectionManager
    private lateinit var sharedPreferencesManager: SharedPreferencesManager
    private var POLLING_TIMER: Int = 5000
    private var workOrderType: String = ""
    private var workOrderNo: String = ""
    private var workOrderStatus: String = ""
    private lateinit var workorderFetchingHandler: Handler
    private lateinit var workorderFetchingRunnable: Runnable
    private lateinit var workOrderDetailsRecAdapter: WorkOrderDetailsAdapter
    private lateinit var workOrderDetailsDisAdapter: WorkOrderDetailsAdapter
    private var orderDetailsList: MutableList<OrderDetails> = mutableListOf()
    private var recOrderDetailsList: MutableList<OrderDetails> = mutableListOf()
    private var disOrderDetailsList: MutableList<OrderDetails> = mutableListOf()
    private var IS_ON: Boolean = false
    private var IS_API_CALL_IS_IN_PROGRESS: Boolean = false
    private var bottomSheetDialog: BottomSheetDialog? = null
    private var mqttConnection: MQTTConnection = MQTTConnection()
    private var mqttPub: MqttPublisher = MqttPublisher()
    private var mqttSub: MqttSubscriber = MqttSubscriber()
    private var topics: HashMap<String, String>? = null
    private lateinit var confirmationDialog : Dialog
    private lateinit var customConfirmationDialog : Dialog
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var db : DBHandler
    private var timerDateTime : String = "";
    private var statusDateTime : String = "";
    private var binDateTime : String = "";
    private var Is_Popup_Showing : Boolean = false
    private var Is_App_Running : Boolean = false
    private var Is_Bin_Running : Boolean = false
    private var job: Job? = null
    private var currentBinDestination = "__"
    private var isPostingInventory : Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_workorder_display)

        db = DBHandler(context)
        cd = ConnectionManager(this, this)
        cd.registerNetworkCallback()
        sharedPreferencesManager = SharedPreferencesManager(context)

        topics = intent.getSerializableExtra("TOPIC_LIST") as? HashMap<String, String>
        topics?.forEach { (title, topicName) ->
            Log.e("DATA_RECEIVED", "Title: $title, TopicName: $topicName")
        }
        binding.btnlogOut.setOnClickListener {
            showCustomConfirmationDialog("Do you want to Log out?","BACK");
        }
        POLLING_TIMER = sharedPreferencesManager.getPollingTimer()

        setOnOffButtonClickListener()
        initAdapter()

        binding.textDestination.text = ""
        binding.textPalletNo.text = ""
        startWorkorderFetchingHandler()
        Log.e("Lifecycle", "onCreate() called")
        //startTimerCheck()
    }

    @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n", "SuspiciousIndentation")
    private fun setOnOffButtonClickListener() {
        binding.btnOnOff.thumbDrawable = ContextCompat.getDrawable(this, R.drawable.custom_switch_thumb)
            binding.btnOnOff.setOnCheckedChangeListener  { _, isChecked ->
                if(mqttConnection.isConnected()){
                IS_ON = isChecked

                val action = if (IS_ON) "ON" else "OFF"
                startReader(action)
                }
                else{
                    AssetUtils.showCommonBottomSheetErrorDialog(context, "Please wait, the server is not connected yet")
                    binding.btnOnOff.isChecked = !isChecked
                }

        }
    }

    private fun initAdapter() {
        binding.rvPallet.layoutManager = GridLayoutManager(context, 1)
        workOrderDetailsRecAdapter =
            WorkOrderDetailsAdapter(context, recOrderDetailsList, workOrderType)
        binding.rvPallet.adapter = workOrderDetailsRecAdapter

        binding.disPallet.layoutManager = GridLayoutManager(context, 1)
        workOrderDetailsDisAdapter =
            WorkOrderDetailsAdapter(context, disOrderDetailsList, workOrderType)
        binding.disPallet.adapter = workOrderDetailsDisAdapter

    }

    private fun startWorkorderFetchingHandler() {
        workorderFetchingHandler = Handler(Looper.getMainLooper())
        workorderFetchingRunnable = object : Runnable {
            override fun run() {
                if (cd.isConnectedToNetwork()) {
                    GetWorkDetailsTask()
                }
                workorderFetchingHandler.postDelayed(this, POLLING_TIMER.toLong())
            }
        }
        workorderFetchingHandler.postDelayed(workorderFetchingRunnable, 3000)
    }

    private fun stopHandler() {
        CoroutineScope(Dispatchers.IO).launch {
            workorderFetchingHandler.removeCallbacks(workorderFetchingRunnable)
            handler.removeCallbacks(dismissPopupRunnable)
            job?.cancel()
        }
    }

    @SuppressLint("SuspiciousIndentation")
    private fun GetWorkDetailsTask() {
            try {
                val jsonObject = JSONObject()
                jsonObject.put(APIConstants.K_DEVICE_ID, sharedPreferencesManager.getDeviceId())

                val requestBody =
                    jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())

                val loggingInterceptor = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }

                val okHttpClient = OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .connectTimeout(APIConstants.API_TIMEOUT.toLong(), TimeUnit.SECONDS)
                    .readTimeout(APIConstants.API_TIMEOUT.toLong(), TimeUnit.SECONDS)
                    .writeTimeout(APIConstants.API_TIMEOUT.toLong(), TimeUnit.SECONDS)
                    .build()

                val retrofit: Retrofit = Retrofit.Builder()
                    .baseUrl(sharedPreferencesManager.getHostUrl().toString())
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(okHttpClient)
                    .build()

                Log.e(
                    "URL",
                    "Base URL: ${
                        sharedPreferencesManager.getHostUrl().toString()
                    }${APIConstants.M_GET_WORK_ORDER_DETAILS}"
                )
                val apiService1: APIService = retrofit.create(APIService::class.java)
                apiService1.getWorkorder(requestBody)
                    .enqueue(object : Callback<JsonObject> {
                        override fun onResponse(
                            call: Call<JsonObject>,
                            response: Response<JsonObject>
                        ) {
                            try {
                                if (response.isSuccessful) {
                                    response.body()?.takeIf { it.has(APIConstants.K_STATUS) }
                                        ?.let { jsonObject ->
                                            val status =
                                                jsonObject.get(APIConstants.K_STATUS)?.asBoolean
                                                    ?: false
                                            val message =
                                                jsonObject.get(APIConstants.K_MESSAGE)?.asString
                                                    ?: ""
                                            if (status) {
                                                parseWorkDetailsObjectAndDoAction(jsonObject)
                                            } else {
                                                AssetUtils.showCommonBottomSheetErrorDialog(
                                                    context,
                                                    message
                                                )
                                            }
                                        }
                                } else {
                                    Log.e(
                                        "HTTP_ERROR",
                                        "HTTP Error Code: ${response.code()} - ${
                                            response.errorBody()?.string()
                                        }"
                                    )
                                    AssetUtils.showCommonBottomSheetErrorDialog(
                                        context,
                                        "Response Error(${response.code()}): ${
                                            response.errorBody()?.string()
                                        }"
                                    )
                                }
                            } catch (e: Exception) {
                                Log.e(
                                    "RESPONSE_ERROR",
                                    "Error handling response: ${e.localizedMessage}"
                                )
                                AssetUtils.showCommonBottomSheetErrorDialog(
                                    context,
                                    "An error occurred while processing the response."
                                )
                            }
                        }

                        override fun onFailure(
                            call: Call<JsonObject>,
                            t: Throwable
                        ) {
                            when (t) {
                                is SocketTimeoutException -> {
                                    AssetUtils.showCommonBottomSheetErrorDialog(
                                        context,
                                        resources.getString(R.string.TimeOutError)
                                    )
                                }

                                is UnknownHostException -> {
                                    AssetUtils.showCommonBottomSheetErrorDialog(
                                        context,
                                        resources.getString(R.string.internet_error)
                                    )
                                }

                                is ConnectException -> {
                                    AssetUtils.showCommonBottomSheetErrorDialog(
                                        context,
                                        resources.getString(R.string.communication_error)
                                    )
                                }

                                else -> {
                                    Log.e("NETWORK_ERROR", "Network Failure: ${t.localizedMessage}")
                                    AssetUtils.showCommonBottomSheetErrorDialog(
                                        context,
                                        "Network Failure: ${t.localizedMessage}."
                                    )
                                }

                            }
                        }

                    })
            } catch (ex: Exception) {
                Log.e("LOGIN_ERROR", "Unexpected Error: ${ex.localizedMessage}")
                AssetUtils.showCommonBottomSheetErrorDialog(context, "Something Went Wrong.")
            }
    }

    private fun parseWorkDetailsObjectAndDoAction(jsonObject: JsonObject) {
        try {
            Log.e("Data", jsonObject.toString())
            jsonObject.takeIf { it.has(APIConstants.K_DATA) }?.let {
                val dataObject = it.getAsJsonObject(APIConstants.K_DATA)
                Log.e("DataObject", dataObject.toString())
                orderDetailsList.clear()
                recOrderDetailsList.clear()
                disOrderDetailsList.clear()
                dataObject.takeIf { it.has(APIConstants.K_POLLING_TIMER) }?.let {
                    POLLING_TIMER = dataObject.get(APIConstants.K_POLLING_TIMER)
                        ?.takeIf { !it.isJsonNull }?.asInt ?: 5000
                    sharedPreferencesManager.setPollingTimer(POLLING_TIMER)
                }
                dataObject.takeIf { it.has(APIConstants.K_WORK_ORDER_DETAILS_ARRAY) }?.let {
                    val jsonArray =
                        dataObject.getAsJsonArray(APIConstants.K_WORK_ORDER_DETAILS_ARRAY)
                    for (i in 0 until jsonArray.size()) {
                        var detailObj = jsonArray.get(i).asJsonObject
                        val orderDetails = OrderDetails()
                        detailObj.takeIf { it.has(APIConstants.K_WORK_ORDER_NUMBER) }?.let {
                            workOrderNo = detailObj.get(APIConstants.K_WORK_ORDER_NUMBER)
                                ?.takeIf { !it.isJsonNull }?.asString ?: ""
                            orderDetails.workorderNo = workOrderNo
                        }
                        detailObj.takeIf { it.has(APIConstants.K_PALLET_NUMBER) }?.let {
                            orderDetails.palletNumber = detailObj.get(APIConstants.K_PALLET_NUMBER)
                                ?.takeIf { !it.isJsonNull }?.asString ?: ""
                        }
                        detailObj.takeIf { it.has(APIConstants.K_PALLET_TAG_ID) }?.let {
                            orderDetails.palletTagID = detailObj.get(APIConstants.K_PALLET_TAG_ID)
                                ?.takeIf { !it.isJsonNull }?.asString ?: ""
                        }
                        detailObj.takeIf { it.has(APIConstants.K_WORK_ORDER_TYPE) }?.let {
                            workOrderType = detailObj.get(APIConstants.K_WORK_ORDER_TYPE)
                                ?.takeIf { !it.isJsonNull }?.asString ?: ""
                            orderDetails.workorderType = workOrderType
                            orderDetails.pickupLocation = when (workOrderType) {
                                "U0", "U1", "L1" -> detailObj.get(APIConstants.K_LOCATION_NAME)
                                    ?.takeIf { !it.isJsonNull }?.asString ?: ""

                                "L0" -> detailObj.get(APIConstants.K_BIN_LOCATION)
                                    ?.takeIf { !it.isJsonNull }?.asString ?: ""

                                "I0" -> detailObj.get(APIConstants.K_LOADING_AREA)
                                    ?.takeIf { !it.isJsonNull }?.asString ?: ""

                                else -> ""
                            }
                        }
                        detailObj.takeIf { it.has(APIConstants.K_LOADING_AREA) }?.let{
                            orderDetails.loadingAreaName = detailObj.get(APIConstants.K_LOADING_AREA)
                                ?.takeIf { !it.isJsonNull }?.asString ?: ""
                        }
                        detailObj.takeIf { it.has(APIConstants.K_BIN_LOCATION) }?.let{
                            orderDetails.binLocation = detailObj.get(APIConstants.K_BIN_LOCATION)
                                ?.takeIf { !it.isJsonNull }?.asString ?: ""
                        }
                        detailObj.takeIf { it.has(APIConstants.K_TEMP_STORAGE) }?.let{
                            orderDetails.tempStorage = detailObj.get(APIConstants.K_TEMP_STORAGE)
                                ?.takeIf { !it.isJsonNull }?.asString ?: ""
                        }
                        detailObj.takeIf { it.has(APIConstants.K_LAST_UPDATED_DATE_TIME) }?.let {
                            orderDetails.lastUpdatedDateTime =
                                detailObj.get(APIConstants.K_LAST_UPDATED_DATE_TIME)
                                    ?.takeIf { !it.isJsonNull }?.asString ?: ""
                        }
                        detailObj.takeIf { it.has(APIConstants.K_LIST_ITEM_STATUS) }?.let {
                            orderDetails.listItemStatus =
                                detailObj.get(APIConstants.K_LIST_ITEM_STATUS)
                                    ?.takeIf { !it.isJsonNull }?.asString ?: ""
                        }
                        orderDetailsList.add(orderDetails)
                        when (orderDetails.workorderType) {
                            "U0", "U1", "I0" -> recOrderDetailsList.add(orderDetails)
                            "L0", "L1" -> disOrderDetailsList.add(orderDetails)
                        }
                    }


                        Log.e("RecyclerViewDebug", "recOrderDetailsList size: ${recOrderDetailsList.size}")
                        Log.e("RecyclerViewDebug", "disOrderDetailsList size: ${disOrderDetailsList.size}")
                    if(workOrderDetailsRecAdapter!=null){
                        workOrderDetailsRecAdapter.notifyDataSetChanged()
                    }
                    if(workOrderDetailsDisAdapter!=null){
                        workOrderDetailsDisAdapter.notifyDataSetChanged()
                    }

                        binding.recCount.text = recOrderDetailsList.size.toString()
                        binding.disCount.text = disOrderDetailsList.size.toString()

                    workOrderDetailsRecAdapter.notifyDataSetChanged()
                    workOrderDetailsDisAdapter.notifyDataSetChanged()
                }

            }

        } catch (ex: Exception) {
            Log.e("GETWORKDETAILSEXC", ex.message ?: "Unknown error")
            AssetUtils.showCommonBottomSheetErrorDialog(context, "Error parsing work details")
        }

    }

    private fun postInventoryData(
        palletTag: String,
        WorkOrderType: String,
        destinationTag: String,
        workOrderNo: String
    ) {
            try {
                if (isPostingInventory) return
                isPostingInventory = true
                workorderFetchingHandler.removeCallbacks(workorderFetchingRunnable)
                val id = UUID.randomUUID().toString()
                val deviceID = sharedPreferencesManager.getDeviceId().toString()
                val dateTime = AssetUtils.getUTCSystemDateTimeInFormatt().toString()
                val subTagList = listOf(
                    dataModels.SubTagData(
                        TagID = destinationTag,
                        Count = 1,
                        TagType = "Bin",
                        RSSI = "-60",
                        TransDatetime = dateTime,
                        CategoryID = 3
                    )
                )
                val requestBody = dataModels.InventoryRequest(
                    TransID = id,
                    ClientDeviceID = deviceID,
                    AntennaID = 1,
                    RSSI = 40,
                    TransDatetime = dateTime,
                    TouchPointType = "1",
                    Count = "1",
                    PalletTagID = palletTag,
                    ListItemStatus = "Completed",
                    WorkorderNumber = workOrderNo,
                    WorkorderType = WorkOrderType,
                    CategoryID = "2",
                    SubTagDetails = subTagList
                )
                val loggingInterceptor = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }

                val okHttpClient = OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .connectTimeout(APIConstants.API_TIMEOUT.toLong(), TimeUnit.SECONDS)
                    .readTimeout(APIConstants.API_TIMEOUT.toLong(), TimeUnit.SECONDS)
                    .writeTimeout(APIConstants.API_TIMEOUT.toLong(), TimeUnit.SECONDS)
                    .build()

                val retrofit: Retrofit = Retrofit.Builder()
                    .baseUrl(sharedPreferencesManager.getHostUrl().toString())
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(okHttpClient)
                    .build()
                val apiService2: APIService = retrofit.create(APIService::class.java)
                apiService2.postInventory(requestBody)
                    .enqueue(object : Callback<JsonObject> {
                        override fun onResponse(
                            call: Call<JsonObject>,
                            response: Response<JsonObject>
                        ) {
                            try {
                                if (response.isSuccessful) {
                                    response.body()?.takeIf { it.has(APIConstants.K_STATUS) }
                                        ?.let { jsonObject ->
                                            val status =
                                                jsonObject.get(APIConstants.K_STATUS)?.asBoolean
                                                    ?: false
                                            val message =
                                                jsonObject.get(APIConstants.K_MESSAGE)?.asString
                                                    ?: ""
                                            if (status) {
                                                orderDetailsList.removeAll { it.palletTagID == palletTag }
                                                recOrderDetailsList.removeAll { it.palletTagID == palletTag }
                                                disOrderDetailsList.removeAll { it.palletTagID == palletTag }

                                                workOrderDetailsRecAdapter.notifyDataSetChanged()
                                                workOrderDetailsDisAdapter.notifyDataSetChanged()

                                                binding.recCount.text = recOrderDetailsList.size.toString()
                                                binding.disCount.text = disOrderDetailsList.size.toString()
                                                AssetUtils.showCommonBottomSheetSuccessDialog(
                                                    context,
                                                    message
                                                )
                                                    binding.textPalletNo.text = ""
                                                    binding.textDestination.text = ""

                                                    bottomSheetDialog?.dismiss()
                                                    timerDateTime = ""

                                                Handler(Looper.getMainLooper()).postDelayed({
                                                    isPostingInventory = false
                                                    startWorkorderFetchingHandler()  // Resume polling
                                                }, 2000)

                                            } else {
                                                isPostingInventory = false
                                                startWorkorderFetchingHandler()
                                                AssetUtils.showCommonBottomSheetErrorDialog(
                                                    context,
                                                    message
                                                )
                                            }
                                        }
                                } else {
                                    isPostingInventory = false
                                    startWorkorderFetchingHandler()
                                    Log.e(
                                        "HTTP_ERROR",
                                        "HTTP Error Code: ${response.code()} - ${
                                            response.errorBody()?.string()
                                        }"
                                    )
                                    AssetUtils.showCommonBottomSheetErrorDialog(
                                        context,
                                        "Response Error(${response.code()}): ${
                                            response.errorBody()?.string()
                                        }"
                                    )
                                }
                            } catch (e: Exception) {
                                isPostingInventory = false
                                startWorkorderFetchingHandler()
                                Log.e(
                                    "RESPONSE_ERROR",
                                    "Error handling response: ${e.localizedMessage}"
                                )
                                AssetUtils.showCommonBottomSheetErrorDialog(
                                    context,
                                    "An error occurred while processing the response."
                                )
                            }
                        }

                        override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                            when (t) {
                                is SocketTimeoutException -> {
                                    AssetUtils.showCommonBottomSheetErrorDialog(
                                        context,
                                        resources.getString(R.string.TimeOutError)
                                    )
                                }

                                is UnknownHostException -> {
                                    AssetUtils.showCommonBottomSheetErrorDialog(
                                        context,
                                        resources.getString(R.string.internet_error)
                                    )
                                }

                                is ConnectException -> {
                                    AssetUtils.showCommonBottomSheetErrorDialog(
                                        context,
                                        resources.getString(R.string.communication_error)
                                    )
                                }

                                else -> {
                                    Log.e("NETWORK_ERROR", "Network Failure: ${t.localizedMessage}")
                                    AssetUtils.showCommonBottomSheetErrorDialog(
                                        context,
                                        "Network Failure: ${t.localizedMessage}."
                                    )
                                }

                            }
                            isPostingInventory = false
                            startWorkorderFetchingHandler()
                        }

                    })

            } catch (ex: Exception) {
                isPostingInventory = false
                startWorkorderFetchingHandler()
                Log.e("LOGIN_ERROR", "Unexpected Error: ${ex.localizedMessage}")
                AssetUtils.showCommonBottomSheetErrorDialog(context, "Something Went Wrong.")
            }
    }

    override fun onNetworkChanged(isConnected: Boolean) {
        if (isConnected) {
            if (!mqttConnection.isConnected()) {
                Log.e("MQTT", "Network restored, attempting reconnection...")
                Handler(Looper.getMainLooper()).postDelayed({
                    mqttConnect()
                }, 3000) // Delay to ensure network stability
            }
        } else {
            mqttConnection.disconnect()
            resetLED()
            binding.appLed.setImageResource(R.drawable.off_indicator)
            binding.textPalletNo.text = ""
            binding.textDestination.text = ""
        }
    }

    private fun mqttConnect() {
        if (mqttConnection.isConnected()) {
            Log.e("MQTT", "Already connected, skipping reconnection.")
            return
        }
        mqttConnection.connect(sharedPreferencesManager.getHostUrl().toString(), "Tab", object :
            MqttConnectionCallBack {
            override fun onSuccess() {
                subscribe()
            }

            override fun onFailure(errorMessage: String) {
                Log.e("MQTTConn", errorMessage)
                resetLED()
                binding.appLed.setImageResource(R.drawable.off_indicator)
                binding.textPalletNo.text = ""
                binding.textDestination.text = ""
                bottomSheetDialog?.dismiss()
            }
        })
    }

    private fun subscribe() {
        if (mqttConnection.isConnected()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val commandTopic = topics?.get("Command") ?: ""
                    val deviceId = sharedPreferencesManager.getDeviceId()
                    val subscribeTopic = "$commandTopic$deviceId/#"
                    Log.e("Topic", subscribeTopic)
                    if (subscribeTopic.isNotEmpty()) {
                            subscribeTopicAsync(subscribeTopic)
                    } else {
                        Log.e("MQTT", "Subscribe topic is empty, skipping subscription.")
                    }
                } catch (ex: Exception) {
                    Log.e("Error", "Publishing error: ${ex.message}")
                }
            }
        }
    }

    private suspend fun subscribeTopicAsync(topicName: String) {
        return withContext(Dispatchers.IO) {
            try {
                mqttSub.subscribeMessage(mqttConnection, topicName, object : MqttCallback {
                    override fun disconnected(disconnectResponse: MqttDisconnectResponse?) {
                        if (disconnectResponse != null) {
                            Log.d("Disconnected Message", disconnectResponse.getReasonString())
                            resetLED()
                            binding.appLed.setImageResource(R.drawable.off_indicator)
                            binding.textPalletNo.text = ""
                            binding.textDestination.text = ""
                        }
                    }

                    override fun mqttErrorOccurred(exception: MqttException?) {
                        exception?.message?.let { Log.e("Exception Message", it) }
                    }

                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        Log.e("Message Arrived Topic", topic!!)
                        Log.e("Message Arrived", message.toString())
                        handler.removeCallbacks(dismissPopupRunnable)
                        handler.postDelayed(dismissPopupRunnable, 15000) // Restart 15s timer
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val msgStr = message.toString()
                                val j = JsonParser.parseString(msgStr).asJsonObject
                                val messageType = j.get("messageType")?.asString ?: "Unknown"
                                val data = j.getAsJsonObject("data")
                                if (messageType.equals("DataLogger")) {
                                        handleDisplayMessage(data)
                                } else if (messageType.equals("DataConfig")) {
                                    handleConfigMessage(data)
                                }
                            } catch (ex: Exception) {
                                Log.e("Receiving Error", ex.message.toString())
                            }
                        }
                    }

                    override fun deliveryComplete(token: IMqttToken?) {
                        Log.d("Delivery Complete", "Message delivered");
                    }

                    override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                        Log.d("Connected Message", "Connected to: " + serverURI);
                    }

                    override fun authPacketArrived(reasonCode: Int, properties: MqttProperties?) {
                        Log.d("Auth Packet Arrived", "Auth reason: " + reasonCode);
                    }

                })
            } catch (e: Exception) {
                Log.e("MQTT", "Publish exception: ${e.message}")
            }
        }
    }

    private suspend fun handleDisplayMessage(data: JsonObject) {
        return withContext(Dispatchers.Main) {
            try {
                statusDateTime = AssetUtils.getSystemDateTime()
                Is_App_Running = true
                binding.appLed.setImageResource(R.drawable.on_indicator)
                if (!data.isJsonNull) {
                    if (data.has("tagDetails")) {
                        var palletName = ""
                        var WOType = ""
                        var palletTagID = ""
                        var WONo = ""
                        var destination = ""
                        var matchedPallet: OrderDetails? = null
                        var currentDestinationTag = ""
                        val destinationList = mutableMapOf<String, MutableMap<String, Int>>()

                        val jsonArray = data.getAsJsonArray("tagDetails")
                        if (jsonArray.size() > 0) {
                            for (j in jsonArray) {
                                val tagObject = j.asJsonObject
                                val tagID = tagObject.get("tagID")?.asString.orEmpty()
                                val tagName = tagObject.get("tagName")?.asString.orEmpty()
                                val rssi = tagObject.get("rssi")?.asInt ?: 0
                                val antennaID = tagObject.get("antennaID")?.asString.orEmpty()
                                Log.e("ANTS", antennaID).toString()
                                resetLED()
                                when (antennaID.trim()) {

                                    "1" -> binding.ledAnt1.setImageResource(R.drawable.on_indicator)
                                    "2" -> binding.ledAnt2.setImageResource(R.drawable.on_indicator)
                                    "3" -> binding.ledAnt3.setImageResource(R.drawable.on_indicator)
                                    "4" -> binding.ledAnt4.setImageResource(R.drawable.on_indicator)
                                    "5" -> binding.ledAnt5.setImageResource(R.drawable.on_indicator)
                                    "6" -> binding.ledAnt6.setImageResource(R.drawable.on_indicator)
                                    "7" -> binding.ledAnt7.setImageResource(R.drawable.on_indicator)
                                    "8" -> binding.ledAnt8.setImageResource(R.drawable.on_indicator)
                                    else -> Log.e("Antenna", "Unknown Antenna ID: $antennaID")
                                }
                                val tagType = tagID.substring(2, 4).toString()
                                if (orderDetailsList.isNotEmpty()) {
                                    when (tagType) {
                                        "02" -> {
                                            for (order in orderDetailsList) {
                                                Log.e(
                                                    "CheckOrderList",
                                                    "Comparing: ${order.palletTagID?.trim()} with $tagID"
                                                )
                                                if (order.palletTagID?.trim() == tagID.trim()) {
                                                    matchedPallet = order
                                                    Log.e(
                                                        "Matched",
                                                        "Match found: ${matchedPallet!!.palletTagID}"
                                                    )
                                                    break
                                                }
                                            }
                                            matchedPallet?.let {
//                                        Is_Popup_Showing = true
//                                        timerDateTime = AssetUtils.getUTCSystemDateTimeInFormatt()
                                                palletName = it.palletNumber.toString()
                                                WOType = it.workorderType.toString()
                                                palletTagID = it.palletTagID.toString()
                                                WONo = it.workorderNo.toString()


                                                binding.textPalletNo.text = palletName

                                                destination = when {
                                                    WOType == "U0" && it.loadingAreaName?.isNotEmpty() == true -> it.loadingAreaName!!
                                                    WOType == "U1" && it.binLocation?.isNotEmpty() == true -> it.binLocation!!
                                                    WOType == "L0" && it.tempStorage?.isNotEmpty() == true -> it.tempStorage!!
                                                    WOType == "L1" && it.loadingAreaName?.isNotEmpty() == true -> it.loadingAreaName!!
                                                    WOType == "I0" && it.binLocation?.isNotEmpty() == true -> it.binLocation!!
                                                    else -> ""
                                                }

                                                if (destination.isNotEmpty()) {
                                                    val currentDestinations =
                                                        binding.textDestination.text.toString()
                                                            .split(", ")
                                                            .filter { it.isNotEmpty() }
                                                            .toMutableSet() // Convert to a set to prevent duplicates

                                                    if (destination.isNotEmpty() && !currentDestinations.contains(
                                                            destination
                                                        )
                                                    ) {
                                                        currentDestinations.add(destination)
                                                    }

                                                    binding.textDestination.text =
                                                        currentDestinations.joinToString(", ") // Reconstruct text
                                                    binding.textDestination.isSelected = true
                                                }

                                            }
                                        }

                                        "03" -> {
                                            val tagInfo =
                                                destinationList.getOrPut(tagID) { mutableMapOf() }
                                            tagInfo[tagName] = rssi
                                            Log.e("Dest", destinationList.toString())
                                        }
                                        "04" -> {
                                            val tagInfo =
                                                destinationList.getOrPut(tagID) { mutableMapOf() }
                                            tagInfo[tagName] = rssi
                                        }
                                    }
                                }

                            }
                        }


                        if (destinationList.isNotEmpty()) {
                            val minTagData = destinationList.minByOrNull {
                                it.value.values.minOrNull() ?: Int.MAX_VALUE
                            }
                            currentDestinationTag = minTagData?.key.orEmpty()
                            currentBinDestination =
                                minTagData?.value?.minByOrNull { it.value }?.key.orEmpty()
                            Is_Bin_Running = true
                            binDateTime = AssetUtils.getSystemDateTime()
                        }
                        Log.e(
                            "Current Destination",
                            "TagID: $currentDestinationTag, TagName: $currentBinDestination"
                        )

                        Log.e("matchedPallt", matchedPallet?.palletTagID.toString())
                        if (matchedPallet != null) {
                            Is_Popup_Showing = true
                            timerDateTime = AssetUtils.getSystemDateTime()
                            Log.e("SETTime", timerDateTime)
                                showPopup(
                                    palletName,
                                    WOType,
                                    destination,
                                    currentBinDestination,
                                    palletTagID,
                                    currentDestinationTag,
                                    WONo
                                )
                        }

                    }
                }
            } catch (ex: JSONException) {
                Log.e("JSONExc", ex.message.toString())
            }
        }
    }

    private suspend fun handleConfigMessage(data: JsonObject) {
        return withContext(Dispatchers.Main) {
            try {
                statusDateTime = AssetUtils.getSystemDateTime()
                Is_App_Running = true
                binding.appLed.setImageResource(R.drawable.on_indicator)
                if (!data.isJsonNull) {
                    binding.textPalletNo.text = ""
                    binding.textDestination.text = ""
                    bottomSheetDialog?.dismiss()
                    data.takeIf { it.has("AntennaID") }.let {
                        val ants = data.get("AntennaID")?.asString ?: ""
                        resetLED()
                        ants.split(",").forEach { antId ->
                            when (antId.trim()) {
                                "1" -> binding.ledAnt1.setImageResource(R.drawable.on_indicator)
                                "2" -> binding.ledAnt2.setImageResource(R.drawable.on_indicator)
                                "3" -> binding.ledAnt3.setImageResource(R.drawable.on_indicator)
                                "4" -> binding.ledAnt4.setImageResource(R.drawable.on_indicator)
                                "5" -> binding.ledAnt5.setImageResource(R.drawable.on_indicator)
                                "6" -> binding.ledAnt6.setImageResource(R.drawable.on_indicator)
                                "7" -> binding.ledAnt7.setImageResource(R.drawable.on_indicator)
                                "8" -> binding.ledAnt8.setImageResource(R.drawable.on_indicator)
                                else -> Log.e("Antenna", "Unknown Antenna ID: $antId")
                            }
                        }
                    }
                }
            } catch (ex: JSONException) {
                Log.e("JSONExc", ex.message.toString())
            }
        }
    }

    @SuppressLint("MissingInflatedId")
    private fun showPopup(palletNumber: String, workOrderType: String, suggestedDestination: String, currentDestination: String, palletTag: String, destinationTag: String, workOrderNo: String) {
        // Inflate the custom popup layout
        val inflater = LayoutInflater.from(this)
        val popupView = inflater.inflate(R.layout.custom_popup_layout, null)

        // Initialize views
        val textPalletName = popupView.findViewById<TextView>(R.id.textPalletNo)
        val textSuggestDestination = popupView.findViewById<TextView>(R.id.textSuggestDestination)
        val textActualDestinationHeader = popupView.findViewById<TextView>(R.id.textActualDestinationHeader)
        val textActualDestination = popupView.findViewById<TextView>(R.id.textActualDestination)
        val llActualDest = popupView.findViewById<LinearLayout>(R.id.llActualDest)
        val btnPost = popupView.findViewById<Button>(R.id.btnPost)

        // Set visibility and values based on workOrderType
        when (workOrderType) {
            "U0", "L1" -> {
                textPalletName.visibility = View.VISIBLE
                textSuggestDestination.visibility = View.VISIBLE
                llActualDest.visibility = View.GONE
                textPalletName.text = palletNumber
                textSuggestDestination.text = suggestedDestination
            }
            "U1", "I0", "L0" -> {
                textPalletName.visibility = View.VISIBLE
                textSuggestDestination.visibility = View.VISIBLE
                textActualDestination.visibility = View.VISIBLE
                textActualDestinationHeader.visibility = View.VISIBLE
                btnPost.visibility = View.VISIBLE

                textPalletName.text = palletNumber
                textSuggestDestination.text = suggestedDestination
                textActualDestination.text = currentDestination

                if (currentDestination != "__") {
                    //if (suggestedDestination != currentDestination) {
                        btnPost.setOnClickListener {
                            showCustomConfirmationDialogSpecial(
                                "Do you want to put $palletNumber in $currentDestination?",
                                "SAVE",
                                palletTag,
                                workOrderType,
                                destinationTag,
                                workOrderNo
                            )
                        }
//                    } else {
//                        btnPost.visibility = View.VISIBLE
//                    }
                } else {
                    btnPost.visibility = View.INVISIBLE
                }
            }
        }

        // Create or update the BottomSheetDialog
        if (bottomSheetDialog == null) {
            bottomSheetDialog = BottomSheetDialog(this).apply {
                setContentView(popupView)
                setCancelable(false)
            }
        } else {
            bottomSheetDialog?.setContentView(popupView)
        }

        bottomSheetDialog?.show()
    }
    private fun showCustomConfirmationDialogSpecial(msg: String, action: String, palletTag: String, workOrderType: String, destinationTag: String, workOrderNo: String) {
        confirmationDialog = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(false)
            setContentView(R.layout.custom_alert_dialog_layout2)
        }
        confirmationDialog.dismiss()

        val text = confirmationDialog.findViewById<TextView>(R.id.text_dialog)
        val dialogButton = confirmationDialog.findViewById<Button>(R.id.btn_dialog)
        val btnCancel = confirmationDialog.findViewById<Button>(R.id.btn_dialog_cancel)

        text?.text = msg

        dialogButton?.setOnClickListener {
            confirmationDialog.dismiss()
                postInventoryData(palletTag, workOrderType, destinationTag, workOrderNo)
        }

        btnCancel?.setOnClickListener {
            confirmationDialog.dismiss()
            IS_API_CALL_IS_IN_PROGRESS = false
        }

        if (!isFinishing) {
            confirmationDialog.show()
        }
    }
    private fun showCustomConfirmationDialog(msg: String, action: String) {
        customConfirmationDialog = Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(false)
            setContentView(R.layout.custom_alert_dialog_layout2)
        }
        customConfirmationDialog.dismiss()

        val text = customConfirmationDialog.findViewById<TextView>(R.id.text_dialog)
        val dialogButton = customConfirmationDialog.findViewById<Button>(R.id.btn_dialog)
        val dialogButtonCancel = customConfirmationDialog.findViewById<Button>(R.id.btn_dialog_cancel)

        text?.text = msg
        dialogButton?.text = "YES"
        dialogButtonCancel?.text = "NO"

        dialogButton?.setOnClickListener {
            customConfirmationDialog.dismiss()
            if (action == "BACK") {
                //logout()
            }
        }

        dialogButtonCancel?.setOnClickListener {
            customConfirmationDialog.dismiss()
        }

        customConfirmationDialog.show()
    }
    private val dismissPopupRunnable = Runnable {
        if (bottomSheetDialog?.isShowing == true) {
            bottomSheetDialog?.dismiss() // Dismiss the popup
        }
        binding.appLed.setImageResource(R.drawable.off_indicator)
        resetLED()
        binding.textPalletNo.text = ""
        binding.textDestination.text = ""
    }

    private fun resetLED() {
        listOf(
            binding.ledAnt1, binding.ledAnt2, binding.ledAnt3, binding.ledAnt4,
            binding.ledAnt5, binding.ledAnt6, binding.ledAnt7, binding.ledAnt8
        ).forEach { it.setImageResource(R.drawable.off_indicator) }
    }

    fun startTimerCheck() {
            job?.cancel()
            job = CoroutineScope(Dispatchers.Default).launch {
                while (isActive) {
                    if(timerDateTime.isNotEmpty()){
                        val currentTime = AssetUtils.getSystemDateTime()
                        val difference = AssetUtils.getTimeDifferenceInSeconds(timerDateTime, currentTime)
                        if (difference >= 0) {
                            if (difference > 5) {
                                withContext(Dispatchers.Main) {
                                    if (Is_Popup_Showing) {
                                        Is_Popup_Showing = false
                                        bottomSheetDialog?.dismiss()
                                        binding.textPalletNo.text = ""
                                        binding.textDestination.text = ""
                                    }
                                }
                            }
                        }
                        else {
                            Log.e("TimerCheck", "Invalid time difference calculation.")
                        }
                    }
                    else{
                        if (Is_Popup_Showing) {
                            Is_Popup_Showing = false
                            bottomSheetDialog?.dismiss()
                            binding.textPalletNo.text = ""
                            binding.textDestination.text = ""
                        }
                    }
                    if(statusDateTime.isNotEmpty()){
                        val currentTime1 = AssetUtils.getSystemDateTime()
                        val difference1 = AssetUtils.getTimeDifferenceInSeconds(statusDateTime, currentTime1)
                        Log.e("DIFF", difference1.toString())
                        if (difference1 >= 0) {
                            if (difference1 > 20) {
                                withContext(Dispatchers.Main) {
                                    Log.e("StatusCheck", "Time exceeded 20 seconds!")
                                    if (Is_App_Running) {
                                        Is_App_Running = false
                                       binding.appLed.setImageResource(R.drawable.off_indicator)
                                    }
                                }
                            }
                        }
                        else {
                            Log.e("StatusCheck", "Invalid time difference calculation.")
                        }
                    }
                    else{
                        if (Is_App_Running) {
                            Is_App_Running = false
                            binding.appLed.setImageResource(R.drawable.custom_off_button)
                        }
                    }
                    if(binDateTime.isNotEmpty()){
                        val currentTime2 = AssetUtils.getSystemDateTime()
                        val difference2 = AssetUtils.getTimeDifferenceInSeconds(binDateTime, currentTime2)
                        Log.e("DIFFBin", difference2.toString())
                        if (difference2 >= 0) {
                            if (difference2 > 10) {
                                withContext(Dispatchers.Main) {
                                    Log.e("StatusCheck", "Time exceeded 20 seconds!")
                                    if (Is_Bin_Running) {
                                        Is_Bin_Running = false
                                        currentBinDestination = "__"
                                    }
                                }
                            }
                        }
                        else {
                            Log.e("StatusCheck", "Invalid time difference calculation.")
                        }
                    }
                    else{
                        if (Is_Bin_Running) {
                            Is_Bin_Running = false
                            currentBinDestination = "__"
                        }
                    }
                    delay(1000) // Check every 1 second
                }
            }

    }

    private fun setDefault() {
        stopHandler()
        orderDetailsList.clear()
        recOrderDetailsList.clear()
        disOrderDetailsList.clear()
        cd.unregisterNetworkCallback()
        CoroutineScope(Dispatchers.IO).launch {
            mqttConnection.disconnect()
        }
        runOnUiThread {
            resetLED()
            binding.appLed.setImageResource(R.drawable.off_indicator)
            binding.textPalletNo.text = ""
            binding.textDestination.text = ""
            bottomSheetDialog?.dismiss()
        }
    }
    private fun startReader(action: String) {
        val jsonObject = JsonObject()
        jsonObject.addProperty("messageType", "Command")
        jsonObject.addProperty("pubDeviceID", sharedPreferencesManager.getDeviceId())
        jsonObject.addProperty("subDeviceID", sharedPreferencesManager.getPairedDeviceID())
        val dataObject = JsonObject()
        dataObject.addProperty("ReaderCommand", action)
        jsonObject.add("data", dataObject)
        val commandTopic = topics?.get("Command") ?: ""
        val pubTopic = commandTopic+sharedPreferencesManager.getDeviceId()+"/operation"
        if(mqttConnection.isConnected()){
            CoroutineScope(Dispatchers.IO).launch {
                try {
                val isPublished = publishMessageAsync(pubTopic, jsonObject.toString())
                if (isPublished) {
                    Log.e("Success", "Published Successfully")
                    //synchronized(messageStack) {
//                    if (messageStack.isNotEmpty() && messageStack.peek().second == message) {
//                        messageStack.pop() // Remove the message only after successful publishing
//                    }
                    //}
                } else {
                    Log.e("Failure", "Publish failed")

                }
                } catch (e: Exception) {
                    Log.e("Error", "Publishing error: ${e.message}")
                }
            }
        }
    }
    private suspend fun publishMessageAsync(topicName: String, message: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val result = CompletableDeferred<Boolean>()

                mqttPub.publishMessage(mqttConnection, topicName, message, object :
                    MqttResponseCallback {
                    override fun onPublishSuccess() {
                        result.complete(true)
                    }

                    override fun onPublishFailure(error: String) {
                        result.complete(false)
                    }
                })

                result.await() // Wait for publish result
            } catch (e: Exception) {
                Log.e("MQTT", "Publish exception: ${e.message}")
                false
            }
        }
    }

    override fun onDestroy() {
        setDefault()
        super.onDestroy()
    }

    override fun onBackPressed() {
        setDefault()
        super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        startTimerCheck()
    }
}