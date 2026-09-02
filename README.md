# PathMemo

一款 Android 个人轨迹记录与回溯 APP，支持静默后台记录、高德地图可视化、时间轴回放，并提供 Web Viewer 可在电脑浏览器上查看与管理轨迹数据。

## 功能特性

### APP 端功能

- **静默记录**：前台 Service + 高德定位 SDK，后台持续采集位置。
- **自动记录**：开启后无需手动开始/停止，应用会自动持续记录轨迹。
- **开机自动记录**：支持设备重启后自动恢复轨迹记录（需在设置中开启并授权位置权限）。
- **按日期查看**：轨迹以自然日为单位聚合，跨天记录会自动归属到对应日期；也支持选择连续多日查看完整轨迹。
- **地图可视化**：实时显示当前位置与已记录轨迹；历史轨迹详情页绘制完整路径。
- **时间轴回溯**：历史轨迹按日期倒序展示，详情页支持滑块定位与多倍速回放。
- **基站信息记录**：记录位置的同时保存当前 4G/5G 服务小区信息（网络类型、运营商、MCC/MNC、TAC、PCI、CI、ARFCN、频段、SS RSRP/RSRQ/SINR），首页实时显示当前小区与信号强度，数据仅保存在本地。
- **本地存储**：使用 Room 数据库保存轨迹与位置点，数据不上传。
- **隐私合规**：首次运行申请位置权限，提供隐私说明与电池优化白名单引导。

### Web Viewer（电脑端查看）

- **浏览器查看轨迹**：通过电脑浏览器访问本地网页，实时查看手机中的轨迹数据。
- **地图散点展示**：按时间段着色展示所有轨迹点。
- **日历热力图**：以日历形式直观展示各日期的轨迹密度。
- **日期范围筛选**：支持选择日期范围，并提供「上一周 / 下一周 / 上一年 / 下一年」快捷跳转。
- **轨迹列表**：按日期列出轨迹，点击即可查看详情。
- **区域查询**：在地图上用鼠标框选一个矩形区域，即可查询哪些日期在该区域内有轨迹点，并列出各日期的起止时间与点数。
- **点位详情**：点击地图上的点位即可查看详细信息。

## 效果预览

| 首页 / 正在记录 | 轨迹回顾 |
|---|---|
| ![正在记录](screenshots/recording_home.jpg) | ![轨迹回顾](screenshots/track_review.jpg) |

| 区间轨迹预览 · 整体路径 | 区间轨迹预览 · 点位详情 |
|---|---|
| ![区间轨迹预览整体](screenshots/range_preview_overview.jpg) | ![区间轨迹预览详情](screenshots/range_preview_detail.jpg) |

## 技术栈

- Kotlin 1.9.24
- Jetpack Compose
- MVVM + Repository
- Room + KSP
- Koin 依赖注入
- 高德地图 AMap（定位 + 地图）
- Accompanist Permissions

## 项目结构

```
app/src/main/java/com/pathmemo/
├── PathMemoApp.kt              # Application + Koin 初始化
├── data/
│   ├── db/                     # Room DAO 与 Database
│   ├── model/                  # Track / LocationPoint / AppSettings 实体
│   ├── repository/             # TrackRepository
│   └── store/                  # DataStore 设置持久化
├── location/
│   ├── LocationRecorder.kt     # AMap 定位封装与位置过滤
│   └── CellInfoProvider.kt     # 4G/5G 服务小区信息采集
├── service/
│   └── LocationRecordService.kt# 前台轨迹记录服务
├── ui/                         # Compose 页面与地图组件
│   ├── map/AmapMap.kt / TrackMapView.kt
│   ├── home/HomeScreen.kt
│   ├── history/HistoryScreen.kt
│   ├── overview/OverviewScreen.kt
│   ├── preview/DayPreviewScreen.kt
│   ├── range/RangePreviewScreen.kt
│   ├── detail/TrackDetailScreen.kt
│   ├── settings/SettingsScreen.kt
│   └── navigation/PathMemoNavHost.kt
├── viewmodel/                  # 各页面对应 ViewModel
└── di/AppModule.kt             # Koin 模块
```

## 运行前配置

