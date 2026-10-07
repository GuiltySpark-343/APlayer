param(
  [Parameter(Mandatory = $true)][string]$Name,
  [string]$Uri = "aplayer://annual_report"
)
$adb = "D:/Application2/Android/Sdk/platform-tools/adb.exe"
$dir = Join-Path $PSScriptRoot "..\docs\screenshots"
New-Item -ItemType Directory -Force -Path $dir | Out-Null

& $adb shell am start -a android.intent.action.VIEW -d $Uri | Out-Null
Start-Sleep -Seconds 3

# 先落盘到设备再 pull：PowerShell 的 > 重定向会按文本处理，直接 exec-out 会损坏 PNG
$remote = "/sdcard/$Name.png"
& $adb shell screencap -p $remote
& $adb pull $remote (Join-Path $dir "$Name.png") | Out-Null
& $adb shell rm -f $remote

$file = Join-Path $dir "$Name.png"
if (Test-Path $file) {
  Write-Host ("saved {0} ({1} bytes)" -f $file, (Get-Item $file).Length)
} else {
  Write-Host "FAILED: no screenshot produced"
}
