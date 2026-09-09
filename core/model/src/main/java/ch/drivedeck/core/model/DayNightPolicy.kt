package ch.drivedeck.core.model

/** Predictable offline schedule; sunrise/sunset support can replace this policy later. */
object DayNightPolicy {
    const val DayStartsAtHour = 7
    const val NightStartsAtHour = 19

    fun isNight(hourOfDay: Int): Boolean {
        require(hourOfDay in 0..23)
        return hourOfDay < DayStartsAtHour || hourOfDay >= NightStartsAtHour
    }
}
