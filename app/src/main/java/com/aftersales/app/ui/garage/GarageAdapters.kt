package com.aftersales.app.ui.garage

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.aftersales.app.R
import com.aftersales.app.databinding.ItemActiveWorkOrderBinding
import com.aftersales.app.databinding.ItemTCardBinding
import com.aftersales.app.databinding.ItemVehicleBinding
import com.aftersales.app.models.ActiveWorkOrder
import com.aftersales.app.models.DocumentValidity
import com.aftersales.app.models.TCard
import com.aftersales.app.models.Vehicle
import com.aftersales.app.models.VehicleStatus
import java.text.NumberFormat
import java.util.Currency

class VehicleAdapter : ListAdapter<Vehicle, VehicleAdapter.ViewHolder>(diffBy { it.key }) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemVehicleBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class ViewHolder(private val binding: ItemVehicleBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(vehicle: Vehicle) = with(binding) {
            val context = root.context
            imageVehicle.load(vehicle.imageUrl) {
                placeholder(R.drawable.ic_vehicle_placeholder)
                error(R.drawable.ic_vehicle_placeholder)
                fallback(R.drawable.ic_vehicle_placeholder)
            }
            textVehicleName.text = vehicle.displayName
            textVehicleMeta.text = listOfNotNull(vehicle.modelYear, vehicle.plateNumber).joinToString(" • ")
            textVin.text = context.getString(R.string.garage_vin_value, vehicle.vin.orEmpty())
            textMileage.text = vehicle.mileage?.let { context.getString(R.string.garage_mileage_km, it) }
            textMileage.isVisible = vehicle.mileage != null

            when (vehicle.vehicleStatus) {
                VehicleStatus.APPROVED -> textStatus.setPill(R.string.vehicle_status_approved, R.color.status_success, R.color.status_success_container)
                VehicleStatus.PENDING -> textStatus.setPill(R.string.vehicle_status_pending, R.color.status_warning, R.color.status_warning_container)
                VehicleStatus.REJECTED -> textStatus.setPill(R.string.vehicle_status_rejected, R.color.status_danger, R.color.status_danger_container)
                VehicleStatus.UNKNOWN -> textStatus.setPill(R.string.vehicle_status_unknown, R.color.status_neutral, R.color.status_neutral_container)
            }

            val attention = documentWarning(context, R.string.garage_document_registration, vehicle.registrationValidity())
                ?: documentWarning(context, R.string.garage_document_insurance, vehicle.insuranceValidity())
            textAttention.text = attention
            textAttention.isVisible = attention != null
            textAttention.setTone(R.color.status_danger, R.color.status_danger_container)
        }

        private fun documentWarning(context: Context, documentRes: Int, validity: DocumentValidity): String? {
            val document = context.getString(documentRes)
            val resources = context.resources
            return when (validity) {
                is DocumentValidity.Expired -> resources.getQuantityString(
                    R.plurals.garage_document_expired_days_ago, validity.daysAgo.toInt(), document, validity.daysAgo.toInt(),
                )
                is DocumentValidity.ExpiringSoon if validity.daysLeft == 0L ->
                    context.getString(R.string.garage_document_expires_today, document)
                is DocumentValidity.ExpiringSoon -> resources.getQuantityString(
                    R.plurals.garage_document_expires_in_days, validity.daysLeft.toInt(), document, validity.daysLeft.toInt(),
                )
                else -> null
            }
        }
    }
}

class WorkOrderAdapter : ListAdapter<ActiveWorkOrder, WorkOrderAdapter.ViewHolder>(diffBy { it.id }) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemActiveWorkOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class ViewHolder(private val binding: ItemActiveWorkOrderBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(order: ActiveWorkOrder) = with(binding) {
            val context = root.context
            textOrderNumber.text = context.getString(R.string.garage_work_order_number, order.workOrderNo ?: order.id.orEmpty())
            textOrderStatus.text = order.status
            textOrderStatus.isVisible = !order.status.isNullOrBlank()
            textOrderStatus.setTone(R.color.status_info, R.color.status_info_container)
            textWorkshop.text = listOfNotNull(order.workshop, order.company).joinToString(" · ")
            textOrderVehicle.text = listOfNotNull(order.vehicle?.make, order.vehicle?.description, order.vehicle?.plateNumber).joinToString(" • ")
            textOpenedAt.text = order.openedAt?.let { context.getString(R.string.garage_work_order_opened_at, it) }
            textOpenedAt.isVisible = order.openedAt != null

            val lines = order.pricing?.linesCount ?: 0
            textLineCount.text = context.resources.getQuantityString(R.plurals.garage_work_order_lines, lines, lines)
            textTotal.text = formatMoney(order.pricing?.total, order.pricing?.currency)
        }

        private fun formatMoney(amount: String?, currency: String?): String {
            val format = NumberFormat.getCurrencyInstance()
            runCatching { format.currency = Currency.getInstance(currency ?: "ILS") }
            return format.format(amount?.toBigDecimalOrNull() ?: 0)
        }
    }
}

class TCardAdapter : ListAdapter<TCard, TCardAdapter.ViewHolder>(diffBy { it.id }) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemTCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class ViewHolder(private val binding: ItemTCardBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(card: TCard) = with(binding) {
            val context = root.context
            textItemName.text = card.itemName
            textBranch.text = card.branch
            textAdvisor.text = context.getString(R.string.garage_t_card_advisor, card.serviceAdvisor.orEmpty())
            textQuantity.text = card.quantity?.let { context.getString(R.string.garage_t_card_quantity, it) }
            textQuantity.isVisible = card.quantity != null
            textTCardStatus.text = card.status
            textTCardStatus.isVisible = !card.status.isNullOrBlank()
            textTCardStatus.setTone(R.color.status_warning, R.color.status_warning_container)
        }
    }
}

private fun <T : Any> diffBy(key: (T) -> Any?) = object : DiffUtil.ItemCallback<T>() {
    override fun areItemsTheSame(oldItem: T, newItem: T) = key(oldItem) == key(newItem)
    override fun areContentsTheSame(oldItem: T, newItem: T) = oldItem == newItem
}

private fun TextView.setPill(textRes: Int, @ColorRes content: Int, @ColorRes container: Int) {
    setText(textRes)
    setTone(content, container)
}

private fun TextView.setTone(@ColorRes content: Int, @ColorRes container: Int) {
    setTextColor(ContextCompat.getColor(context, content))
    backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, container))
}
