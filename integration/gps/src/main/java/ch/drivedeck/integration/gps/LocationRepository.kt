package ch.drivedeck.integration.gps

import kotlinx.coroutines.flow.StateFlow

interface LocationRepository {
    val reading: StateFlow<LocationReading>
    fun start()
    fun stop()
}
