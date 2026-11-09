# Genera los ítems nuevos (pixel art 8 x 8) como vector drawables y una vista previa.
# Uso: powershell -File tools\items.ps1
$ErrorActionPreference = "Stop"
$raiz = Join-Path $PSScriptRoot ".."

$pal = [ordered]@{
  C = "#D9A066"; X = "#B07A3C"; Y = "#FFE14D"; R = "#E5484D"; P = "#FF8FA3"; W = "#FFFFFF"
  K = "#1F1F24"; G = "#5BD15B"; S = "#6E7781"; B = "#2F6FDB"; O = "#FF8A00"; M = "#7A4A2A"
  L = "#9BF59B"; V = "#9B6BF2"
}

$items = [ordered]@{
  pizza = @("CCCCCCCC", ".YRYYRY.", ".YYYYYY.", "..YRYY..", "..YYYR..", "...YY...", "...Y....", "........")
  helado = @("..PPPP..", ".PPWPPP.", ".PPPPPP.", "..CCCC..", "..CXCC..", "...CX...", "...CC...", "....C...")
  balon = @("..WWWW..", ".WWKKWW.", "WWKKKKWW", "WKWWWWKW", "WWWKKWWW", "WKWKKWKW", ".WWWWWW.", "..WWWW..")
  control = @("........", ".SSSSSS.", "SSKSSRSS", "SKKKSSBS", "SSKSSSSS", "SSSSSSSS", ".SS..SS.", "........")
  libro = @(".BBBBBB.", ".BWWWWBB", ".BWKKWBB", ".BWWWWBB", ".BWKKWBB", ".BWWWWBB", ".BBBBBBB", "........")
  cohete = @("...RR...", "..WWWW..", "..WBBW..", "..WWWW..", ".RWWWWR.", "RR.WW.RR", "...OO...", "...YY...")
  estrella = @("...YY...", "...YY...", "YYYYYYYY", ".YYYYYY.", "..YYYY..", ".YYYYYY.", ".YY..YY.", "Y......Y")
  regalo = @("..R..R..", "...RR...", "PPPRRPPP", "PPPRRPPP", ".PPRRPP.", ".PPRRPP.", ".PPRRPP.", ".PPPPPP.")
  trofeo = @("YYYYYYYY", "Y.YYYY.Y", "Y.YYYY.Y", ".YYYYYY.", "..YYYY..", "...YY...", "..YYYY..", ".XXXXXX.")
  flor = @(".P.P....", "PPPPP...", ".PYP....", "PPPPP...", ".P.P....", "..G.....", "..GG....", "..G.....")
  varita = @("......Y.", ".....YWY", "......Y.", ".....S..", "....S...", "...S....", "..S.....", ".S......")
  hamburguesa = @("..CCCC..", ".CCWCWC.", ".CCCCCC.", ".LLLLLL.", ".RRRRRR.", ".MMMMMM.", ".CCCCCC.", "........")
}

foreach ($nombre in $items.Keys) {
  $g = $items[$nombre]
  $porColor = [ordered]@{}
  for ($y = 0; $y -lt 8; $y++) {
    for ($x = 0; $x -lt 8; $x++) {
      $ch = [string]$g[$y][$x]
      if ($ch -eq ".") { continue }
      $c = $pal[$ch]
      if (-not $porColor.Contains($c)) { $porColor[$c] = New-Object Text.StringBuilder }
      [void]$porColor[$c].Append("M$x,${y}h1v1h-1z ")
    }
  }
  $xml = "<?xml version=`"1.0`" encoding=`"utf-8`"?>`n"
  $xml += "<vector xmlns:android=`"http://schemas.android.com/apk/res/android`" android:width=`"32dp`" android:height=`"32dp`" android:viewportWidth=`"8`" android:viewportHeight=`"8`">`n"
  foreach ($k in $porColor.Keys) { $xml += "    <path android:fillColor=`"$k`" android:pathData=`"$($porColor[$k].ToString().Trim())`" />`n" }
  $xml += "</vector>`n"
  [IO.File]::WriteAllText((Join-Path $raiz "res\drawable\item_$nombre.xml"), $xml, (New-Object Text.UTF8Encoding $false))
}

# Vista previa
Add-Type -AssemblyName System.Drawing
$S = 12; $n = $items.Count
$bmp = New-Object Drawing.Bitmap ($n * (8 * $S + 10) + 10), (8 * $S + 20)
$gr = [Drawing.Graphics]::FromImage($bmp)
$gr.Clear([Drawing.ColorTranslator]::FromHtml("#1A1716"))
$i = 0
foreach ($nombre in $items.Keys) {
  $g = $items[$nombre]
  for ($y = 0; $y -lt 8; $y++) {
    for ($x = 0; $x -lt 8; $x++) {
      $ch = [string]$g[$y][$x]
      if ($ch -eq ".") { continue }
      $gr.FillRectangle((New-Object Drawing.SolidBrush ([Drawing.ColorTranslator]::FromHtml($pal[$ch]))), 10 + $i * (8 * $S + 10) + $x * $S, 10 + $y * $S, $S, $S)
    }
  }
  $i++
}
$prev = Join-Path $raiz "..\imagenes\items nuevos.png"
$bmp.Save($prev)
Write-Output "Ítems generados: $($items.Keys -join ', ')"
