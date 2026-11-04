# Genera los cuadros (vector drawables) de las skins a partir de dibujos en texto.
# Uso: powershell -File tools\skins.ps1
$ErrorActionPreference = "Stop"
$out = Join-Path $PSScriptRoot "..\res\drawable"
$inv = [Globalization.CultureInfo]::InvariantCulture

function F($n) { return ([double]$n).ToString("0.###", $inv) }

$skins = @(
  @{ id = "gatita"; scale = 1.0; blink = "T"
     pal = @{ T = "#C68642"; B = "#8D5524"; C = "#F4C7A1"; G = "#5BD15B"; E = "#101010"; P = "#FF8FA3"; D = "#484F58"; K = "#1F1F24" }
     A = @(
       ".....T.....T....",
       ".....TP...PT....",
       ".....TBTBTBT....",
       "....TTTTTTTTT...",
       "....TGETTTEGT...",
       "....TTTTPTTTT...",
       ".....TCCCCCT....",
       ".D...TBTTTBT....",
       "KD..TTCCCCCTT...",
       "K...TBCCCCCBT...",
       "DD.TTBCCCCCBT...",
       ".DDDTT....TT...."
     )
     B = @(
       ".....T.....T....",
       ".....TP...PT....",
       ".....TBTBTBT....",
       "....TTTTTTTTT...",
       "....TGETTTEGT...",
       "....TTTTPTTTT...",
       ".....TCCCCCT....",
       "K....TBTTTBT....",
       "KD..TTCCCCCTT...",
       ".D..TBCCCCCBT...",
       ".DDTTBCCCCCBT...",
       "...DTT....TT...."
     )
  },
  @{ id = "marciano"; scale = 0.75; ty = 1.0; blink = "G"
     pal = @{ G = "#5BD15B"; D = "#2E8B3E"; L = "#9BF59B"; E = "#0B1A0B"; W = "#FFFFFF"; Y = "#FFE14D" }
     A = @(
       "....D.....D....",
       ".....D...D.....",
       "...GGGGGGGGG...",
       "..GGGGGGGGGGG..",
       ".GGEEGGEEGGEEG.",
       ".GGEWGGEWGGEWG.",
       ".GGGGGGGGGGGGG.",
       "..LGL.LGL.LGL..",
       "..L.L.L.L.L.L.."
     )
     B = @(
       "...D.......D...",
       "....D.....D....",
       "...GGGGGGGGG...",
       "..GGGGGGGGGGG..",
       ".GGEEGGEEGGEEG.",
       ".GGEWGGEWGGEWG.",
       ".GGGGGGGGGGGGG.",
       "..GL..GL..GL...",
       "..L...L...L...."
     )
  },
  @{ id = "gatito"; scale = 1.0; blink = "K"
     pal = @{ K = "#A7ADB4"; D = "#5F6670"; P = "#F4A0B0"; E = "#101010"; W = "#FFFFFF" }
     A = @(
       ".K........K...",
       ".KK......KK...",
       ".KPKKKKKKPK...",
       ".KKDKDDKDKK...",
       ".KEKKKKKKEK...",
       ".KEKKPPKKEK..K",
       ".KKKKKKKKKK..K",
       "..KKKKKKKK..K.",
       ".KKDKKKKDKKKK.",
       ".KKKKKKKKKK...",
       ".KKKKKKKKKK...",
       ".KW.KW.KW.KW.."
     )
     B = @(
       ".K........K...",
       ".KK......KK...",
       ".KPKKKKKKPK...",
       ".KKDKDDKDKK.K.",
       ".KEKKKKKKEK.K.",
       ".KEKKPPKKEK.K.",
       ".KKKKKKKKKK.K.",
       "..KKKKKKKK.K..",
       ".KKDKKKKDKKK..",
       ".KKKKKKKKKK...",
       ".KKKKKKKKKK...",
       "..KW.KWKW.KW.."
     )
  },
  @{ id = "robotin"; scale = 1.0; blink = "M"
     pal = @{ S = "#C9D1D9"; M = "#6E7781"; C = "#4FC3F7"; R = "#FF5A5A"; Y = "#FFD84D" }
     A = @(
       "......R.......",
       "......M.......",
       "..SSSSSSSSSS..",
       "..SMMMMMMMMS..",
       "..SMCCMMCCMS..",
       "..SMCCMMCCMS..",
       "..SMMMMMMMMS..",
       "..SSSSSSSSSS..",
       ".MSSSYSSRSSSM.",
       "M.SSSSSSSSSS.M",
       "..SSSSSSSSSS..",
       "..MM......MM.."
     )
     B = @(
       "......Y.......",
       "......M.......",
       "..SSSSSSSSSS..",
       "..SMMMMMMMMS..",
       "..SMCCMMCCMS..",
       "..SMCCMMCCMS..",
       "..SMMMMMMMMS..",
       "M.SSSSSSSSSS.M",
       ".MSSSRSSYSSSM.",
       "..SSSSSSSSSS..",
       "..SSSSSSSSSS..",
       "...MM....MM..."
     )
  },
  @{ id = "slime"; scale = 1.0; blink = "U"
     pal = @{ U = "#9B6BF2"; L = "#D2B8FF"; D = "#6A3FC0"; E = "#1A0F2E"; W = "#FFFFFF"; Y = "#FFE14D" }
     A = @(
       "......UU......",
       "....UUUUUU....",
       "...UULUUUUU...",
       "..UULLUUUYUU..",
       ".UUULUUUUUUUU.",
       ".UUEEUUUUEEUU.",
       "UUUEWUUUUEWUUU",
       "UUUUUUDDUUUUUU",
       "UUUUUUUUUUUUUU",
       ".DDDDDDDDDDDD."
     )
     B = @(
       "..............",
       ".....UUUU.....",
       "...UUUUUUUU...",
       "..UULUUUUUUU..",
       ".UULLUUUUUUYU.",
       "UUUEEUUUUEEUUU",
       "UUUEWUUUUEWUUU",
       "UUUUUUDDUUUUUU",
       "UUUUUUUUUUUUUU",
       "DDDDDDDDDDDDDD"
     )
  }
)

