package com.hominoid.safetynet.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Base64
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.*

class Utils internal constructor() {
    private val secureRandom: SecureRandom

    init {
        secureRandom = SecureRandom()
    }

    fun generateOneTimeRequestNonce(): ByteArray {
        val nonce = ByteArray(32)
        secureRandom.nextBytes(nonce)
        return nonce
    }

    companion object {
        private const val SHA_256 = "SHA-256"
        fun calcApkCertificateDigests(context: Context, packageName: String?): List<String> {
            val encodedSignatures: MutableList<String> = ArrayList()

            // Get signatures from package manager
            val pm = context.packageManager
            val packageInfo: PackageInfo
            packageInfo = try {
                pm.getPackageInfo(packageName!!, PackageManager.GET_SIGNATURES)
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
                return encodedSignatures
            }
            val signatures = packageInfo.signatures

            // Calculate b64 encoded sha256 hash of signatures
            for (signature in signatures) {
                try {
                    val md = MessageDigest.getInstance(SHA_256)
                    md.update(signature.toByteArray())
                    val digest = md.digest()
                    encodedSignatures.add(Base64.encodeToString(digest, Base64.NO_WRAP))
                } catch (e: NoSuchAlgorithmException) {
                    e.printStackTrace()
                }
            }
            return encodedSignatures
        }
    }
}