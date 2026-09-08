package ch.drivedeck.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardLayoutCodecTest {
    @Test fun layoutRoundTripsWithoutLosingOrderOrSize() {
        val layout = listOf(
            DashboardItem(DashboardElementType.MEDIA, DashboardElementSize.WIDE),
            DashboardItem(DashboardElementType.NAVIGATION, DashboardElementSize.SMALL),
        )
        assertEquals(layout, DashboardLayoutCodec.decode(DashboardLayoutCodec.encode(layout)))
    }

    @Test fun invalidLayoutFallsBackToSafeDefault() {
        assertEquals(DefaultDashboardItems, DashboardLayoutCodec.decode("not:a-layout"))
    }
}
