package com.hominoid.safetynet.device_verifier

import android.content.Context
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.safetynet.SafetyNet
import com.google.android.gms.safetynet.SafetyNetApi
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.gson.Gson
import com.hominoid.safetynet.device_verifier.AndroidDeviceVerifier.AndroidDeviceVerifierCallback
import com.hominoid.safetynet.model.SafetyNetResponse
import com.hominoid.safetynet.utils.Utils
import com.hominoid.safetynet.utils.Utils.Companion.calcApkCertificateDigests
import java.lang.Exception

class SafetyNetHelper(private val googleDeviceVerificationApiKey: String) :
    OnSuccessListener<SafetyNetApi.AttestationResponse>,
    OnFailureListener {

    private val TAG = SafetyNetHelper::class.java.simpleName
    private var requestNonce: ByteArray? = null
    private var packageName: String? = null
    private var apkCertificateDigests: List<String>? = null
    private var callback: SafetyNetWrapperCallback? = null
    private val safetyNetUtils: Utils

    companion object {
        const val ERROR_SAFETY_NET_API_REQUEST_UNSUCCESSFUL = 999
        const val ERROR_RESPONSE_ERROR_VALIDATING_SIGNATURE = 1000
        const val ERROR_RESPONSE_FAILED_SIGNATURE_VALIDATION = 1002
        const val ERROR_RESPONSE_FAILED_SIGNATURE_VALIDATION_NO_API_KEY = 1003
        const val ERROR_RESPONSE_VALIDATION_FAILED = 1001
    }

    init {
        safetyNetUtils = Utils()
    }

    fun startSafetyNetTest(context: Context, safetyNetWrapperCallback: SafetyNetWrapperCallback?) {
        packageName = context.packageName
        callback = safetyNetWrapperCallback
        apkCertificateDigests = calcApkCertificateDigests(context, packageName)
        Log.d(TAG, "apkCertificateDigests:$apkCertificateDigests")
        runSafetyNetTest(context)
    }

    private fun runSafetyNetTest(context: Context) {
        Log.v(TAG, "running SafetyNet.API Test")
        requestNonce = safetyNetUtils.generateOneTimeRequestNonce()
        SafetyNet.getClient(context).attest(requestNonce!!, googleDeviceVerificationApiKey)
            .addOnSuccessListener(this)
            .addOnFailureListener(this)
    }

    override fun onSuccess(attestationResponse: SafetyNetApi.AttestationResponse) {
        val jwsResult =
            attestationResponse.jwsResult // Successfully communicated with SafetyNet API.
        /*
          @jwsResult : Forward this result to your server together with the nonce for verification.

          You can also parse the JwsResult locally to confirm that the API
          returned a response by checking for an 'error' field first and before
          retrying the request with an exponential backoff.

          NOTE: Do NOT rely on a local, client-side only check for security, you
          must verify the response on a remote server!
          */
        val response = parseJsonWebSignature(jwsResult)
        if (response != null) {
            //only need to validate the response if it says we pass
            if (!response!!.isCtsProfileMatch || !response.isBasicIntegrity) {
                callback!!.success(response.isCtsProfileMatch, response.isBasicIntegrity)
                return
            } else {
                //validate payload of the response
                if (response != null) {
                    if (!TextUtils.isEmpty(googleDeviceVerificationApiKey)) {
                        //if the api key is set, run the AndroidDeviceVerifier
                        val androidDeviceVerifier = AndroidDeviceVerifier(
                            googleDeviceVerificationApiKey, jwsResult
                        )
                        androidDeviceVerifier.verify(object : AndroidDeviceVerifierCallback {
                            override fun error(errorMsg: String?) {
                                callback!!.error(
                                    ERROR_RESPONSE_ERROR_VALIDATING_SIGNATURE,
                                    "Response signature validation error: $errorMsg"
                                )
                            }

                            override fun success(isValidSignature: Boolean) {
                                if (isValidSignature) {
                                    callback!!.success(
                                        response.isCtsProfileMatch,
                                        response.isBasicIntegrity
                                    )
                                } else {
                                    callback!!.error(
                                        ERROR_RESPONSE_FAILED_SIGNATURE_VALIDATION,
                                        "Response signature invalid"
                                    )
                                }
                            }
                        })
                    } else {
                        Log.w(TAG, "No google Device Verification ApiKey defined")
                        callback!!.error(
                            ERROR_RESPONSE_FAILED_SIGNATURE_VALIDATION_NO_API_KEY,
                            "No Google Device Verification ApiKey defined. Marking as failed. SafetyNet CtsProfileMatch: " + response.isCtsProfileMatch
                        )
                    }
                } else {
                    callback!!.error(
                        ERROR_RESPONSE_VALIDATION_FAILED,
                        "Response payload validation failed"
                    )
                }
            }
        } else {
            callback!!.error(
                ERROR_SAFETY_NET_API_REQUEST_UNSUCCESSFUL,
                "SafetyNet request unsucessful"
            )
        }
    }

    override fun onFailure(e: Exception) {
        // An error with the Google Play Services API contains some additional details.
        if (e is ApiException) {
            val apiException = e
            callback!!.error(
                ERROR_RESPONSE_VALIDATION_FAILED,
                "ApiException[" + apiException.statusCode + "] " + apiException.message
            )
        } else {
            // A different, unknown type of error occurred.
            Log.d(TAG, "Error: " + e.message)
            callback!!.error(
                ERROR_RESPONSE_VALIDATION_FAILED,
                "Response payload validation failed"
            )
        }
    }
}

private fun parseJsonWebSignature(jwsResult: String?): SafetyNetResponse? {
    if (jwsResult == null) {
        return null
    }
    //the JWT (JSON WEB TOKEN) is just a 3 base64 encoded parts concatenated by a . character
    val jwtParts = jwsResult.split(".").toTypedArray()
    return if (jwtParts.size == 3) {
        //we're only really interested in the body/payload
        val decodedPayload =
            String(Base64.decode(jwtParts[1], Base64.DEFAULT))
        Gson().fromJson(decodedPayload, SafetyNetResponse::class.java)
    } else {
        null
    }
}

interface SafetyNetWrapperCallback {
    fun error(errorCode: Int, errorMessage: String?)
    fun success(ctsProfileMatch: Boolean, basicIntegrity: Boolean)
}