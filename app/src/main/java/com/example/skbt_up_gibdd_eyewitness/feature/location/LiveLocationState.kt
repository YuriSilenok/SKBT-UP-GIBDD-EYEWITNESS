package com.example.skbt_up_gibdd_eyewitness.feature.location

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface LiveLocationState {
    data object Idle : LiveLocationState

    data class Active(
        val startedAtMillis: Long,
        val endsAtMillis: Long,
        val latestLocation: StaticLocation? = null,
        val pointsRecorded: Int = 0,
    ) : LiveLocationState
}

object LiveLocationTracker {
    private val mutableState = MutableStateFlow<LiveLocationState>(LiveLocationState.Idle)
    val state = mutableState.asStateFlow()

    fun start(startedAtMillis: Long, endsAtMillis: Long) {
        mutableState.value = LiveLocationState.Active(startedAtMillis, endsAtMillis)
    }

    fun update(location: StaticLocation) {
        val current = mutableState.value as? LiveLocationState.Active ?: return
        mutableState.value = current.copy(
            latestLocation = location,
            pointsRecorded = current.pointsRecorded + 1,
        )
    }

    fun setLatest(location: StaticLocation) {
        val current = mutableState.value as? LiveLocationState.Active ?: return
        mutableState.value = current.copy(latestLocation = location)
    }

    fun stop() {
        mutableState.value = LiveLocationState.Idle
    }
}
