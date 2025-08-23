package com.odb.myapplication.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.odb.myapplication.ui.dashboard.ObdDashboardScreen
import com.odb.myapplication.ui.ObdViewModel
import com.odb.myapplication.ui.splash.SplashScreen

@Composable
fun MainNavigation(
    vm: ObdViewModel,
    modifier: Modifier = Modifier
) {
    var showSplash by remember { mutableStateOf(true) }
    
    if (showSplash) {
        SplashScreen(
            onSplashComplete = {
                showSplash = false
            },
            modifier = modifier
        )
    } else {
        ObdDashboardScreen(
            vm = vm,
            modifier = modifier
        )
    }
}
