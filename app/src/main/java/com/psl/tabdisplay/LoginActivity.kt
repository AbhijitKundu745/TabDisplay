package com.psl.tabdisplay

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.gson.Gson
import com.psl.tabdisplay.APIHelpers.APIConstants
import com.psl.tabdisplay.APIHelpers.APIResponse
import com.psl.tabdisplay.APIHelpers.APIService
import com.psl.tabdisplay.APIHelpers.LoginRequest
import com.psl.tabdisplay.models.dataModels
import com.psl.tabdisplay.databinding.ActivityLoginBinding
import com.psl.tabdisplay.helper.AssetUtils
import com.psl.tabdisplay.helper.AssetUtils.hideProgressDialog
import com.psl.tabdisplay.helper.AssetUtils.showProgress
import com.psl.tabdisplay.helper.ConnectionManager
import com.psl.tabdisplay.helper.SharedPreferencesManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity(), ConnectionManager.ConnectionListener {
    private var context: Context = this
    private lateinit var binding : ActivityLoginBinding
    private lateinit var cd : ConnectionManager
    private lateinit var sharedPreferencesManager: SharedPreferencesManager
    private var deviceId : String= ""
    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)

        cd = ConnectionManager(this, this)
        sharedPreferencesManager = SharedPreferencesManager(context)

        deviceId = Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
        deviceId = deviceId.uppercase()
        Log.e("DeviceID", deviceId)
        binding.textDeviceId.text = "DeviceID : "+ deviceId
        sharedPreferencesManager.setDeviceId(deviceId)

        if (!sharedPreferencesManager.getIsHostConfig()) {
            AssetUtils.showCommonBottomSheetErrorDialog(
                context,
                resources.getString(R.string.url_not_config)
            )
        }

        if (sharedPreferencesManager.getIsLoginSaved()) {
            binding.chkRemember.isChecked = true
            binding.edtUserName.setText(sharedPreferencesManager.getSavedUser())
            binding.edtPassword.setText(sharedPreferencesManager.getSavedPassword())
        } else {
            binding.chkRemember.isChecked = false
            binding.edtUserName.setText("")
            binding.edtPassword.setText("")
        }

        binding.btnLogin.setOnClickListener{
            if(sharedPreferencesManager.getIsHostConfig()){
                val user =binding.edtUserName.text.toString().trim()
                val password =binding.edtPassword.text.toString().trim()
                if(binding.chkRemember.isChecked){
                    sharedPreferencesManager.setIsLoginSaved(true)
                    sharedPreferencesManager.setSavedUser(user)
                    sharedPreferencesManager.setSavedPassword(password)
                } else{
                    sharedPreferencesManager.setIsLoginSaved(false)
                    sharedPreferencesManager.setSavedUser("")
                    sharedPreferencesManager.setSavedPassword("")
                }
                if (user.equals("") || password.equals("")) {
                    AssetUtils.showCommonBottomSheetErrorDialog(context, resources.getString(R.string.login_data_validation))
                } else{
                    if(cd.isConnectedToNetwork()){
                        userLogin("Please wait...\nUser login is in progress", user, password, deviceId)
                    } else{
                        AssetUtils.showCommonBottomSheetErrorDialog(context, resources.getString(R.string.internet_error))
                    }

                }
            } else {
                AssetUtils.showCommonBottomSheetErrorDialog(context, resources.getString(R.string.url_not_config))
            }
        }
        binding.imgSetting.setOnClickListener{
            val intent = Intent(context, UrlConfigActivity::class.java)
            startActivity(intent)
        }
        binding.btnClear.setOnClickListener{
            binding.chkRemember.isChecked = false
            binding.edtUserName.setText("")
            binding.edtPassword.setText("")
            sharedPreferencesManager.setIsLoginSaved(false)
            sharedPreferencesManager.setSavedUser("")
            sharedPreferencesManager.setSavedPassword("")
        }
    }

    private fun userLogin(progressMessage : String, userName : String, password : String, deviceID : String){
        try {
        showProgress(context, progressMessage)
        val requestBody = LoginRequest(UserName = userName, Password = password, ClientDeviceID = deviceID)

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

        Log.e("URL", "Base URL: $sharedPreferencesManager.getHostUrl().toString()")
        val apiService: APIService = retrofit.create(APIService::class.java)
        apiService.userLogin(requestBody)
            .enqueue(object : Callback<APIResponse<dataModels.LoginResponse>> {
                override fun onResponse(
                    call: Call<APIResponse<dataModels.LoginResponse>>,
                    response: Response<APIResponse<dataModels.LoginResponse>>
                ) {
                    try {
                    hideProgressDialog()
                    if (response.isSuccessful){
                        val result = response.body()
                        Log.e("API_RESPONSE", "Success Body: ${Gson().toJson(result)}")
                        if (result != null) {
                            if(result.status){
                                sharedPreferencesManager.setSavedUser(requestBody.UserName)
                                sharedPreferencesManager.setSavedPassword(requestBody.Password)
                                val pairedDeviceID = result.data?.PairedDeviceID
                                sharedPreferencesManager.setPairedDeviceID(pairedDeviceID.toString())

                                val topicList1 = HashMap<String, String>()

                                result.data?.Topic?.forEach{ topic ->
                                    topicList1[topic.Title] = topic.TopicName
                                }
                                    Log.e("DATA", "Title: ${topicList1}")

                                val WOIntent = Intent(context, WorkorderDisplayActivity::class.java).apply {
                                        putExtra("TOPIC_LIST", topicList1)
                                }
                                startActivity(WOIntent)
                            }
                            else{
                                AssetUtils.showCommonBottomSheetErrorDialog(context, result.message)
                            }
                        }
                    }
                    else{
                        Log.e("HTTP_ERROR", "HTTP Error Code: ${response.code()} - ${response.errorBody()?.string()}")
                        AssetUtils.showCommonBottomSheetErrorDialog(context, "Response Error(${response.code()}): ${response.errorBody()?.string()}")
                    }
                    } catch (e: Exception) {
                        Log.e("RESPONSE_ERROR", "Error handling response: ${e.localizedMessage}")
                        AssetUtils.showCommonBottomSheetErrorDialog(context, "An error occurred while processing the response.")
                    }
                }

                override fun onFailure(
                    call: Call<APIResponse<dataModels.LoginResponse>>,
                    t: Throwable
                ) {
                    hideProgressDialog()
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
        } catch (e: Exception) {
            Log.e("LOGIN_ERROR", "Unexpected Error: ${e.localizedMessage}")
            AssetUtils.showCommonBottomSheetErrorDialog(context, "Something Went Wrong.")
        }
    }
    override fun onNetworkChanged(isConnected: Boolean) {
        Log.e("S", isConnected.toString())
    }
}