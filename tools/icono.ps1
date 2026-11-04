# Genera el ícono de la app (pixel art 24 x 24) como vector drawable y una vista previa PNG.
# Uso: powershell -File tools\icono.ps1
$ErrorActionPreference = "Stop"
$raiz = Join-Path $PSScriptRoot ".."

$g = @(
  "........................",
  "...........W............",
  "..........WWW...........",
  "...........W.....L......",
  ".......KRRRRRK....LP....",
  "......KRRRRRRRK....LP...",
  ".....KRRRRRRRRRK....LP..",
  ".....WRWRWRWRWRW....SPP.",
  ".....OOOOOOOOOOO....S.PP",
  ".....OOEOOOOOEOO...S...P",
  ".....OOEOOOOOEOO..S.....",
  "...OOOOOOOOOOOOOOO......",
  "...OOOOOOOOOOOOOOO......",
  ".....OOOOOOOOOOO........",
  ".....ZZZZZZZZZZZ........",
  "......O.O...O.O.........",
  "......O.O...O.O.........",
  "........................",
  "...Y............GG......",
  "..YYY...........GG.YY...",
  "...Y..............YCCY..",
  "..................YCCY..",
  "...................YY...",
  "........................"
)
$pal = @{
  O = "#D77757"; Z = "#B85C3E"; E = "#101010"
  R = "#E5484D"; K = "#A32D2D"; W = "#F5EDE8"
  P = "#9B6BF2"; L = "#E3D2FF"; S = "#8D5524"
  Y = "#FFE14D"; C = "#F2A65A"; G = "#4FC3F7"
}

$porColor = @{}
for ($y = 0; $y -lt 24; $y++) {
  for ($x = 0; $x -lt 24; $x++) {
    $ch = [string]$g[$y][$x]
    if ($ch -eq ".") { continue }
    $c = $pal[$ch]
    if (-not $porColor.ContainsKey($c)) { $porColor[$c] = New-Object Text.StringBuilder }
    [void]$porColor[$c].Append("M$x,${y}h1v1h-1z ")
  }
}

# Fondo redondeado oscuro con un leve degradado de pixeles (dos tonos).
$xml = "<?xml version=`"1.0`" encoding=`"utf-8`"?>`n"
$xml += "<vector xmlns:android=`"http://schemas.android.com/apk/res/android`" android:width=`"108dp`" android:height=`"108dp`" android:viewportWidth=`"24`" android:viewportHeight=`"24`">`n"
$xml += "    <path android:fillColor=`"#2A1F3D`" android:pathData=`"M5,0h14a5,5 0 0 1 5,5v14a5,5 0 0 1 -5,5h-14a5,5 0 0 1 -5,-5v-14a5,5 0 0 1 5,-5z`" />`n"
$xml += "    <path android:fillColor=`"#3B2A57`" android:pathData=`"M0,14h24v5a5,5 0 0 1 -5,5h-14a5,5 0 0 1 -5,-5z`" />`n"
foreach ($k in $porColor.Keys) {
  $xml += "    <path android:fillColor=`"$k`" android:pathData=`"$($porColor[$k].ToString().Trim())`" />`n"
}
$xml += "</vector>`n"
[IO.File]::WriteAllText((Join-Path $raiz "res\drawable\ic_launcher.xml"), $xml, (New-Object Text.UTF8Encoding $false))

# Vista previa
Add-Type -AssemblyName System.Drawing
$S = 20
$bmp = New-Object Drawing.Bitmap (24 * $S), (24 * $S)
$gr = [Drawing.Graphics]::FromImage($bmp)
$gr.Clear([Drawing.ColorTranslator]::FromHtml("#2A1F3D"))
$gr.FillRectangle((New-Object Drawing.SolidBrush ([Drawing.ColorTranslator]::FromHtml("#3B2A57"))), 0, 14 * $S, 24 * $S, 10 * $S)
for ($y = 0; $y -lt 24; $y++) {
  for ($x = 0; $x -lt 24; $x++) {
    $ch = [string]$g[$y][$x]
    if ($ch -eq ".") { continue }
    $gr.FillRectangle((New-Object Drawing.SolidBrush ([Drawing.ColorTranslator]::FromHtml($pal[$ch]))), $x * $S, $y * $S, $S, $S)
  }
}
$prev = Join-Path $raiz "..\vista\icono.png"
$bmp.Save($prev)
Write-Output "Icono generado. Vista previa: $prev"
