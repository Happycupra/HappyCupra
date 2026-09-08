package ch.drivedeck.core.model

data class LaunchableApp(val packageName: String, val activityName: String, val label: String)

enum class ThemeMode { DARK, LIGHT, AUTO }

data class UserPreferences(
    val favoritePackages: Set<String> = emptySet(),
    val themeMode: ThemeMode = ThemeMode.DARK,
    val editModeEnabled: Boolean = false,
)

data class DemoDriveData(
    val song: String = "Midnight Drive",
    val artist: String = "Demo Artist",
    val speedKmh: Int = 83,
    val destination: String = "Bern",
    val weather: String = "18°C",
) {
    companion object { val Preview = DemoDriveData() }
}
