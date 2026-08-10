package org.microg.installer.updater.data

data class ReleaseInfo(
    val tagName: String,
    val gmsUrl: String?,
    val gmsVersionName: String?,
    val vendingUrl: String?,
    val vendingVersionName: String?
)
