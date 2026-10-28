package com.example.advancedtracker

import app.cash.turbine.test
import com.example.advancedtracker.engine.TelemetryEngine
import com.example.advancedtracker.domain.usecase.DeviceId
import com.example.advancedtracker.presentation.TelemetryUiState
import com.example.advancedtracker.presentation.TelemetryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TelemetryTrackerTests {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var telemetryEngine: TelemetryEngine
    private lateinit var viewModel: TelemetryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        telemetryEngine = TelemetryEngine(defaultDispatcher = testDispatcher, ioDispatcher = testDispatcher)
        viewModel = TelemetryViewModel(telemetryEngine, dispatcher = testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `cuando se inicia el monitoreo, cambia el estado a cargando y luego emite telemetria activa`() = runTest(testDispatcher) {
        val targetId = DeviceId("PRO_TRACK_001")

        viewModel.uiState.test {
            assertEquals(TelemetryUiState.Idle, awaitItem())

            viewModel.startTracking(targetId, 1000L)

            assertEquals(TelemetryUiState.Loading, awaitItem())

            advanceTimeBy(1005)

            val activeState = awaitItem()
            assertTrue(activeState is TelemetryUiState.Active)

            val payload = (activeState as TelemetryUiState.Active).data
            assertEquals("PRO_TRACK_001", payload.deviceId.value)

            viewModel.stopTracking()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cuando ocurre una lectura anomalas, se emite una alerta a traves del flujo caliente SharedFlow`() = runTest(testDispatcher) {
        val targetId = DeviceId("ANOMALY_MONITOR")

        viewModel.alertEvents.test {
            viewModel.startTracking(targetId, 100L)

            advanceTimeBy(300)

            val alertMessage = awaitItem()
            assertTrue(alertMessage.contains("ANOMALY_MONITOR") || alertMessage.contains("¡ANOMALÍA DETECTADA!"))

            viewModel.stopTracking()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
