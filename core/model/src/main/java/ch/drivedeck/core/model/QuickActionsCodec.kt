package ch.drivedeck.core.model

object QuickActionsCodec {
    const val MaxActions = 5

    fun encode(actions: List<QuickAction>): String = actions.take(MaxActions).joinToString(",", transform = QuickAction::name)

    fun decode(value: String?): List<QuickAction> {
        val actions = value.orEmpty().split(',')
            .mapNotNull { runCatching { QuickAction.valueOf(it) }.getOrNull() }
            .distinct()
            .take(MaxActions)
        return actions.takeIf { it.isNotEmpty() } ?: DefaultQuickActions
    }
}
