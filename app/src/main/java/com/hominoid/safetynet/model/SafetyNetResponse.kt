package com.hominoid.safetynet.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class SafetyNetResponse {
    @SerializedName("nonce")
    @Expose
    var nonce: String? = null

    @SerializedName("timestampMs")
    @Expose
    var timestampMs: Long = 0

    @SerializedName("apkPackageName")
    @Expose
    var apkPackageName: String? = null

    @SerializedName("apkDigestSha256")
    @Expose
    var apkDigestSha256: String? = null

    @SerializedName("ctsProfileMatch")
    @Expose
    var isCtsProfileMatch = false

    @SerializedName("apkCertificateDigestSha256")
    @Expose
    var apkCertificateDigestSha256: List<String>? = null

    @SerializedName("basicIntegrity")
    @Expose
    var isBasicIntegrity = false

    @SerializedName("evaluationType")
    @Expose
    var evaluationType: String? = null
}