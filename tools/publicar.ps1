# Publica una versión nueva de Widget by Claude en GitHub.
# 1) Sube versionCode/versionName en AndroidManifest.xml
# 2) powershell -File tools\publicar.ps1 -Notas "Qué hay de nuevo"
param([string]$Notas = "Mejoras y novedades")
$ErrorActionPreference = "Stop"
$raiz = Resolve-Path (Join-Path $PSScriptRoot "..")
$gh = "C:\Program Files\GitHub CLI\gh.exe"
$repo = "Thesknaep/widget-by-claude"
Set-Location $raiz

$man = Get-Content AndroidManifest.xml -Raw -Encoding UTF8
$code = [int]([regex]::Match($man, 'versionCode="(\d+)"').Groups[1].Value)
$name = [regex]::Match($man, 'versionName="([^"]+)"').Groups[1].Value
Write-Output "Publicando versión $name (código $code)"

& .\build.ps1 | Select-Object -Last 1
Copy-Item ClaudeWidget.apk WidgetByClaude.apk -Force

New-Item -ItemType Directory -Force contenido | Out-Null
Copy-Item res\raw\consejos.txt contenido\consejos.txt -Force

$json = [ordered]@{
  versionCode = $code
  versionName = $name
  apk = "https://github.com/$repo/releases/download/v$name/WidgetByClaude.apk"
  notas = $Notas
} | ConvertTo-Json
[IO.File]::WriteAllText("$raiz\version.json", $json, (New-Object Text.UTF8Encoding $false))

git add -A
git commit -m "Versión $name" | Out-Null
git push origin main
& $gh release create "v$name" WidgetByClaude.apk --repo $repo --title "Widget by Claude $name" --notes $Notas
Write-Output "Listo: version.json apunta a v$name. Las apps lo verán en su próxima revisión."
