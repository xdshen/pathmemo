# Create a desktop shortcut for PathMemo Web Viewer
$WshShell = New-Object -ComObject WScript.Shell
$DesktopPath = [Environment]::GetFolderPath('Desktop')
$BatchPath = Join-Path $PSScriptRoot 'start_web_viewer.bat'
$Shortcut = $WshShell.CreateShortcut((Join-Path $DesktopPath 'PathMemo Web Viewer.lnk'))
$Shortcut.TargetPath = $BatchPath
$Shortcut.WorkingDirectory = $PSScriptRoot
$Shortcut.IconLocation = 'shell32.dll,14'
$Shortcut.Description = '在电脑上查看 PathMemo 轨迹数据'
$Shortcut.Save()
Write-Host "快捷方式已创建到桌面：PathMemo Web Viewer.lnk" -ForegroundColor Green
