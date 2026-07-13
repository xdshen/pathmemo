package com.pathmemo.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundHelpScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("后台持续运行设置") },
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
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "PathMemo 需要长期在后台采集位置，但国产定制系统通常会为了省电而清理后台应用。如果发现轨迹记录中断或通知消失，请按以下步骤设置。",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            HelpSection(title = "通用原则") {
                HelpBullet("允许自启动", "让应用在手机重启或被杀死后能够自动恢复服务。")
                HelpBullet("锁定后台任务", "在多任务界面把 PathMemo 锁定，避免一键清理或系统自动优化时被杀掉。")
                HelpBullet("关闭电池优化 / 允许后台活动", "让系统允许 PathMemo 在后台持续运行。")
            }

            Text(
                text = "不同系统版本菜单位置可能略有差异，以下路径仅供参考。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            HelpSection(title = "vivo（OriginOS / Funtouch OS）") {
                HelpNumbered(1, "锁定后台", "点击底部多任务键（或上滑悬停）打开近期任务，找到 PathMemo 卡片，向下拉一下，卡片上方出现“锁”图标即表示已锁定。")
                HelpNumbered(2, "允许自启动", "打开 i管家 → 应用管理 → 权限管理 → 自启动 → 开启 PathMemo。")
                HelpNumbered(3, "允许后台高耗电", "设置 → 电池 → 后台高耗电 → 开启 PathMemo 的“后台高耗电时允许运行”。")
                HelpNumbered(4, "关闭省电模式", "设置 → 电池，关闭省电模式。")
            }

            HelpSection(title = "华为 / 荣耀") {
                HelpNumbered(1, "锁定后台", "打开多任务界面，找到 PathMemo 卡片，向下拉一下（或点击卡片上的锁图标）进行锁定。")
                HelpNumbered(2, "启动管理", "手机管家 → 应用启动管理 → 找到 PathMemo → 关闭“自动管理”，手动开启允许自启动、允许后台活动、允许关联启动。")
                HelpNumbered(3, "电池优化", "设置 → 电池 → 电池优化 → 找到 PathMemo → 选择“不允许”。")
                HelpNumbered(4, "休眠保持网络", "设置 → 电池 → 更多电池设置 → 开启“休眠时始终保持网络连接”。")
            }

            HelpSection(title = "小米 / Redmi（MIUI / HyperOS）") {
                HelpNumbered(1, "锁定后台", "打开多任务界面，长按 PathMemo 卡片 → 选择锁定（或下拉卡片出现锁图标）。")
                HelpNumbered(2, "自启动", "手机管家 → 应用管理 → 权限 → 自启动 → 开启 PathMemo。")
                HelpNumbered(3, "省电策略", "设置 → 省电与电池 → 应用智能省电 → 找到 PathMemo → 选择“无限制”。")
                HelpNumbered(4, "后台弹出界面", "设置 → 应用设置 → 应用管理 → PathMemo → 权限管理 → 其它权限 → 开启“后台弹出界面”。")
            }

            HelpSection(title = "OPPO / 一加 / realme（ColorOS）") {
                HelpNumbered(1, "锁定后台", "打开多任务界面，找到 PathMemo 卡片，向下拉一下，出现锁图标即锁定成功。")
                HelpNumbered(2, "耗电管理", "设置 → 电池 → 应用耗电管理 → 找到 PathMemo → 开启允许完全后台行为、允许自启动。")
                HelpNumbered(3, "自启动管理", "手机管家 → 权限隐私 → 自启动管理 → 开启 PathMemo。")
            }

            HelpSection(title = "仍然被杀死怎么办？") {
                HelpBullet("关闭省电模式", "检查系统是否开启了“省电模式”“超级省电模式”或“睡眠模式”，关闭后再试。")
                HelpBullet("加入清理白名单", "部分系统会在夜间自动清理后台，可尝试把 PathMemo 加入系统管家的“清理加速保护名单”或“内存加速白名单”。")
                HelpBullet("第三方清理工具", "如果使用了 360、腾讯手机管家等清理工具，请把 PathMemo 加入其白名单。")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HelpSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun HelpBullet(
    title: String,
    content: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, bottom = 8.dp)
    ) {
        Text(
            text = "• $title",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp, top = 2.dp)
        )
    }
}

@Composable
private fun HelpNumbered(
    index: Int,
    title: String,
    content: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, bottom = 8.dp)
    ) {
        Text(
            text = "$index. $title",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 2.dp)
        )
    }
}
