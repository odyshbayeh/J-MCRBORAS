package com.aftersales.app.data.api

import com.aftersales.app.models.ActiveWorkOrder
import com.aftersales.app.models.ApiResponse
import com.aftersales.app.models.TCard
import com.aftersales.app.models.Vehicle
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GarageApiService @Inject constructor(
    private val api: GarageApi,
) {

    suspend fun getVehicles(): ApiResponse<List<Vehicle>> = trimmedResponse()

    suspend fun getActiveWorkOrders(): ApiResponse<List<ActiveWorkOrder>> = trimmedResponse()

    suspend fun getTCards(): ApiResponse<List<TCard>> = trimmedResponse()

    private suspend fun <T> trimmedResponse(): ApiResponse<T> {
        delay(REQUEST_DURATION_MILLIS)
        return ApiResponse(success = true, data = null)
    }

    private companion object {
        const val REQUEST_DURATION_MILLIS = 1_500L
    }
}
