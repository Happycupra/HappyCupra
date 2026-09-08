package ch.drivedeck.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class QuickActionsCodecTest {
    @Test fun codecRetainsUniqueOrderedActions() {
        val actions = listOf(QuickAction.HOME, QuickAction.MUSIC, QuickAction.SETTINGS)
        assertEquals(actions, QuickActionsCodec.decode(QuickActionsCodec.encode(actions)))
    }

    @Test fun codecRejectsDuplicatesUnknownValuesAndTooManySlots() {
        assertEquals(
            listOf(QuickAction.HOME, QuickAction.APPS, QuickAction.PHONE, QuickAction.MUSIC, QuickAction.SETTINGS),
            QuickActionsCodec.decode("HOME,NOPE,HOME,APPS,PHONE,MUSIC,SETTINGS,NAVIGATION"),
        )
    }

    @Test fun emptyLayoutUsesDrivingDefaults() {
        assertEquals(DefaultQuickActions, QuickActionsCodec.decode(null))
    }
}
