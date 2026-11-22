package com.aftersales.app.data.api

import com.aftersales.app.models.ActiveWorkOrder
import com.aftersales.app.models.ApiResponse
import com.aftersales.app.models.TCard
import com.aftersales.app.models.Vehicle
import retrofit2.http.GET

interface GarageApi {

    @GET("customer/vehicles")
    suspend fun getVehicles(): ApiResponse<List<Vehicle>>

    @GET("customer/work-orders/active")
    suspend fun getActiveWorkOrders(): ApiResponse<List<ActiveWorkOrder>>

    @GET("customer/t-cards")
    suspend fun getTCards(): ApiResponse<List<TCard>>
}
