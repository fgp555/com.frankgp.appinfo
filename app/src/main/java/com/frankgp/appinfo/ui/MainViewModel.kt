package com.frankgp.appinfo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.frankgp.appinfo.data.AppDetailsData
import com.frankgp.appinfo.data.AppInfo
import com.frankgp.appinfo.data.AppRepository
import com.frankgp.appinfo.data.DeviceInfo
import com.frankgp.appinfo.data.DeviceRepository
import com.frankgp.appinfo.data.LanguageType
import com.frankgp.appinfo.data.PerformanceData
import com.frankgp.appinfo.data.PerformanceRepository
import com.frankgp.appinfo.data.SpeedTestRepository
import com.frankgp.appinfo.data.SpeedTestResult
import com.frankgp.appinfo.service.PerformanceService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class MainTab {
    DEVICE, APPS, PERFORMANCE
}

enum class AppTypeFilter {
    ALL, USER, SYSTEM
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val appRepository = AppRepository(application)
    private val deviceRepository = DeviceRepository(application)
    private val performanceRepository = PerformanceRepository(application)
    private val speedTestRepository = SpeedTestRepository()

    private val _currentTab = MutableStateFlow(MainTab.DEVICE)
    val currentTab: StateFlow<MainTab> = _currentTab

    private val _deviceInfo = MutableStateFlow<DeviceInfo?>(null)
    val deviceInfo: StateFlow<DeviceInfo?> = _deviceInfo

    private val _realtimePerformanceData = MutableStateFlow(PerformanceData())
    val realtimePerformanceData: StateFlow<PerformanceData> = _realtimePerformanceData

    private val _speedTestResult = MutableStateFlow(SpeedTestResult())
    val speedTestResult: StateFlow<SpeedTestResult> = _speedTestResult

    private var speedTestJob: Job? = null

    val isPerformanceServiceRunning: StateFlow<Boolean> = PerformanceService.isServiceRunning

    private var performanceJob: Job? = null

    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedFilter = MutableStateFlow<LanguageType?>(null)
    val selectedFilter: StateFlow<LanguageType?> = _selectedFilter

    private val _selectedAppTypeFilter = MutableStateFlow(AppTypeFilter.USER)
    val selectedAppTypeFilter: StateFlow<AppTypeFilter> = _selectedAppTypeFilter

    private val _selectedApp = MutableStateFlow<AppInfo?>(null)
    val selectedApp: StateFlow<AppInfo?> = _selectedApp

    private val _detailsLoading = MutableStateFlow(false)
    val detailsLoading: StateFlow<Boolean> = _detailsLoading

    private val detailsCache = mutableMapOf<String, AppDetailsData>()

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
            val matchesLang = langFilter == null || (app.details != null && app.languageType == langFilter)
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
        loadDeviceInfo()
        loadApps()
    }

    fun setTab(tab: MainTab) {
        _currentTab.value = tab
        if (tab == MainTab.PERFORMANCE) {
            startPerformanceMonitoring()
        } else {
            stopPerformanceMonitoring()
        }
    }

    fun runSpeedTest() {
        speedTestJob?.cancel()
        speedTestJob = viewModelScope.launch {
            speedTestRepository.runCloudflareSpeedTest().collect { result ->
                _speedTestResult.value = result
            }
        }
    }

    private fun startPerformanceMonitoring() {
        if (performanceJob?.isActive == true) return
        performanceJob = viewModelScope.launch {
            while (isActive) {
                _realtimePerformanceData.value = performanceRepository.samplePerformance()
                delay(1000)
            }
        }
    }

    private fun stopPerformanceMonitoring() {
        performanceJob?.cancel()
        performanceJob = null
    }

    fun loadDeviceInfo() {
        viewModelScope.launch {
            _deviceInfo.value = deviceRepository.getDeviceInfo()
        }
    }

    fun loadApps() {
        viewModelScope.launch {
            _isLoading.value = true
            _allApps.value = appRepository.getInstalledApps()
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
        if (app == null) {
            _selectedApp.value = null
            _detailsLoading.value = false
            return
        }

        val cachedDetails = detailsCache[app.packageName]
        if (cachedDetails != null) {
            _selectedApp.value = app.copy(details = cachedDetails)
            _detailsLoading.value = false
        } else {
            _selectedApp.value = app
            _detailsLoading.value = true
            viewModelScope.launch {
                val detailsData = appRepository.getAppDetails(app.packageName, app.apkPath)
                detailsCache[app.packageName] = detailsData
                if (_selectedApp.value?.packageName == app.packageName) {
                    _selectedApp.value = app.copy(details = detailsData)
                }
                _detailsLoading.value = false
            }
        }
    }
}
