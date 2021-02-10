package com.hominoid.safetynet

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.hominoid.safetynet.device_verifier.SafetyNetHelper
import com.hominoid.safetynet.device_verifier.SafetyNetWrapperCallback

class MainActivity : AppCompatActivity() {

    val TAG = "MainActivity"
    lateinit var btn_saety_net: Button
    lateinit var tv_result: TextView
    lateinit var progress_bar: ProgressBar
    private var safetyNetHelper: SafetyNetHelper? = null
    private val API_KEY = "AIzaSyDxCpnAP-dlinxe97VPh0KX5DD4L3FqXcI"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn_saety_net = findViewById(R.id.btn_saety_net)
        tv_result = findViewById(R.id.tv_result)
        progress_bar = findViewById(R.id.progress_bar)
        safetyNetHelper = SafetyNetHelper(API_KEY)

        btn_saety_net.setOnClickListener {
            runTest()
        }
    }

    private fun runTest() {
        showLoading(true)
        Log.d(TAG, "SafetyNet start request")
        safetyNetHelper?.startSafetyNetTest(
            this,
            object : SafetyNetWrapperCallback {
                override fun error(errorCode: Int, errorMessage: String?) {
                    showLoading(false)
                    handleError(errorCode, errorMessage!!)
                }

                override fun success(ctsProfileMatch: Boolean, basicIntegrity: Boolean) {
                    val result =
                        "SafetyNet req success: \nctsProfileMatch : $ctsProfileMatch and \nbasicIntegrity : $basicIntegrity"
                    Log.d(TAG, result)
                    showLoading(false)
                    tv_result.text = result
                }
            })
    }

    private fun handleError(errorCode: Int, errorMsg: String) {
        Log.e(TAG, errorMsg)
        val b = StringBuilder()
        when (errorCode) {
            SafetyNetHelper.ERROR_SAFETY_NET_API_REQUEST_UNSUCCESSFUL -> {
                b.append("SafetyNet request failed\n")
                b.append("(This could be a networking issue.)\n")
            }
            SafetyNetHelper.ERROR_RESPONSE_ERROR_VALIDATING_SIGNATURE -> {
                b.append("SafetyNet request: success\n")
                b.append("Response signature validation: error\n")
            }
            SafetyNetHelper.ERROR_RESPONSE_FAILED_SIGNATURE_VALIDATION -> {
                b.append("SafetyNet request: success\n")
                b.append("Response signature validation: fail\n")
            }
            SafetyNetHelper.ERROR_RESPONSE_VALIDATION_FAILED -> {
                b.append("SafetyNet request: success\n")
                b.append("Response validation: fail\n")
            }
            else -> {
                b.append("SafetyNet request failed\n")
                b.append("(This could be a networking issue.)\n")
            }
        }
        tv_result.setText(b.toString() + "\n" + errorMsg)
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            btn_saety_net.visibility = View.GONE
            tv_result.visibility = View.GONE
            progress_bar.visibility = View.VISIBLE
        } else {
            btn_saety_net.visibility = View.VISIBLE
            tv_result.visibility = View.VISIBLE
            progress_bar.visibility = View.GONE
        }
    }
}