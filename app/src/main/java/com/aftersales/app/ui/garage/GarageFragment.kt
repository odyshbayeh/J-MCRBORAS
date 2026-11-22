package com.aftersales.app.ui.garage

import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aftersales.app.R
import com.aftersales.app.data.api.ApiError
import com.aftersales.app.databinding.FragmentGarageBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GarageFragment : Fragment(R.layout.fragment_garage) {

    private val viewModel: GarageViewModel by viewModels()

    private var _binding: FragmentGarageBinding? = null
    private val binding get() = checkNotNull(_binding)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentGarageBinding.bind(view)

        with(binding) {
            recyclerWorkOrders.adapter = WorkOrderAdapter()
            recyclerTCards.adapter = TCardAdapter()
            recyclerVehicles.adapter = VehicleAdapter()

            headerWorkOrders.textSectionTitle.setText(R.string.garage_section_active_orders)
            headerTCards.textSectionTitle.setText(R.string.garage_section_t_cards)
            headerVehicles.textSectionTitle.setText(R.string.garage_section_vehicles)

            toolbar.setOnMenuItemClickListener { item ->
                if (item.itemId == R.id.action_refresh) viewModel.refresh()
                item.itemId == R.id.action_refresh
            }
            swipeRefresh.setOnRefreshListener {
                swipeRefresh.isRefreshing = false
                viewModel.refresh()
            }
            search.inputSearch.doAfterTextChanged { viewModel.onSearchQueryChanged(it?.toString().orEmpty()) }

            stateEmpty.imageStateIcon.setImageResource(R.drawable.ic_garage_empty)
            stateEmpty.textStateTitle.setText(R.string.garage_empty_title)
            stateEmpty.textStateMessage.setText(R.string.garage_empty_message)
            stateEmpty.buttonStateAction.setText(R.string.action_refresh)
            stateEmpty.buttonStateAction.setOnClickListener { viewModel.refresh() }

            stateError.imageStateIcon.setImageResource(R.drawable.ic_cloud_off)
            stateError.textStateTitle.setText(R.string.garage_error_title)
            stateError.buttonStateAction.setText(R.string.action_retry)
            stateError.buttonStateAction.setOnClickListener { viewModel.refresh() }

            ViewCompat.setOnApplyWindowInsetsListener(contentScroll) { scroll, insets ->
                scroll.updatePadding(bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom)
                insets
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun render(state: GarageUiState) = with(binding) {
        val isLoading = state is GarageUiState.Loading
        stateLoading.root.isVisible = isLoading
        if (isLoading) stateLoading.root.startShimmer() else stateLoading.root.stopShimmer()

        stateEmpty.root.isVisible = state is GarageUiState.Empty
        stateError.root.isVisible = state is GarageUiState.Error
        swipeRefresh.isVisible = state is GarageUiState.Content
        search.root.isVisible = state is GarageUiState.Content

        if (state is GarageUiState.Error) stateError.textStateMessage.setText(state.error.messageRes)

        if (state is GarageUiState.Content) {
            val garage = state.garage
            (recyclerWorkOrders.adapter as WorkOrderAdapter).submitList(garage.activeWorkOrders)
            (recyclerTCards.adapter as TCardAdapter).submitList(garage.tCards)
            (recyclerVehicles.adapter as VehicleAdapter).submitList(garage.vehicles)

            sectionWorkOrders.isVisible = garage.activeWorkOrders.isNotEmpty()
            sectionTCards.isVisible = garage.tCards.isNotEmpty()
            headerWorkOrders.textSectionCount.text = garage.activeWorkOrders.size.toString()
            headerTCards.textSectionCount.text = garage.tCards.size.toString()
            headerVehicles.textSectionCount.text = garage.vehicles.size.toString()
            textNoSearchResults.isVisible = garage.vehicles.isEmpty() && state.query.isNotBlank()
        }
    }

    private val ApiError.messageRes: Int
        get() = when (this) {
            ApiError.NO_CONNECTION -> R.string.error_no_connection
            ApiError.TIMEOUT -> R.string.error_timeout
            ApiError.UNAUTHORIZED -> R.string.error_session_expired
            ApiError.INVALID_RESPONSE -> R.string.error_invalid_response
            ApiError.SERVER_ERROR, ApiError.REQUEST_FAILED -> R.string.error_server
            ApiError.UNKNOWN -> R.string.error_unknown
        }
}
