package com.frankgp.appinfo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.frankgp.appinfo.data.AppInfo
import com.frankgp.appinfo.data.AppRepository
import com.frankgp.appinfo.data.LanguageType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTypeFilter {
    ALL, USER, SYSTEM
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)

    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedFilter = MutableStateFlow<LanguageType?>(null)
    val selectedFilter: StateFlow<LanguageType?> = _selectedFilter

    private val _selectedAppTypeFilter = MutableStateFlow(AppTypeFilter.ALL)
    val selectedAppTypeFilter: StateFlow<AppTypeFilter> = _selectedAppTypeFilter

    private val _selectedApp = MutableStateFlow<AppInfo?>(null)
    val selectedApp: StateFlow<AppInfo?> = _selectedApp

    val filteredApps: StateFlow<List<AppInfo>> = combine(
        _allApps,
        _searchQuery,
        _selectedFilter,
        _selectedAppTypeFilter
    ) { apps, query, langFilter, appTypeFilter ->
        apps.filter { app ->
            val matchesQuery = query.isBlank() ||
                    app.name.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true)
            val matchesLang = langFilter == null || app.languageType == langFilter
            val matchesAppType = when (appTypeFilter) {
                AppTypeFilter.ALL -> true
                AppTypeFilter.USER -> !app.isSystemApp
                AppTypeFilter.SYSTEM -> app.isSystemApp
            }
            matchesQuery && matchesLang && matchesAppType
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch {
            _isLoading.value = true
            _allApps.value = repository.getInstalledApps()
            _isLoading.value = false
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: LanguageType?) {
        _selectedFilter.value = filter
    }

    fun setAppTypeFilter(filter: AppTypeFilter) {
        _selectedAppTypeFilter.value = filter
    }

    fun selectApp(app: AppInfo?) {
        _selectedApp.value = app
    }
}
