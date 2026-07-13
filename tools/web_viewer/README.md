# PathMemo Web Viewer

在电脑上查看手机里的 PathMemo 轨迹数据的小型网页工具。

## 前置条件

1. 手机通过 USB 连接到电脑，并已开启 **USB 调试**。
2. 电脑上安装有 Android SDK 的 `adb`，并且 `adb` 在系统 PATH 中。
3. 网页地图复用项目 `local.properties` 里的 `AMAP_API_KEY`。如果该 Key 只绑定了 Android 平台，需要再创建一个 Web JS API Key 并填入 `local.properties`；如开启了 Key 白名单，需要把 `127.0.0.1` / `localhost` 加入 HTTP Referer 白名单。

## 启动方式

### Windows 快速启动

- 方式一：项目根目录双击 `start_web_viewer.bat`。
- 方式二：右键项目根目录的 `start_web_viewer.bat` → 发送到 → 桌面快捷方式；以后双击桌面图标即可。
- 方式三：运行一次 PowerShell 脚本创建桌面快捷方式（如果系统限制脚本执行，可在 PowerShell 中先执行 `Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process`，再运行脚本）：
  ```powershell
  .\create_web_viewer_shortcut.ps1
  ```

### 命令行

```bash
cd tools/web_viewer
python web_viewer.py
```

也可以直接指定本地数据库文件（无需连接手机）：

```bash
python web_viewer.py path/to/pathmemo_database
```

脚本会自动：
1. 检查 ADB 设备连接。
2. 从手机 `/data/data/com.pathmemo/databases/pathmemo_database` 拉取 SQLite 数据库。
3. 读取 `tracks` 和 `location_points` 表。
4. 启动本地 HTTP 服务（默认 `http://127.0.0.1:8765`）。
5. 自动打开浏览器。

## 功能

- 地图展示：使用高德地图显示所选日期范围内的所有轨迹点。
- 颜色区分时间：紫色=凌晨、绿色=上午、橙色=下午、红色=晚上。
- 侧边栏：显示轨迹统计、日期范围选择、按月日历热力图、日期列表。
- 点击日期：快速查看某一天。
- 选择连续日期范围：跨天记录会显示为连续完整轨迹。
- 点击地图点：弹出完整时间、经纬度、海拔、精度信息。
- 数据按 `location_points.timestamp` 过滤，不再依赖 `tracks.startTime`，避免跨天轨迹归属错误。

## 文件说明

- `web_viewer.py`：ADB 拉取 + 数据导出 + 本地 HTTP 服务。
- `index.html`：前端网页应用。
- `start_web_viewer.bat`：Windows 一键启动脚本。
