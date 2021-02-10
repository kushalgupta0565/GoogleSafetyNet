package com.hominoid.safetynet.device_verifier

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class AndroidDeviceVerifier(private val apiKey: String, private val signatureToVerify: String) {

    interface AndroidDeviceVerifierCallback {
        fun error(s: String?)
        fun success(isValidSignature: Boolean)
    }

    fun verify(androidDeviceVerifierCallback: AndroidDeviceVerifierCallback) {
        CoroutineScope(Dispatchers.Main).launch {
            val isValidSignature = verifySafetyNetSignature()
            if (isValidSignature) {
                androidDeviceVerifierCallback!!.success(isValidSignature)
            } else {
                androidDeviceVerifierCallback!!.error("Invalid Signature")
            }
        }
    }

    private suspend fun verifySafetyNetSignature(): Boolean = withContext(Dispatchers.IO) {
        var isValidSignature = false
        try {
            val verifyApiUrl = URL(GOOGLE_VERIFICATION_URL + apiKey)
            val urlConnection = verifyApiUrl.openConnection() as HttpsURLConnection
            urlConnection.requestMethod = "POST"
            urlConnection.setRequestProperty("Content-Type", "application/json")

            //build post body { "signedAttestation": "<output of getJwsResult()>" }
            val requestJsonBody = "{ \"signedAttestation\": \"$signatureToVerify\"}"
            val outputInBytes = requestJsonBody.toByteArray(charset("UTF-8"))
            val os = urlConnection.outputStream
            os.write(outputInBytes)
            os.close()
            urlConnection.connect()

            //resp ={ “isValidSignature”: true }
            val `is` = urlConnection.inputStream
            val sb = StringBuilder()
            val rd = BufferedReader(InputStreamReader(`is`))
            var line: String?
            while (rd.readLine().also { line = it } != null) {
                sb.append(line)
            }
            val response = sb.toString()
            val responseRoot = JSONObject(response)
            if (responseRoot.has("isValidSignature")) {
                isValidSignature = responseRoot.getBoolean("isValidSignature")
            }
        } catch (e: Exception) {
            //something went wrong requesting validation of the JWS Message
            Log.e(TAG, "problem validating JWS Message :" + e.message, e)
            isValidSignature = false
        }
        isValidSignature
    }

    companion object {
        private val TAG = AndroidDeviceVerifier::class.java.simpleName

        //used to verify the safety net response - 10,000 requests/day free
        private const val GOOGLE_VERIFICATION_URL =
            "https://www.googleapis.com/androidcheck/v1/attestations/verify?key="
    }
}