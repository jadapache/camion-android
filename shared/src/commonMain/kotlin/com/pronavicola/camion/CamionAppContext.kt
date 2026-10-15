package com.pronavicola.camion

import com.pronavicola.camion.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * Backwards-compatibility wrapper around [AppContainer].
 */
typealias CamionAppContext = AppContainer

fun createCamionAppContext(
    appScope: CoroutineScope = CoroutineScope(SupervisorJob()),
    platformContext: Any? = null,
): AppContainer = AppContainer(appScope, platformContext)
