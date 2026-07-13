package com.pathmemo.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pathmemo.data.model.AppSettings
import com.pathmemo.data.model.LocationInterval
import com.pathmemo.data.model.MapType
import com.pathmemo.data.model.MinAccuracy
import com.pathmemo.data.model.MinDistance
import com.pathmemo.viewmodel.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    var showClearDialog by remember { mutableStateOf(false) }
    var dialogState by remember { mutableStateOf<SettingDialog?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(bottom = bottomPadding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSection(title = stringResource(com.pathmemo.R.string.settings_recording)) {
                SettingsItem(
                    title = stringResource(com.pathmemo.R.string.settings_location_interval),
                    summary = stringResource(settings.locationInterval.labelRes),
                    onClick = { dialogState = SettingDialog.Interval }
                )
                SettingsItem(
                    title = stringResource(com.pathmemo.R.string.settings_min_distance),
                    summary = stringResource(settings.minDistance.labelRes),
                    onClick = { dialogState = SettingDialog.MinDistance }
                )
                SettingsItem(
                    title = stringResource(com.pathmemo.R.string.settings_min_accuracy),
                    summary = stringResource(settings.minAccuracy.labelRes),
                    onClick = { dialogState = SettingDialog.MinAccuracy }
                )
                SettingsSwitchItem(
                    title = stringResource(com.pathmemo.R.string.settings_auto_record),
                    summary = stringResource(com.pathmemo.R.string.settings_auto_record_summary),
                    checked = settings.autoRecordEnabled,
                    onCheckedChange = { viewModel.setAutoRecordEnabled(it) }
                )
                SettingsSwitchItem(
                    title = stringResource(com.pathmemo.R.string.settings_auto_start_on_boot),
                    summary = stringResource(com.pathmemo.R.string.settings_auto_start_on_boot_summary),
                    checked = settings.autoStartOnBoot,
                    onCheckedChange = { viewModel.setAutoStartOnBoot(it) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsSection(title = stringResource(com.pathmemo.R.string.settings_map)) {
                SettingsItem(
                    title = stringResource(com.pathmemo.R.string.settings_map_type),
                    summary = stringResource(settings.mapType.labelRes),
                    onClick = { dialogState = SettingDialog.MapType }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsSection(title = "后台记录") {
                SettingsItem(
                    title = "电池优化白名单",
                    summary = "允许 PathMemo 在后台持续记录轨迹",
                    onClick = { viewModel.openBatteryOptimizationSettings() }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsSection(title = "数据管理") {
                SettingsItem(
                    title = "清除所有轨迹",
                    summary = "删除本地保存的全部轨迹数据，不可恢复",
                    onClick = { showClearDialog = true }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsSection(title = "关于") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "PathMemo",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "个人轨迹记录与回溯工具。所有位置数据仅保存在本地设备，不上传服务器。",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }

    dialogState?.let { dialog ->
        when (dialog) {
            SettingDialog.Interval -> SingleChoiceDialog(
                title = stringResource(com.pathmemo.R.string.settings_location_interval),
                options = LocationInterval.entries.map { stringResource(it.labelRes) },
                selectedIndex = LocationInterval.entries.indexOf(settings.locationInterval),
                onSelect = { index ->
                    viewModel.setLocationInterval(LocationInterval.entries[index])
                    dialogState = null
                },
                onDismiss = { dialogState = null }
            )
            SettingDialog.MinDistance -> SingleChoiceDialog(
                title = stringResource(com.pathmemo.R.string.settings_min_distance),
                options = MinDistance.entries.map { stringResource(it.labelRes) },
                selectedIndex = MinDistance.entries.indexOf(settings.minDistance),
                onSelect = { index ->
                    viewModel.setMinDistance(MinDistance.entries[index])
                    dialogState = null
                },
                onDismiss = { dialogState = null }
            )
            SettingDialog.MinAccuracy -> SingleChoiceDialog(
                title = stringResource(com.pathmemo.R.string.settings_min_accuracy),
                options = MinAccuracy.entries.map { stringResource(it.labelRes) },
                selectedIndex = MinAccuracy.entries.indexOf(settings.minAccuracy),
                onSelect = { index ->
                    viewModel.setMinAccuracy(MinAccuracy.entries[index])
                    dialogState = null
                },
                onDismiss = { dialogState = null }
            )
            SettingDialog.MapType -> SingleChoiceDialog(
                title = stringResource(com.pathmemo.R.string.settings_map_type),
                options = MapType.entries.map { stringResource(it.labelRes) },
                selectedIndex = MapType.entries.indexOf(settings.mapType),
                onSelect = { index ->
                    viewModel.setMapType(MapType.entries[index])
                    dialogState = null
                },
                onDismiss = { dialogState = null }
            )
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清除所有数据") },
            text = { Text("确定要删除所有轨迹记录吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllData { showClearDialog = false }
                }) { Text("清除") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            }
        )
    }
}

private enum class SettingDialog {
    Interval, MinDistance, MinAccuracy, MapType
}

@Composable
private fun SingleChoiceDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEachIndexed { index, label ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = index == selectedIndex,
                                onClick = { onSelect(index) }
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = index == selectedIndex,
                            onClick = { onSelect(index) }
                        )
                        Text(
                            text = label,
                            modifier = Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
        )
        content()
    }
}

@Composable
private fun SettingsItem(
    title: String,
    summary: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
