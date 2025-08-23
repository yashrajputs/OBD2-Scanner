package com.odb.myapplication.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.odb.myapplication.data.BluetoothObdRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ObdViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = BluetoothObdRepository(app)

    private val _rpm = MutableStateFlow(0f)
    val rpm: StateFlow<Float> = _rpm

    private val _speed = MutableStateFlow(0f)
    val speed: StateFlow<Float> = _speed

    private val _coolant = MutableStateFlow(0f)
    val coolant: StateFlow<Float> = _coolant

    // Additional OBD2 parameters
    private val _fuelLevel = MutableStateFlow(0f)
    val fuelLevel: StateFlow<Float> = _fuelLevel

    private val _engineLoad = MutableStateFlow(0f)
    val engineLoad: StateFlow<Float> = _engineLoad

    private val _throttlePosition = MutableStateFlow(0f)
    val throttlePosition: StateFlow<Float> = _throttlePosition

    private val _mafFlow = MutableStateFlow(0f)
    val mafFlow: StateFlow<Float> = _mafFlow

    private val _intakeTemp = MutableStateFlow(0f)
    val intakeTemp: StateFlow<Float> = _intakeTemp

    private val _fuelPressure = MutableStateFlow(0f)
    val fuelPressure: StateFlow<Float> = _fuelPressure

    private val _fuelRailPressure = MutableStateFlow(0f)
    val fuelRailPressure: StateFlow<Float> = _fuelRailPressure

    private val _o2Sensor1 = MutableStateFlow(0f)
    val o2Sensor1: StateFlow<Float> = _o2Sensor1

    private val _o2Sensor2 = MutableStateFlow(0f)
    val o2Sensor2: StateFlow<Float> = _o2Sensor2

    private val _o2Sensor3 = MutableStateFlow(0f)
    val o2Sensor3: StateFlow<Float> = _o2Sensor3

    private val _o2Sensor4 = MutableStateFlow(0f)
    val o2Sensor4: StateFlow<Float> = _o2Sensor4

    private val _ambientTemp = MutableStateFlow(0f)
    val ambientTemp: StateFlow<Float> = _ambientTemp

    private val _barometricPressure = MutableStateFlow(0f)
    val barometricPressure: StateFlow<Float> = _barometricPressure

    private val _manifoldPressure = MutableStateFlow(0f)
    val manifoldPressure: StateFlow<Float> = _manifoldPressure

    private val _controlVoltage = MutableStateFlow(0f)
    val controlVoltage: StateFlow<Float> = _controlVoltage

    private val _shortFuelTrim1 = MutableStateFlow(0f)
    val shortFuelTrim1: StateFlow<Float> = _shortFuelTrim1

    private val _longFuelTrim1 = MutableStateFlow(0f)
    val longFuelTrim1: StateFlow<Float> = _longFuelTrim1

    private val _shortFuelTrim2 = MutableStateFlow(0f)
    val shortFuelTrim2: StateFlow<Float> = _shortFuelTrim2

    private val _longFuelTrim2 = MutableStateFlow(0f)
    val longFuelTrim2: StateFlow<Float> = _longFuelTrim2

    private val _lastRx = MutableStateFlow("")
    @Suppress("unused")
    val lastRx: StateFlow<String> = _lastRx

    @Suppress("unused")
    val connected: StateFlow<Boolean> = repo.connected
    @Suppress("unused")
    val status: StateFlow<String> = repo.status

    fun pairedDeviceNames(): List<String> {
        return try {
            val mgr = getApplication<Application>().getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
            val adapter = mgr?.adapter
            adapter?.bondedDevices?.map { it.name ?: it.address } ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    fun connect(deviceName: String = "ESP32_OBD2_Scanner") {
        viewModelScope.launch {
            repo.connectByName(deviceName, onLine = { raw ->
                val line = raw.trim()
                if (line.isEmpty()) return@connectByName
                _lastRx.value = line

                // Try multi-key extraction from a single line first
                if (extractAllKeys(line)) {
                    return@connectByName
                }

                // 1) JSON payload: {"rpm":850,"speed":10,"coolant":87}
                if (line.startsWith("{") && line.endsWith("}")) {
                    jsonUpdate(line)
                    return@connectByName
                }

                // 2) CSV payload: rpm,speed,coolant (numbers only)
                val csvValues = line.split(',').mapNotNull { it.trim().toFloatOrNull() }
                if (csvValues.size >= 3) {
                    _rpm.value = csvValues[0]
                    _speed.value = csvValues[1]
                    _coolant.value = csvValues[2]
                    return@connectByName
                }

                // 3) Keyed text lines like "Engine RPM: 850" or "rpm=850"
                val value = extractFirstNumber(line) ?: return@connectByName
                val keyPart = line.substringBefore(":").lowercase()

                when {
                    keyPart.contains("rpm") || keyPart.contains("engine rpm") -> _rpm.value = value
                    keyPart.contains("speed") || keyPart.contains("vehicle speed") || keyPart.contains("spd") -> _speed.value = value
                    keyPart.contains("coolant") || keyPart.contains("engine coolant") || keyPart.contains("ect") -> _coolant.value = value
                    // Also support key=value minimal format
                    line.contains("=") -> {
                        val k = line.substringBefore("=").lowercase()
                        when {
                            k.contains("rpm") || k.contains("engine rpm") -> _rpm.value = value
                            k.contains("speed") || k.contains("vehicle speed") || k.contains("spd") -> _speed.value = value
                            k.contains("coolant") || k.contains("ect") || k.contains("ect") -> _coolant.value = value
                        }
                    }
                }

                // 4) Fallback: any line with 3+ numbers -> rpm, speed, coolant in order
                val allNumbers = Regex("-?\\d+(?:\\.\\d+)?").findAll(line).mapNotNull { it.value.toFloatOrNull() }.toList()
                if (allNumbers.size >= 3) {
                    _rpm.value = allNumbers[0]
                    _speed.value = allNumbers[1]
                    _coolant.value = allNumbers[2]
                }
            }, onError = { /* could expose error state */ })
        }
    }

    private val floatRegex = Regex("-?\\d+(?:\\.\\d+)?")
    private fun extractFirstNumber(s: String): Float? =
        floatRegex.find(s)?.value?.toFloatOrNull()

    // Extract any of rpm/speed/coolant regardless of separators; returns true if at least one was applied
    private fun extractAllKeys(text: String): Boolean {
        var applied = false
        fun findAndSet(vararg names: String, setter: (Float) -> Unit) {
            val joined = names.joinToString("|") { Regex.escape(it) }
            val r = Regex("(?i)($joined)\\s*[:=]?\\s*(-?\\d+(?:\\.\\d+)?)")
            val m = r.find(text)
            val v = m?.groupValues?.getOrNull(1)?.toFloatOrNull()
            if (v != null) { setter(v); applied = true }
        }
        findAndSet("rpm", "engine rpm") { _rpm.value = it }
        findAndSet("speed", "vehicle speed", "spd") { _speed.value = it }
        findAndSet("coolant", "engine coolant", "ect") { _coolant.value = it }
        return applied
    }

    private fun jsonUpdate(json: String) {
        // Very lightweight JSON parsing without bringing a dependency
        fun find(key: String): Float? {
            val r = Regex("\"$key\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)")
            return r.find(json)?.groupValues?.getOrNull(1)?.toFloatOrNull()
        }
        
        // RPM aliases - engineRPM is the key your ESP32 sends
        find("engineRPM")?.let { _rpm.value = it }
            ?: find("EngineRPM")?.let { _rpm.value = it }
            ?: find("rpm")?.let { _rpm.value = it }
            ?: find("RPM")?.let { _rpm.value = it }

        // Speed aliases (mph converted to km/h if present)
        val mph = find("mph") ?: find("speed_mph") ?: find("speedMph")
        if (mph != null) {
            _speed.value = mph * 1.60934f
        } else {
            val kmh = find("kmh") ?: find("speed_kmh") ?: find("speedKmh")
            val generic = kmh ?: find("vehicleSpeed") ?: find("VehicleSpeed") ?: find("speed")
            generic?.let { _speed.value = it }
        }

        // Coolant aliases
        find("coolant")?.let { _rpm.value = it }
            ?: find("coolantTemp")?.let { _coolant.value = it }
            ?: find("engineCoolantTemp")?.let { _coolant.value = it }
            ?: find("ECT")?.let { _coolant.value = it }
            ?: find("ect")?.let { _rpm.value = it }

        // Fuel Level
        find("fuelLevel")?.let { _fuelLevel.value = it }

        // Engine Performance
        find("engineLoad")?.let { _engineLoad.value = it }
        find("throttlePos")?.let { _throttlePosition.value = it }
        find("mafFlow")?.let { _mafFlow.value = it }
        find("intakeTemp")?.let { _intakeTemp.value = it }

        // Fuel System
        find("fuelPressure")?.let { _fuelPressure.value = it }
        find("fuelRailPressure")?.let { _fuelRailPressure.value = it }

        // Oxygen Sensors
        find("o2Voltage1")?.let { _o2Sensor1.value = it }
        find("o2Voltage2")?.let { _o2Sensor2.value = it }
        find("o2Voltage3")?.let { _o2Sensor3.value = it }
        find("o2Voltage4")?.let { _o2Sensor4.value = it }

        // Environmental
        find("ambientTemp")?.let { _ambientTemp.value = it }
        find("barometricPressure")?.let { _barometricPressure.value = it }
        find("manifoldPressure")?.let { _manifoldPressure.value = it }
        find("controlVoltage")?.let { _controlVoltage.value = it }

        // Fuel Trims
        find("shortFuelTrim1")?.let { _shortFuelTrim1.value = it }
        find("longFuelTrim1")?.let { _longFuelTrim1.value = it }
        find("shortFuelTrim2")?.let { _shortFuelTrim2.value = it }
        find("longFuelTrim2")?.let { _longFuelTrim2.value = it }
    }

    override fun onCleared() {
        super.onCleared()
        repo.close()
    }
}
