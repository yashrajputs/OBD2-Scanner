package com.odb.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.odb.myapplication.ui.theme.ObdSystemTheme
import com.odb.myapplication.ui.navigation.MainNavigation
import androidx.activity.viewModels
import com.odb.myapplication.ui.ObdViewModel
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestBluetoothPermissionsIfNeeded()
        setContent {
            ObdSystemTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val vm: ObdViewModel by viewModels()
                    MainNavigation(
                        vm = vm,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
    private fun requestBluetoothPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val needsConnect = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
            val needsScan = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
            if (needsConnect || needsScan) {
                val launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
                launcher.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN))
            }
        }
    }
}
