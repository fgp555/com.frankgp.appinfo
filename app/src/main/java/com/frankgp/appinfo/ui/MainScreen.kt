package com.frankgp.appinfo.ui

import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.frankgp.appinfo.data.AppInfo
import com.frankgp.appinfo.data.LanguageType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val apps by viewModel.filteredApps.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val selectedAppTypeFilter by viewModel.selectedAppTypeFilter.collectAsState()
    val selectedApp by viewModel.selectedApp.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FGP App Info") },
                actions = {
                    IconButton(onClick = { viewModel.loadApps() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Recargar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar por nombre o package...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Filter Chips - App Type (All, User, System)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedAppTypeFilter == AppTypeFilter.ALL,
                    onClick = { viewModel.setAppTypeFilter(AppTypeFilter.ALL) },
                    label = { Text("Todas las apps") }
                )
                FilterChip(
                    selected = selectedAppTypeFilter == AppTypeFilter.USER,
                    onClick = { viewModel.setAppTypeFilter(AppTypeFilter.USER) },
                    label = { Text("Usuario") }
                )
                FilterChip(
                    selected = selectedAppTypeFilter == AppTypeFilter.SYSTEM,
                    onClick = { viewModel.setAppTypeFilter(AppTypeFilter.SYSTEM) },
                    label = { Text("Sistema") }
                )
            }

            // Filter Chips - Language Type (All, Java, Native, Hybrid)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { viewModel.setFilter(null) },
                    label = { Text("Todos los lenguajes") }
                )
                FilterChip(
                    selected = selectedFilter == LanguageType.JAVA_KOTLIN,
                    onClick = { viewModel.setFilter(LanguageType.JAVA_KOTLIN) },
                    label = { Text("Java") }
                )
                FilterChip(
                    selected = selectedFilter == LanguageType.NATIVE,
                    onClick = { viewModel.setFilter(LanguageType.NATIVE) },
                    label = { Text("Nativa") }
                )
                FilterChip(
                    selected = selectedFilter == LanguageType.HYBRID,
                    onClick = { viewModel.setFilter(LanguageType.HYBRID) },
                    label = { Text("Híbrida") }
                )
            }

            // App List or Loading
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (apps.isEmpty()) {
                    Text(
                        text = "No se encontraron aplicaciones",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(apps, key = { it.packageName }) { app ->
                            AppItemCard(app = app, onClick = { viewModel.selectApp(app) })
                        }
                    }
                }
            }
        }

        // Detail Bottom Sheet
        if (selectedApp != null) {
            AppDetailSheet(
                app = selectedApp!!,
                onDismiss = { viewModel.selectApp(null) }
            )
        }
    }
}

@Composable
fun AppItemCard(app: AppInfo, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val updateDateStr = remember(app.lastUpdateTime) {
        try { dateFormat.format(Date(app.lastUpdateTime)) } catch (_: Exception) { "" }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (app.icon != null) {
                AndroidView(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    factory = { ctx ->
                        ImageView(ctx).apply {
                            scaleType = ImageView.ScaleType.FIT_CENTER
                        }
                    },
                    update = { imageView ->
                        imageView.setImageDrawable(app.icon)
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (app.isSystemApp) "Sistema" else "Usuario",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (app.isSystemApp) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "${app.packageName} • v${app.versionName} • $updateDateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val badgeColor = when (app.languageType) {
                        LanguageType.JAVA_KOTLIN -> Color(0xFF1976D2)
                        LanguageType.NATIVE -> Color(0xFFD32F2F)
                        LanguageType.HYBRID -> Color(0xFF7B1FA2)
                    }
                    Surface(
                        color = badgeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = app.languageType.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (app.framework != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = app.framework,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (app.nativeLibraries.isNotEmpty()) {
                        Text(
                            text = "• ${app.nativeLibraries.size} .so",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
