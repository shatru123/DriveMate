package com.shatrughna.drivemate.core.telemetry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class VehicleTelemetryViewModel(
    private val repository: VehicleTelemetryRepository
) : ViewModel() {

    val telemetry: StateFlow<VehicleTelemetry> = repository.telemetry
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = VehicleTelemetry()
        )

    fun getDiagnostics(): Map<String, String> {
        return repository.getDiagnostics()
    }

    class Factory(
        private val repository: VehicleTelemetryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return VehicleTelemetryViewModel(repository) as T
        }
    }
}
