package com.carlauncherc.launcher.core

/**
 * Layout presets use stable names in SharedPreferences; this is pure Kotlin for JVM tests.
 * Users can also independently show/hide the navigation and music quick cards.
 */
enum class DashboardPreset(val stored: String) {
    CLASSIC("classic"),
    NAVIGATION("navigation"),
    MUSIC("music"),
    MINIMAL("minimal");

    companion object {
        fun parse(raw: String?): DashboardPreset =
            entries.firstOrNull { it.stored == raw } ?: CLASSIC
    }
}

data class DashboardArrangement(
    val gaugeOrder: List<String>,
    val gaugeWeights: List<Float>,
    val navWeight: Float,
    val musicWeight: Float,
    val quickCardsVisible: Boolean
)

object DashboardLayouts {
    fun arrange(
        preset: DashboardPreset,
        showNav: Boolean,
        showMusic: Boolean
    ): DashboardArrangement {
        val order = when (preset) {
            DashboardPreset.CLASSIC, DashboardPreset.MINIMAL ->
                listOf("speed", "clock", "compass")
            DashboardPreset.NAVIGATION -> listOf("clock", "speed", "compass")
            DashboardPreset.MUSIC -> listOf("speed", "compass", "clock")
        }
        val weights = when (preset) {
            DashboardPreset.CLASSIC, DashboardPreset.MINIMAL -> listOf(1f, 1.4f, 1f)
            DashboardPreset.NAVIGATION -> listOf(1.25f, 1.65f, 0.9f)
            DashboardPreset.MUSIC -> listOf(1.1f, 0.95f, 1.25f)
        }
        val quick = preset != DashboardPreset.MINIMAL && (showNav || showMusic)
        val navWeight = when (preset) {
            DashboardPreset.NAVIGATION -> 1.6f
            DashboardPreset.MUSIC -> 0.75f
            else -> 0.8f
        }
        val musicWeight = when (preset) {
            DashboardPreset.NAVIGATION -> 1.2f
            DashboardPreset.MUSIC -> 2.4f
            else -> 1.7f
        }
        return DashboardArrangement(order, weights, navWeight, musicWeight, quick)
    }
}
