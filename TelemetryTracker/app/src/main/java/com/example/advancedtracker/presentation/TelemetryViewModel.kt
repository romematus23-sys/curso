
package com.example.advancedtracker.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.advancedtracker.data.engine.TelemetryEngine
import com.example.advancedtracker.domain.model.DeviceId
import com.example.advancedtracker.domain.model.TelemetryData


import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch

class TelemetryViewModel(
    private val telemetryEngine: TelemetryEngine,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

    // Estado UI Caliente: Retiene el último estado emitido de la telemetría
    private val _uiState = MutableStateFlow<TelemetryUiState>(TelemetryUiState.Idle)
    val uiState: StateFlow<TelemetryUiState> = _uiState.asStateFlow()

    // Flujo de Eventos Únicos Caliente: No retiene estado, ideal para alertas o notificaciones "one-shot"
    private val _alertEvents = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val alertEvents: SharedFlow<String> = _alertEvents.asSharedFlow()

    private var telemetryJob: Job? = null

    fun startTracking(deviceId: DeviceId, intervalMs: Long) {
        // Cancelamos cualquier job previo de monitoreo para evitar fugas de memoria o corrutinas huérfanas
        telemetryJob?.cancel()
        _uiState.value = TelemetryUiState.Loading

        telemetryJob = viewModelScope.launch(dispatcher) {
            telemetryEngine.startTelemetryEmission(deviceId, intervalMs)
                .conflate()
                .catch { exception ->
                    _uiState.value = TelemetryUiState.Error(
                        exception.localizedMessage ?: "Error de Conexión"
                    )
                }
                .collect { telemetryData ->
                    _uiState.value = TelemetryUiState.Active(telemetryData)

                    // Evaluar anomalías en tiempo real para disparar el flujo caliente de alertas directas
                    if (telemetryData.isAnomaly) {
                        _alertEvents.emit(
                            "¡ANOMALÍA DETECTADA! El dispositivo ${deviceId.value} superó límites críticos. Temp: ${"%.1f".format(telemetryData.temperatureCelsius)}°C."
                        )
                    }
                }
        }
    }

    fun stopTracking() {
        telemetryJob?.cancel()
        _uiState.value = TelemetryUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        stopTracking()
    }
}

// Interfaz de sellado para representar los estados explícitos de la interfaz de usuario
sealed interface TelemetryUiState {
    object Idle : TelemetryUiState
    object Loading : TelemetryUiState
    data class Active(val data: TelemetryData) : TelemetryUiState
    data class Error(val message: String) : TelemetryUiState
}
