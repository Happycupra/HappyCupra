pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "DriveDeck"
include(":app", ":core:model", ":core:preferences", ":core:design", ":feature:home", ":feature:apps", ":feature:settings")
include(":integration:media")
include(":integration:gps")
