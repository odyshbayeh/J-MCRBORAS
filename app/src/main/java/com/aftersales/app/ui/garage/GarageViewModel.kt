package com.aftersales.app.ui.garage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aftersales.app.data.api.ApiError
import com.aftersales.app.data.api.ApiResult
import com.aftersales.app.data.prefs.SessionPreferences
import com.aftersales.app.data.repository.GarageRepository
import com.aftersales.app.models.GarageData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface GarageUiState {
    data object Loading : GarageUiState
    data object Empty : GarageUiState
    data class Error(val error: ApiError) : GarageUiState
    data class Content(val garage: GarageData, val query: String) : GarageUiState
}

@HiltViewModel
class GarageViewModel @Inject constructor(
    private val repository: GarageRepository,
    private val sessionPreferences: SessionPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow<GarageUiState>(GarageUiState.Loading)
    val uiState: StateFlow<GarageUiState> = _uiState.asStateFlow()

    private var garage = GarageData()
    private var query = ""
    private var loadJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (!sessionPreferences.isLoggedIn) {
                _uiState.value = GarageUiState.Error(ApiError.UNAUTHORIZED)
                return@launch
            }
            _uiState.value = GarageUiState.Loading
            when (val result = repository.getGarage()) {
                is ApiResult.Success -> {
                    garage = result.data
                    publish()
                }
                is ApiResult.Error -> _uiState.value = GarageUiState.Error(result.error)
            }
        }
    }

    fun onSearchQueryChanged(value: String) {
        query = value
        if (_uiState.value is GarageUiState.Content) publish()
    }

    private fun publish() {
        _uiState.value = if (garage.isEmpty) {
            GarageUiState.Empty
        } else {
            GarageUiState.Content(garage.copy(vehicles = garage.vehicles.filter { it.matches(query) }), query)
        }
    }
}
