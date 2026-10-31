package com.carlauncherc.launcher.media

/** Package IDs for YMusic v4 and the earlier Android app. */
object YMusicPackages {
    const val V4 = "io.ymusic.android"
    const val V4_DEBUG = "io.ymusic.android.debug"
    const val LEGACY = "com.kapp.youtube.final"

    fun isYMusic(packageName: String?): Boolean =
        packageName == V4 || packageName == V4_DEBUG || packageName == LEGACY

    /** Only the YMusic v4 app understands the direct start-link behaviour. */
    fun isV4(packageName: String?): Boolean = packageName == V4 || packageName == V4_DEBUG

    /** Match the user's music selection first; otherwise prefer the newer media session. */
    fun selectSessionPackage(activePackages: List<String>, selectedPackage: String?): String? {
        if (selectedPackage != null &&
            isYMusic(selectedPackage) &&
            selectedPackage in activePackages
        ) return selectedPackage
        return listOf(V4, V4_DEBUG, LEGACY).firstOrNull { it in activePackages }
    }
}
