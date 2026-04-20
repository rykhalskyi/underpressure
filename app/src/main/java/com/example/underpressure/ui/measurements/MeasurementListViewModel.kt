package com.example.underpressure.ui.measurements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.underpressure.data.local.entities.MeasurementListEntity
import com.example.underpressure.data.local.entities.MeasurementListType
import com.example.underpressure.domain.repository.GenericMeasurementRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MeasurementListUiState(
    val lists: List<MeasurementListEntity> = emptyList(),
    val isLoading: Boolean = false
)

class MeasurementListViewModel(
    private val genericRepository: GenericMeasurementRepository
) : ViewModel() {

    val uiState: StateFlow<MeasurementListUiState> = genericRepository.getAllLists()
        .map { MeasurementListUiState(lists = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MeasurementListUiState(isLoading = true)
        )

    fun addList(name: String, type: MeasurementListType) {
        viewModelScope.launch {
            genericRepository.saveList(MeasurementListEntity(name = name, type = type))
        }
    }

    fun toggleListActive(list: MeasurementListEntity) {
        viewModelScope.launch {
            genericRepository.saveList(list.copy(active = !list.active))
        }
    }

    fun deleteList(list: MeasurementListEntity) {
        viewModelScope.launch {
            genericRepository.deleteList(list)
        }
    }
}
