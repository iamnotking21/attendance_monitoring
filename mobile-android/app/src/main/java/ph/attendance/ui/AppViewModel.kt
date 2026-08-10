package ph.attendance.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ph.attendance.AppContainer
import ph.attendance.AttendanceApplication
import ph.attendance.data.DemoData
import ph.attendance.domain.Section

/**
 * One view model for the whole app.
 *
 * The screens share a single selected section, a single toast channel, and one repository;
 * splitting that across six view models would mean six copies of the same coordination and a
 * selection that silently disagrees with itself between tabs.
 */
class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val container: AppContainer =
        (application as AttendanceApplication).container

    val repository = container.repository
    val settings = container.settings
    val syncEngine = container.syncEngine
    val api = container.api

    val sections: StateFlow<List<Section>> = repository.observeSections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedSectionId = MutableStateFlow<String?>(null)
    val selectedSectionId: StateFlow<String?> = _selectedSectionId.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        viewModelScope.launch {
            // Demo data first, so a first run is not six empty screens, then settle any window
            // that closed while the app was shut. The original app did the same on launch.
            runCatching {
                DemoData.seedIfEmpty(repository)
                repository.openDay()
            }.onFailure { error -> _message.value = error.message }
        }
    }

    /** Keeps a section chosen once any exist; screens built around "the current section" need one. */
    fun resolveSelected(available: List<Section>): Section? {
        val chosen = available.firstOrNull { it.id == _selectedSectionId.value }
        return chosen ?: available.firstOrNull()
    }

    fun selectSection(id: String) {
        _selectedSectionId.value = id
    }

    fun say(text: String) {
        _message.value = text
    }

    fun messageShown() {
        _message.value = null
    }

    fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                return AppViewModel(application) as T
            }
        }
    }
}

@Composable
fun appViewModel(): AppViewModel = viewModel(factory = AppViewModel.Factory)