1. 前往 [高德开放平台](https://lbs.amap.com) 注册并创建应用，获取 **Key**。
2. 打开项目根目录 `local.properties`，将 `AMAP_API_KEY=YOUR_AMAP_API_KEY` 替换为实际 Key。
3. 使用 Android Studio 或 `./gradlew assembleDebug` 构建安装包。

## 权限说明

APP 需要以下权限：

- `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`：基础定位
- `ACCESS_BACKGROUND_LOCATION`：后台持续记录
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_LOCATION`：前台服务
- `POST_NOTIFICATIONS`：记录通知（Android 13+）
- `RECEIVE_BOOT_COMPLETED`：开机自动恢复记录
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`：引导加入电池优化白名单

## 后台持续运行设置指南

PathMemo 需要长期在后台采集位置，但国产定制系统（小米、华为、OPPO、vivo 等）通常会为了省电而清理后台应用。如果发现轨迹记录中断、通知消失，建议按以下步骤设置：

### 通用原则

1. **允许自启动**：让应用在手机重启或被杀死后能够自动恢复服务。
2. **锁定后台任务**：在多任务界面把 PathMemo **锁定**，避免一键清理或系统自动优化时被杀掉。
3. **关闭电池优化 / 允许后台活动**：让系统允许 PathMemo 在后台持续运行。

### 各品牌设置参考

> 不同系统版本菜单位置可能略有差异，以下路径仅供参考。

#### vivo（OriginOS / Funtouch OS）

1. **锁定后台**：点击屏幕底部多任务键（或上滑悬停）打开近期任务，找到 PathMemo 卡片，**向下拉一下**，卡片上方出现“锁”图标即表示已锁定。
2. **允许自启动**：打开 **i管家** → 应用管理 → 权限管理 → 自启动 → 开启 PathMemo。
3. **允许后台高耗电**：设置 → 电池 → 后台高耗电 → 开启 PathMemo 的“后台高耗电时允许运行”。
4. **关闭省电模式**：设置 → 电池，关闭省电模式。

#### 华为 / 荣耀

1. **锁定后台**：打开多任务界面，找到 PathMemo 卡片，**向下拉一下**（或点击卡片上的锁图标）进行锁定。
2. **启动管理**：手机管家 → 应用启动管理 → 找到 PathMemo → 关闭“自动管理”，手动开启 **允许自启动**、**允许后台活动**、**允许关联启动**。
3. **电池优化**：设置 → 电池 → 电池优化 → 找到 PathMemo → 选择 **不允许**。
4. **休眠保持网络**：设置 → 电池 → 更多电池设置 → 开启“休眠时始终保持网络连接”。

#### 小米 / Redmi（MIUI / HyperOS）

1. **锁定后台**：打开多任务界面，长按 PathMemo 卡片 → 选择 **锁定**（或下拉卡片出现锁图标）。
2. **自启动**：手机管家 → 应用管理 → 权限 → 自启动 → 开启 PathMemo。
3. **省电策略**：设置 → 省电与电池 → 应用智能省电 → 找到 PathMemo → 选择 **无限制**。
4. **后台弹出界面**：设置 → 应用设置 → 应用管理 → PathMemo → 权限管理 → 其它权限 → 开启“后台弹出界面”。

#### OPPO / 一加 / realme（ColorOS）

1. **锁定后台**：打开多任务界面，找到 PathMemo 卡片，**向下拉一下**，出现锁图标即锁定成功。
2. **耗电管理**：设置 → 电池 → 应用耗电管理 → 找到 PathMemo → 开启 **允许完全后台行为**、**允许自启动**。
3. **自启动管理**：手机管家 → 权限隐私 → 自启动管理 → 开启 PathMemo。

### 仍然被杀死怎么办？

- 检查系统是否开启了“省电模式”“超级省电模式”或“睡眠模式”，关闭后再试。
- 部分系统会在夜间自动清理后台，可尝试把 PathMemo 加入系统管家的“清理加速保护名单”或“内存加速白名单”。
- 如果使用了第三方清理工具（如 360、腾讯手机管家），请把 PathMemo 加入其白名单。

## 电脑端查看工具

项目提供 `tools/web_viewer/`，可在电脑上通过浏览器查看手机里的轨迹数据。

1. 手机连接电脑并开启 USB 调试。
2. 网页地图会自动读取项目 `local.properties` 中的 `AMAP_API_KEY`。如果该 Key 仅绑定 Android 平台，需要再创建一个 Web JS API Key 并填入 `local.properties`；如开启了 Key 白名单，需将 `127.0.0.1` / `localhost` 加入 HTTP Referer 白名单。
3. Windows 直接双击项目根目录的 `start_web_viewer.bat`，或右键它发送到桌面快捷方式；也可以运行一次 `create_web_viewer_shortcut.ps1` 在桌面生成图标（如系统限制脚本执行，可在 PowerShell 中先执行 `Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process`）。
4. 命令行方式：
   ```bash
   cd tools/web_viewer
   python web_viewer.py
   ```
5. 脚本会直接在手机上通过 `adb shell sqlite3` 查询数据，按需返回结果，**不再一次性把整个数据库拉到电脑**，首次打开更快；随后自动打开本地网页 `http://127.0.0.1:8765/`。

Web Viewer 的详细功能介绍请参见上方「功能特性 → Web Viewer（电脑端查看）」。

## 注意事项

- 部分国产系统（小米/华为/OPPO/vivo）会限制后台定位，建议在“设置 → 电池优化白名单”中开启忽略电池优化。
- 轨迹数据仅保存在本地 SQLite，卸载 APP 会丢失，后续可扩展导出 GPX / JSON 功能。
- Web Viewer 通过 ADB 读取应用私有数据库，仅适用于调试版（debug）安装包。
- Web Viewer 依赖设备上的 `sqlite3` 命令（Android 10+ 通常已内置）直接在手机上查询数据。
