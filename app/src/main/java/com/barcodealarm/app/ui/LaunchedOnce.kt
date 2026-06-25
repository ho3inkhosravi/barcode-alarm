package com.barcodealarm.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/**
 * Runs [block] only once when first composed (and whenever [key] changes).
 */
@Composable
fun LaunchedOnce(key: Any?, block: () -> Unit) {
    val fired = remember { mutableStateOf(false) }
    LaunchedEffect(key) {
        if (!fired.value || key != null) {
            fired.value = true
            block()
        }
    }
}
