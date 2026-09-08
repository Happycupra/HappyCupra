package ch.drivedeck.core.model

data class LaunchableApp(val packageName: String, val activityName: String, val label: String)

enum class ThemeMode { DARK, LIGHT, AUTO }

enum class QuickAction { HOME, NAVIGATION, MUSIC, PHONE, APPS, SETTINGS }

val DefaultQuickActions = listOf(
    QuickAction.HOME,
    QuickAction.NAVIGATION,
    QuickAction.MUSIC,
    QuickAction.PHONE,
    QuickAction.APPS,
)

enum class DashboardElementType { NAVIGATION, MEDIA, PHONE, RADIO, SPEED, WEATHER }

enum class DashboardElementSize {
    SMALL, MEDIUM, LARGE, WIDE;

    fun next(): DashboardElementSize = entries[(ordinal + 1) % entries.size]
}

data class DashboardItem(
    val type: DashboardElementType,
    val size: DashboardElementSize,
)

val DefaultDashboardItems = listOf(
    DashboardItem(DashboardElementType.NAVIGATION, DashboardElementSize.LARGE),
    DashboardItem(DashboardElementType.MEDIA, DashboardElementSize.WIDE),
    DashboardItem(DashboardElementType.SPEED, DashboardElementSize.SMALL),
    DashboardItem(DashboardElementType.WEATHER, DashboardElementSize.SMALL),
    DashboardItem(DashboardElementType.PHONE, DashboardElementSize.MEDIUM),
    DashboardItem(DashboardElementType.RADIO, DashboardElementSize.MEDIUM),
)

data class UserPreferences(
    val favoritePackages: Set<String> = emptySet(),
    val themeMode: ThemeMode = ThemeMode.DARK,
    val editModeEnabled: Boolean = false,
    val dashboardItems: List<DashboardItem> = DefaultDashboardItems,
    val quickActions: List<QuickAction> = DefaultQuickActions,
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
