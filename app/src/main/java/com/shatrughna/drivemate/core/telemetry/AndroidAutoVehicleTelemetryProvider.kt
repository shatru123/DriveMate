package com.shatrughna.drivemate.core.telemetry

import androidx.car.app.CarContext
import androidx.car.app.hardware.CarHardwareManager
import androidx.car.app.hardware.common.CarValue
import androidx.car.app.hardware.common.OnCarDataAvailableListener
import androidx.car.app.hardware.info.CarInfo
import androidx.car.app.hardware.info.EnergyLevel
import androidx.car.app.hardware.info.Mileage
import androidx.car.app.hardware.info.Speed
import androidx.core.content.ContextCompat
import com.shatrughna.drivemate.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AndroidAutoVehicleTelemetryProvider(
    private val carContext: CarContext
) : VehicleTelemetryProvider {

    override val name: String = "Android Auto Car Hardware"
    override val source: TelemetrySource = TelemetrySource.ANDROID_AUTO_CAR_HARDWARE

    private val _telemetry = MutableStateFlow(
        VehicleTelemetry(
            isCarConnected = true,
            speedSource = TelemetrySource.ANDROID_AUTO_CAR_HARDWARE,
            odometerSource = TelemetrySource.ANDROID_AUTO_CAR_HARDWARE,
            fuelSource = TelemetrySource.ANDROID_AUTO_CAR_HARDWARE
        )
    )
    override val telemetry: StateFlow<VehicleTelemetry> = _telemetry.asStateFlow()

    private var carInfo: CarInfo? = null
    private var isRegistered = false

    override val isAvailable: Boolean
        get() = isRegistered && carInfo != null

    private val speedListener = OnCarDataAvailableListener<Speed> { speed ->
        try {
            val displayMps = speed.displaySpeedMetersPerSecond.value
            val rawMps = speed.rawSpeedMetersPerSecond.value
            val effectiveMps = displayMps ?: rawMps

            val speedKmh = if (effectiveMps != null && effectiveMps >= 0f) {
                effectiveMps * 3.6f
            } else null

            val status = speed.displaySpeedMetersPerSecond.status
            val availability = mapCarValueStatus(status, speedKmh != null)

            AppLogger.d(
                AppLogger.TAG_CAR_HARDWARE,
                "Speed updated: display=$displayMps m/s, raw=$rawMps m/s, converted=${speedKmh} km/h, status=$status"
            )

            _telemetry.update { current ->
                current.copy(
                    speedKmh = speedKmh,
                    speedSource = TelemetrySource.ANDROID_AUTO_CAR_HARDWARE,
                    speedAvailability = availability,
                    isCarConnected = true,
                    lastUpdatedTimestamp = System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            AppLogger.e(AppLogger.TAG_CAR_HARDWARE, "Error handling Speed callback", e)
        }
    }

    private val mileageListener = OnCarDataAvailableListener<Mileage> { mileage ->
        try {
            val meters = mileage.odometerMeters.value
            val odoKm = if (meters != null && meters >= 0f) {
                meters / 1000.0
            } else null

            val status = mileage.odometerMeters.status
            val availability = mapCarValueStatus(status, odoKm != null)

            AppLogger.d(
                AppLogger.TAG_CAR_HARDWARE,
                "Mileage updated: meters=$meters, converted=${odoKm} km, status=$status"
            )

            _telemetry.update { current ->
                current.copy(
                    vehicleOdometerKm = odoKm,
                    odometerSource = TelemetrySource.ANDROID_AUTO_CAR_HARDWARE,
                    odometerAvailability = availability,
                    isCarConnected = true,
                    lastUpdatedTimestamp = System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            AppLogger.e(AppLogger.TAG_CAR_HARDWARE, "Error handling Mileage callback", e)
        }
    }

    private val energyLevelListener = OnCarDataAvailableListener<EnergyLevel> { energyLevel ->
        try {
            val fuelPercent = energyLevel.fuelPercent.value
            val rangeMeters = energyLevel.rangeRemainingMeters.value
            val rangeKm = if (rangeMeters != null && rangeMeters >= 0f) {
                rangeMeters / 1000f
            } else null

            val status = energyLevel.fuelPercent.status
            val availability = mapCarValueStatus(status, fuelPercent != null)

            AppLogger.d(
                AppLogger.TAG_CAR_HARDWARE,
                "EnergyLevel updated: fuel=${fuelPercent}%, range=${rangeKm} km, status=$status"
            )

            _telemetry.update { current ->
                current.copy(
                    fuelLevelPercent = fuelPercent,
                    rangeRemainingKm = rangeKm,
                    fuelSource = TelemetrySource.ANDROID_AUTO_CAR_HARDWARE,
                    fuelAvailability = availability,
                    isCarConnected = true,
                    lastUpdatedTimestamp = System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            AppLogger.e(AppLogger.TAG_CAR_HARDWARE, "Error handling EnergyLevel callback", e)
        }
    }

    override fun start() {
        if (isRegistered) {
            AppLogger.d(AppLogger.TAG_CAR_HARDWARE, "CarHardware listeners already started")
            return
        }

        try {
            AppLogger.i(AppLogger.TAG_CAR_HARDWARE, "Acquiring CarHardwareManager from CarContext...")
            val hardwareManager = carContext.getCarService(CarHardwareManager::class.java)
            if (hardwareManager == null) {
                AppLogger.w(AppLogger.TAG_CAR_HARDWARE, "CarHardwareManager not available on this host")
                _telemetry.update {
                    it.copy(
                        speedAvailability = TelemetryAvailability.UNAVAILABLE,
                        odometerAvailability = TelemetryAvailability.UNAVAILABLE,
                        fuelAvailability = TelemetryAvailability.UNAVAILABLE
                    )
                }
                return
            }

            carInfo = hardwareManager.carInfo
            val info = carInfo
            if (info == null) {
                AppLogger.w(AppLogger.TAG_CAR_HARDWARE, "CarInfo not available on this host")
                return
            }

            val executor = ContextCompat.getMainExecutor(carContext)

            // Register Speed
            try {
                info.addSpeedListener(executor, speedListener)
                AppLogger.i(AppLogger.TAG_CAR_HARDWARE, "Registered addSpeedListener")
            } catch (se: SecurityException) {
                AppLogger.e(AppLogger.TAG_CAR_HARDWARE, "Permission denied for CAR_SPEED", se)
                _telemetry.update { it.copy(speedAvailability = TelemetryAvailability.NOT_AUTHORIZED) }
            } catch (e: Exception) {
                AppLogger.e(AppLogger.TAG_CAR_HARDWARE, "Failed to register speed listener", e)
                _telemetry.update { it.copy(speedAvailability = TelemetryAvailability.NOT_SUPPORTED) }
            }

            // Register Mileage
            try {
                info.addMileageListener(executor, mileageListener)
                AppLogger.i(AppLogger.TAG_CAR_HARDWARE, "Registered addMileageListener")
            } catch (se: SecurityException) {
                AppLogger.e(AppLogger.TAG_CAR_HARDWARE, "Permission denied for CAR_MILEAGE", se)
                _telemetry.update { it.copy(odometerAvailability = TelemetryAvailability.NOT_AUTHORIZED) }
            } catch (e: Exception) {
                AppLogger.e(AppLogger.TAG_CAR_HARDWARE, "Failed to register mileage listener", e)
                _telemetry.update { it.copy(odometerAvailability = TelemetryAvailability.NOT_SUPPORTED) }
            }

            // Register Energy Level
            try {
                info.addEnergyLevelListener(executor, energyLevelListener)
                AppLogger.i(AppLogger.TAG_CAR_HARDWARE, "Registered addEnergyLevelListener")
            } catch (se: SecurityException) {
                AppLogger.e(AppLogger.TAG_CAR_HARDWARE, "Permission denied for CAR_FUEL/ENERGY", se)
                _telemetry.update { it.copy(fuelAvailability = TelemetryAvailability.NOT_AUTHORIZED) }
            } catch (e: Exception) {
                AppLogger.e(AppLogger.TAG_CAR_HARDWARE, "Failed to register energy listener", e)
                _telemetry.update { it.copy(fuelAvailability = TelemetryAvailability.NOT_SUPPORTED) }
            }

            isRegistered = true
            AppLogger.i(AppLogger.TAG_CAR_HARDWARE, "All CarHardware listeners registered successfully")

        } catch (e: Exception) {
            AppLogger.e(AppLogger.TAG_CAR_HARDWARE, "Fatal error starting CarHardware listeners", e)
        }
    }

    override fun stop() {
        if (!isRegistered) return

        AppLogger.i(AppLogger.TAG_CAR_HARDWARE, "Stopping and unregistering CarHardware listeners")
        val info = carInfo
        if (info != null) {
            try {
                info.removeSpeedListener(speedListener)
                AppLogger.d(AppLogger.TAG_CAR_HARDWARE, "Unregistered speedListener")
            } catch (e: Exception) {
                AppLogger.w(AppLogger.TAG_CAR_HARDWARE, "Error removing speedListener: ${e.message}")
            }

            try {
                info.removeMileageListener(mileageListener)
                AppLogger.d(AppLogger.TAG_CAR_HARDWARE, "Unregistered mileageListener")
            } catch (e: Exception) {
                AppLogger.w(AppLogger.TAG_CAR_HARDWARE, "Error removing mileageListener: ${e.message}")
            }

            try {
                info.removeEnergyLevelListener(energyLevelListener)
                AppLogger.d(AppLogger.TAG_CAR_HARDWARE, "Unregistered energyLevelListener")
            } catch (e: Exception) {
                AppLogger.w(AppLogger.TAG_CAR_HARDWARE, "Error removing energyLevelListener: ${e.message}")
            }
        }

        isRegistered = false
        carInfo = null
        _telemetry.update { current ->
            current.copy(
                isCarConnected = false,
                speedAvailability = TelemetryAvailability.NOT_CONNECTED,
                odometerAvailability = if (current.vehicleOdometerKm != null) TelemetryAvailability.STALE else TelemetryAvailability.NOT_CONNECTED,
                fuelAvailability = if (current.fuelLevelPercent != null) TelemetryAvailability.STALE else TelemetryAvailability.NOT_CONNECTED
            )
        }
    }

    private fun mapCarValueStatus(status: Int, hasNonNullValue: Boolean): TelemetryAvailability {
        return when (status) {
            CarValue.STATUS_SUCCESS -> TelemetryAvailability.LIVE
            CarValue.STATUS_UNAVAILABLE -> TelemetryAvailability.UNAVAILABLE
            CarValue.STATUS_UNIMPLEMENTED -> TelemetryAvailability.NOT_SUPPORTED
            else -> if (hasNonNullValue) TelemetryAvailability.LIVE else TelemetryAvailability.UNAVAILABLE
        }
    }
}