function Paths($grid, $pal, $mode, $blinkChar) {
  # $mode: normal | blink | angry
  $porColor = @{}
  $extra = @()
  for ($y = 0; $y -lt $grid.Count; $y++) {
    $row = $grid[$y]
    for ($x = 0; $x -lt $row.Length; $x++) {
      $ch = [string]$row[$x]
      if ($ch -eq ".") { continue }
      $esOjo = ($ch -eq "E" -or $ch -eq "C")
      $col = $pal[$ch]
      if ($esOjo -and $mode -eq "blink") {
        $col = $pal[$blinkChar]
        $extra += "M$x,$(F ($y + 0.55))h1v0.35h-1z"
      } elseif ($esOjo -and $mode -eq "angry") {
        $col = "#E5484D"
      }
      if (-not $porColor.ContainsKey($col)) { $porColor[$col] = New-Object System.Text.StringBuilder }
      [void]$porColor[$col].Append("M$x,${y}h1v1h-1z ")
    }
  }
  $s = ""
  foreach ($k in $porColor.Keys) { $s += "        <path android:fillColor=`"$k`" android:pathData=`"$($porColor[$k].ToString().Trim())`" />`n" }
  if ($extra.Count -gt 0) { $s += "        <path android:fillColor=`"#101010`" android:pathData=`"$($extra -join ' ')`" />`n" }
  return $s
}

function Escribir($nombre, $grid, $pal, $mode, $blinkChar, $scale, $tx, $ty, $extraXml) {
  $xml = "<?xml version=`"1.0`" encoding=`"utf-8`"?>`n"
  $xml += "<vector xmlns:android=`"http://schemas.android.com/apk/res/android`" android:width=`"180dp`" android:height=`"140dp`" android:viewportWidth=`"18`" android:viewportHeight=`"14`">`n"
  $xml += "    <group android:translateX=`"$(F $tx)`" android:translateY=`"$(F $ty)`" android:scaleX=`"$(F $scale)`" android:scaleY=`"$(F $scale)`">`n"
  $xml += (Paths $grid $pal $mode $blinkChar)
  $xml += "    </group>`n"
  if ($extraXml) { $xml += $extraXml }
  $xml += "</vector>`n"
  [IO.File]::WriteAllText((Join-Path $out "$nombre.xml"), $xml, (New-Object Text.UTF8Encoding $false))
}

function Flecha($x, $y, $s) {
  # Flecha amarilla apuntando hacia abajo (5 x 5 píxeles)
  $p = "M2,0h1v4h-1z M0,3h5v1h-5z M1,4h3v1h-3z M2,5h1v1h-1z"
  return "    <group android:translateX=`"$(F $x)`" android:translateY=`"$(F $y)`" android:scaleX=`"$(F $s)`" android:scaleY=`"$(F $s)`">`n        <path android:fillColor=`"#FFE14D`" android:pathData=`"$p`" />`n    </group>`n"
}

foreach ($sk in $skins) {
  $id = $sk.id; $A = $sk.A; $B = $sk.B; $pal = $sk.pal; $sc = $sk.scale; $bl = $sk.blink
  $w = $A[0].Length * $sc; $h = $A.Count * $sc
  $tx = (18 - $w) / 2
  if ($sk.ContainsKey("ty")) { $ty = $sk.ty } else { $ty = 14 - $h }
  $tyJ = [Math]::Max(0, $ty - 2)
  if ($id -eq "marciano") { $tyJ = 0 }
  $p = "s_${id}_"
  Escribir "${p}n" $A $pal "normal" $bl $sc $tx $ty $null
  Escribir "${p}b" $A $pal "blink" $bl $sc $tx $ty $null
  Escribir "${p}q" $A $pal "blink" $bl $sc $tx ($ty + 1) $null
  Escribir "${p}j" $B $pal "normal" $bl $sc $tx $tyJ $null
  Escribir "${p}a" $A $pal "angry" $bl $sc $tx $ty $null
  Escribir "${p}c" $A $pal "angry" $bl $sc ($tx + 0.35) $ty $null
  Escribir "${p}d" $A $pal "angry" $bl $sc ($tx - 0.35) $ty $null
  if ($id -eq "marciano") {
    $cx = 9 - 2.5 * 0.6
    $ys = @(7.6, 9.0, 10.4, 11.8)
    for ($i = 0; $i -lt 4; $i++) {
      $fl = Flecha $cx $ys[$i] 0.6
      if ($i -ge 2) { $fl += Flecha ($cx - 4) ($ys[$i] - 2.8) 0.6; $fl += Flecha ($cx + 4) ($ys[$i] - 2.8) 0.6 }
      $g = if ($i % 2 -eq 0) { $B } else { $A }
      Escribir "${p}$($i + 1)" $g $pal "normal" $bl $sc $tx $ty $fl
    }
    Escribir "${p}w" $B $pal "normal" $bl $sc $tx $ty (Flecha $cx 7.6 0.6)
    Escribir "${p}s" $A $pal "normal" $bl $sc $tx $ty (Flecha $cx 10.4 0.6)
  } else {
    Escribir "${p}w" $B $pal "normal" $bl $sc $tx $ty $null
    Escribir "${p}s" $B $pal "normal" $bl $sc $tx ($ty + 0.0) $null
  }
}
Write-Output "Skins generadas en $out"
