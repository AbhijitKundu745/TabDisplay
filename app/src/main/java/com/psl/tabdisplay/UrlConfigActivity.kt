package com.psl.tabdisplay

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.psl.tabdisplay.APIHelpers.APIConstants
import com.psl.tabdisplay.APIHelpers.APIResponse
import com.psl.tabdisplay.APIHelpers.APIService
import com.psl.tabdisplay.APIHelpers.LoginRequest
import com.psl.tabdisplay.models.dataModels
import com.psl.tabdisplay.databinding.ActivityUrlConfigBinding
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

class UrlConfigActivity : AppCompatActivity(), ConnectionManager.ConnectionListener {
    private var context : Context = this
    private lateinit var binding: ActivityUrlConfigBinding
    private lateinit var cd : ConnectionManager
    private lateinit var sharedPreferencesManager: SharedPreferencesManager
    private var host_config : Boolean = false
    private lateinit var HOST_URL : String
    private lateinit var dialogsuccess : Dialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_url_config)

        cd = ConnectionManager(this, this)
        sharedPreferencesManager = SharedPreferencesManager(context)
        HOST_URL = sharedPreferencesManager.getHostUrl().toString()
        host_config = sharedPreferencesManager.getIsHostConfig()
        if (host_config) {
           binding.edtUrl.setText(HOST_URL)
        } else {
            binding.edtUrl.setText(sharedPreferencesManager.getHostUrl())
        }
        binding.btnClear.setOnClickListener{
            binding.edtUrl.setText("")
        }
        binding.btnConfig.setOnClickListener{
            if (binding.edtUrl.text.toString().length < 8) {
                AssetUtils.showCommonBottomSheetErrorDialog(context, resources.getString(R.string.enter_config_url))
            } else{
                if(cd.isConnectedToNetwork()){
                    val url = binding.edtUrl.text.toString().trim()
                    try{
                        GetAccessToken("""Please wait...URL Validation is in progress""".trimIndent(), url)

                    } catch (ex: Exception){
                        Log.e("ConfigException", ex.toString())
                    }
                } else {
                    AssetUtils.showCommonBottomSheetErrorDialog(
                        context,
                        resources.getString(R.string.internet_error)
                    )
                }
            }
        }
    }
    private fun GetAccessToken(progressMessage : String, url : String) {
        showProgress(context, progressMessage)
        val requestBody = LoginRequest(UserName = "", Password = "", ClientDeviceID = "")

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
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()

        Log.e("URL", "Base URL: $url")

        val apiService: APIService = retrofit.create(APIService::class.java)
        apiService.userLogin(requestBody)
            .enqueue(object : Callback<APIResponse<dataModels.LoginResponse>> {
                override fun onResponse(
                    call: Call<APIResponse<dataModels.LoginResponse>>,
                    response: Response<APIResponse<dataModels.LoginResponse>>
                ) {
                    hideProgressDialog()
                    val result = response.body()
                    if(result!=null){
                        Log.e("RES", result.toString())
                        showCustomSuccessConfirmationDialog(context,
                           resources.getString(R.string.url_validation), url)
                    }
                    else{
                        AssetUtils.showCommonBottomSheetErrorDialog(context, resources.getString(R.string.communication_error))
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
    }
    fun showCustomSuccessConfirmationDialog(context: Context?, msg: String?, url: String) {
        if (context == null || (context is Activity && context.isFinishing)) {
            Log.e("Dialog", "Context is null or activity is finishing, cannot show dialog.")
            return
        }
        dialogsuccess = Dialog(context)
        if (::dialogsuccess.isInitialized && dialogsuccess.isShowing) {
            dialogsuccess.dismiss()
        }


        dialogsuccess.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogsuccess.setCancelable(false)
        dialogsuccess.setContentView(R.layout.custom_alert_success_confirmation_dialog_layout)
        val text = dialogsuccess.findViewById<View>(R.id.text_dialog) as TextView
        text.text = msg
        val dialogButton = dialogsuccess.findViewById<View>(R.id.btn_dialog) as Button
        val dialogCancel = dialogsuccess.findViewById<View>(R.id.btnCancel) as Button
        dialogButton.setOnClickListener {
            dialogsuccess.dismiss()
            sharedPreferencesManager.setIsHostConfig(true)
            sharedPreferencesManager.setHostUrl(url)
            HOST_URL = url
            Log.e("URL", url)
            Log.e("SHAREDURL", sharedPreferencesManager.getHostUrl().toString())
            showCustomSuccessDialog(context, resources.getString(R.string.url_config_success))
        }
        dialogCancel.setOnClickListener { dialogsuccess.dismiss() }
        dialogsuccess.show()
    }
    fun showCustomSuccessDialog(context: Context?, msg: String?) {
        val dialog = Dialog(context!!)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.custom_alert_success_dialog_layout)
        val text = dialog.findViewById<View>(R.id.text_dialog) as TextView
        text.text = msg
        val dialogButton = dialog.findViewById<View>(R.id.btn_dialog) as Button
        dialogButton.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(
                context,
                LoginActivity::class.java
            )
            startActivity(intent)
            finish()
        }
        dialog.show()
    }
    override fun onNetworkChanged(isConnected: Boolean) {
        Log.e("S", isConnected.toString())
    }

    override fun onBackPressed() {
        finish()
        if (::dialogsuccess.isInitialized && dialogsuccess.isShowing) {
            dialogsuccess.dismiss()
        }
        super.onBackPressed()
    }
    override fun onDestroy() {
        super.onDestroy()
        if (::dialogsuccess.isInitialized && dialogsuccess.isShowing) {
            dialogsuccess.dismiss()
        }
    }
}