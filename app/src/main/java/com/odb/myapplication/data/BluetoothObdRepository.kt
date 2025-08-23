package com.odb.myapplication.data

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.annotation.SuppressLint
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.UUID

class BluetoothObdRepository(private val context: Context) {
    private val bluetoothAdapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    private var socket: BluetoothSocket? = null
    private var output: OutputStream? = null
    private var reader: BufferedReader? = null
    private var ioScope: CoroutineScope? = null

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected
    private val _status = MutableStateFlow("Idle")
    val status: StateFlow<String> = _status

    @SuppressLint("MissingPermission")
    fun connectByName(deviceName: String, onLine: (String) -> Unit, onError: (Throwable) -> Unit) {
        val adapter = bluetoothAdapter ?: run {
            onError(IllegalStateException("Bluetooth adapter not available"))
            return
        }
        if (!hasBluetoothPermissions()) {
            onError(SecurityException("Bluetooth permissions not granted"))
            return
        }
        try {
            _status.value = "Discovering paired devices"
            val device: BluetoothDevice = try {
                adapter.bondedDevices.firstOrNull {
                    val n = it.name ?: ""
                    n.equals(deviceName, ignoreCase = true) || n.contains(deviceName, ignoreCase = true)
                }
            } catch (_: SecurityException) { null }
                ?: run {
                    onError(IllegalStateException("Device not paired: $deviceName"))
                    _status.value = "Device not paired"
                    return
                }

            val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // SPP
            // Try secure socket first, then fall back to insecure
            socket = try {
                device.createRfcommSocketToServiceRecord(uuid)
            } catch (_: Throwable) {
                try { device.createInsecureRfcommSocketToServiceRecord(uuid) } catch (t: Throwable) {
                    onError(t); return
                }
            }
            try { if (hasBluetoothPermissions()) adapter.cancelDiscovery() } catch (_: SecurityException) {}
            _status.value = "Connecting to ${device.address}"
            try { socket?.connect() } catch (se: SecurityException) { onError(se); _status.value = "Error: ${se.message}"; close(); return }
            _connected.value = true
            _status.value = "Connected"
            output = socket?.outputStream
            reader = BufferedReader(InputStreamReader(socket!!.inputStream))
            // If ESP32 expects CRLF or a wake command, you can send one here
            // output?.write("\r\n".toByteArray())
            // output?.flush()
            ioScope = CoroutineScope(Dispatchers.IO)
            ioScope?.launch { readLoop(onLine, onError) }
        } catch (t: Throwable) {
            onError(t)
            _status.value = "Error: ${t.message}"
            close()
        }
    }

    @Suppress("unused")
    fun send(command: String) {
        output?.write((command + "\r").toByteArray())
        output?.flush()
    }

    fun close() {
        _connected.value = false
        _status.value = "Disconnected"
        try { reader?.close() } catch (_: Throwable) {}
        try { output?.close() } catch (_: Throwable) {}
        try { socket?.close() } catch (_: Throwable) {}
        ioScope?.cancel()
        ioScope = null
    }

    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val connect = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            val scan = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            connect && scan
        } else {
            true
        }
    }

    private fun readLoop(onLine: (String) -> Unit, onError: (Throwable) -> Unit) {
        try {
            val input = socket?.inputStream ?: return
            val buffer = ByteArray(1024)
            val sb = StringBuilder()
            while (_connected.value) {
                val available = input.read(buffer)
                if (available <= 0) break
                val chunk = String(buffer, 0, available)
                sb.append(chunk)
                var idx = sb.indexOf("\n")
                while (idx >= 0) {
                    var line = sb.substring(0, idx)
                    // Trim CR and whitespace
                    line = line.trim('\r', '\n', ' ')
                    if (line.isNotEmpty()) {
                        _status.value = "RX: $line"
                        onLine(line)
                    }
                    sb.delete(0, idx + 1)
                    idx = sb.indexOf("\n")
                }
            }
        } catch (t: Throwable) {
            onError(t)
            _status.value = "Error: ${t.message}"
        } finally {
            close()
        }
    }
}
