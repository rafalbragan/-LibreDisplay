package com.libredisplay.service

import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

@OptIn(ExperimentalCoroutinesApi::class)
class RefreshController(
    private val intervalMs: Long = 60_000L
) {
    private val enabled = MutableStateFlow(true)

    fun stop() {
        enabled.value = false
    }

    fun resume() {
        enabled.value = true
    }

    fun isActive(): Boolean = enabled.value

    fun ticks(): Flow<Unit> = enabled.flatMapLatest { active ->
        if (!active) {
            emptyFlow()
        } else {
            flow {
                emit(Unit)
                while (true) {
                    delay(intervalMs)
                    emit(Unit)
                }
            }
        }
    }
}
