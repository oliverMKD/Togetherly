package com.togetherly.app.application

import androidx.compose.runtime.Composable
import com.togetherly.designsystem.theme.TogetherlyTheme
import com.togetherly.navigation.host.TogetherlyNavHost

@Composable
fun App() {
    TogetherlyTheme {
        TogetherlyNavHost()
    }
}
