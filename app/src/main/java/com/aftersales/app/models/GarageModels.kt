package com.aftersales.app.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Serializable
data class Vehicle(
    @SerialName("id") val id: String? = null,
    @SerialName("vin") val vin: String? = null,
    @SerialName("make") val make: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("modelYear") val modelYear: String? = null,
    @SerialName("plateNumber") val plateNumber: String? = null,
    @SerialName("mileage") val mileage: String? = null,
    @SerialName("fuelType") val fuelType: String? = null,
    @SerialName("gearType") val gearType: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("imageUrl") val imageUrl: String? = null,
    @SerialName("registrationEndDate") val registrationEndDate: String? = null,
    @SerialName("insuranceEndDate") val insuranceEndDate: String? = null,
) {
    val key: String get() = id ?: vin.orEmpty()

    val displayName: String
        get() = listOfNotNull(make, description).filter { it.isNotBlank() }.joinToString(" ").ifBlank { vin.orEmpty() }

    val vehicleStatus: VehicleStatus get() = VehicleStatus.from(status)

    fun registrationValidity(today: LocalDate = LocalDate.now()) = DocumentValidity.of(parseDate(registrationEndDate), today)

    fun insuranceValidity(today: LocalDate = LocalDate.now()) = DocumentValidity.of(parseDate(insuranceEndDate), today)

    fun matches(query: String): Boolean {
        val text = listOfNotNull(vin, make, description, modelYear, plateNumber).joinToString(" ").lowercase()
        return query.trim().lowercase().split(" ").filter { it.isNotEmpty() }.all { it in text }
    }
}

enum class VehicleStatus {
    APPROVED, PENDING, REJECTED, UNKNOWN;

    companion object {
        fun from(raw: String?): VehicleStatus {
            val value = raw?.trim()?.lowercase().orEmpty()
            return when {
                value.startsWith("approv") -> APPROVED
                value.startsWith("pend") -> PENDING
                value.startsWith("reject") -> REJECTED
                else -> UNKNOWN
            }
        }
    }
}

sealed interface DocumentValidity {
    data object Unknown : DocumentValidity
    data class Valid(val daysLeft: Long) : DocumentValidity
    data class ExpiringSoon(val daysLeft: Long) : DocumentValidity
    data class Expired(val daysAgo: Long) : DocumentValidity

    companion object {
        const val EXPIRING_SOON_DAYS = 30L

        fun of(expiry: LocalDate?, today: LocalDate): DocumentValidity {
            if (expiry == null) return Unknown
            val daysLeft = ChronoUnit.DAYS.between(today, expiry)
            return when {
                daysLeft < 0 -> Expired(-daysLeft)
                daysLeft <= EXPIRING_SOON_DAYS -> ExpiringSoon(daysLeft)
                else -> Valid(daysLeft)
            }
        }
    }
}

@Serializable
data class ActiveWorkOrder(
    @SerialName("id") val id: String? = null,
    @SerialName("workOrderNo") val workOrderNo: String? = null,
    @SerialName("company") val company: String? = null,
    @SerialName("workshop") val workshop: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("openedAt") val openedAt: String? = null,
    @SerialName("vehicle") val vehicle: WorkOrderVehicle? = null,
    @SerialName("pricing") val pricing: WorkOrderPricing? = null,
)

@Serializable
data class WorkOrderVehicle(
    @SerialName("vin") val vin: String? = null,
    @SerialName("plateNumber") val plateNumber: String? = null,
    @SerialName("make") val make: String? = null,
    @SerialName("description") val description: String? = null,
)

@Serializable
data class WorkOrderPricing(
    @SerialName("currency") val currency: String? = null,
    @SerialName("total") val total: String? = null,
    @SerialName("linesCount") val linesCount: Int? = null,
)

@Serializable
data class TCard(
    @SerialName("id") val id: String? = null,
    @SerialName("itemName") val itemName: String? = null,
    @SerialName("branch") val branch: String? = null,
    @SerialName("serviceAdvisor") val serviceAdvisor: String? = null,
    @SerialName("quantity") val quantity: Int? = null,
    @SerialName("status") val status: String? = null,
)

data class GarageData(
    val vehicles: List<Vehicle> = emptyList(),
    val activeWorkOrders: List<ActiveWorkOrder> = emptyList(),
    val tCards: List<TCard> = emptyList(),
) {
    val isEmpty: Boolean get() = vehicles.isEmpty() && activeWorkOrders.isEmpty() && tCards.isEmpty()
}

private val DATE_FORMATS = listOf(DateTimeFormatter.ISO_LOCAL_DATE, DateTimeFormatter.ofPattern("yyyy/MM/dd"))

private fun parseDate(raw: String?): LocalDate? {
    val value = raw?.trim()?.take(10)?.takeIf { it.isNotEmpty() } ?: return null
    return DATE_FORMATS.firstNotNullOfOrNull { runCatching { LocalDate.parse(value, it) }.getOrNull() }
}
