package com.aftersales.app.data.repository

import com.aftersales.app.data.api.ApiResult
import com.aftersales.app.data.api.GarageApiService
import com.aftersales.app.data.api.apiCall
import com.aftersales.app.models.GarageData
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GarageRepository @Inject constructor(
    private val apiService: GarageApiService,
) {

    suspend fun getGarage(): ApiResult<GarageData> = coroutineScope {
        val vehicles = async { apiCall { apiService.getVehicles() } }
        val workOrders = async { apiCall { apiService.getActiveWorkOrders() } }
        val tCards = async { apiCall { apiService.getTCards() } }

        when (val vehiclesResult = vehicles.await()) {
            is ApiResult.Error -> vehiclesResult
            is ApiResult.Success -> ApiResult.Success(
                GarageData(
                    vehicles = vehiclesResult.data.orEmpty(),
                    activeWorkOrders = (workOrders.await() as? ApiResult.Success)?.data.orEmpty(),
                    tCards = (tCards.await() as? ApiResult.Success)?.data.orEmpty(),
                ),
            )
        }
    }
}
