package ch.drivedeck.core.model

/** Stable, dependency-free representation suitable for DataStore. */
object DashboardLayoutCodec {
    fun encode(items: List<DashboardItem>): String =
        items.joinToString(",") { "${it.type.name}:${it.size.name}" }

    fun decode(value: String?): List<DashboardItem> {
        if (value.isNullOrBlank()) return DefaultDashboardItems
        val decoded = value.split(',').mapNotNull { entry ->
            val parts = entry.split(':')
            if (parts.size != 2) return@mapNotNull null
            val type = runCatching { DashboardElementType.valueOf(parts[0]) }.getOrNull()
            val size = runCatching { DashboardElementSize.valueOf(parts[1]) }.getOrNull()
            if (type == null || size == null) null else DashboardItem(type, size)
        }.distinctBy(DashboardItem::type)
        return decoded.takeIf { it.isNotEmpty() } ?: DefaultDashboardItems
    }
}
