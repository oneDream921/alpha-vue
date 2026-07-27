$sound = Join-Path $PSScriptRoot "assets\notify.mp3"
if (Test-Path -LiteralPath $sound) {
  try {
    Add-Type -AssemblyName presentationCore
    $player = New-Object System.Windows.Media.MediaPlayer
    $soundUri = (New-Object System.Uri ((Resolve-Path -LiteralPath $sound).Path)).AbsoluteUri
    $player.Open([Uri]$soundUri)
    $player.Play()
    Start-Sleep -Seconds 2
  } catch {}
}
Write-Output "{}"
exit 0
