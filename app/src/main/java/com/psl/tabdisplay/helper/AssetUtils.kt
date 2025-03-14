package com.psl.tabdisplay.helper

import android.app.ProgressDialog
import android.content.Context
import android.os.CountDownTimer
import android.util.Log
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.psl.tabdisplay.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

object AssetUtils {
    var progressDialog: ProgressDialog? = null
    var bottomSheetDialog: BottomSheetDialog? = null

    fun showProgress(context: Context?, progress_message: String?) {
        if (progressDialog != null) {
            progressDialog!!.dismiss()
        }
        progressDialog = ProgressDialog(context)
        progressDialog!!.setMessage(progress_message)
        progressDialog!!.setProgressStyle(ProgressDialog.STYLE_SPINNER)

        progressDialog!!.isIndeterminate = true
        progressDialog!!.setCancelable(false)

        //progressDialog.setIndeterminate(true);
        progressDialog!!.setCanceledOnTouchOutside(false)
        progressDialog!!.show()
    }

    /**
     * method to hide Progress Dialog
     */
    fun hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog!!.dismiss()
        }
    }


    fun showCommonBottomSheetErrorDialog(context: Context, message: String?) {
        try {
            if (bottomSheetDialog != null) {
                bottomSheetDialog!!.dismiss()
            }
            bottomSheetDialog = BottomSheetDialog(context)
            bottomSheetDialog!!.setContentView(R.layout.custom_bottom_dialog_layout)
            val textmessage = bottomSheetDialog!!.findViewById<TextView>(R.id.textMessage)
            textmessage!!.text = message
            textmessage!!.setBackgroundColor(context.resources.getColor(R.color.red1))
            bottomSheetDialog!!.show()
            object : CountDownTimer(2500, 500) {
                override fun onTick(millisUntilFinished: Long) {
                    // TODO Auto-generated method stub
                }

                override fun onFinish() {
                    // TODO Auto-generated method stub
                    bottomSheetDialog!!.dismiss()
                }
            }.start()
        } catch (e: Exception) {
        }
    }


    fun showCommonBottomSheetSuccessDialog(context: Context, message: String?) {
        try {
            if (bottomSheetDialog != null) {
                bottomSheetDialog!!.dismiss()
            }
            bottomSheetDialog = BottomSheetDialog(context)
            bottomSheetDialog!!.setContentView(R.layout.custom_bottom_dialog_layout)
            val textmessage = bottomSheetDialog!!.findViewById<TextView>(R.id.textMessage)
            textmessage!!.text = message
            textmessage!!.setBackgroundColor(context.resources.getColor(R.color.green))
            bottomSheetDialog!!.show()
            object : CountDownTimer(2000, 500) {
                override fun onTick(millisUntilFinished: Long) {
                    // TODO Auto-generated method stub
                }

                override fun onFinish() {
                    // TODO Auto-generated method stub
                    bottomSheetDialog!!.dismiss()
                }
            }.start()
        } catch (e: Exception) {
        }
    }
    fun getUTCSystemDateTimeInFormatt(): String {
        try {
            val f = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            f.timeZone = TimeZone.getTimeZone("UTC")
            Log.e("UTCDATETIME1", f.format(Date()))
            // f.setTimeZone(TimeZone.getTimeZone("GMT"));
            println(f.format(Date()))
            Log.e("UTCDATETIME2", f.format(Date()))
            val utcdatetime = f.format(Date())
            return utcdatetime
        } catch (e: java.lang.Exception) {
            // return "01011970000000";
            // return "1970-01-01 00:00:00";
            return "1970-01-01 00:00:00"
        }
    }

}