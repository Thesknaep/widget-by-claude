$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$env:JAVA_HOME = (Get-ChildItem "C:\Program Files\Microsoft" -Filter "jdk-17*" | Select-Object -First 1).FullName
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$bt = "$env:LOCALAPPDATA\Android\Sdk\dl\bt\android-15"
$jar = "$env:LOCALAPPDATA\Android\Sdk\dl\pf\android-35\android.jar"
$out = "$root\build"

if (Test-Path $out) { Remove-Item -Recurse -Force $out }
New-Item -ItemType Directory -Force "$out\gen", "$out\classes", "$out\dex" | Out-Null

function Run($exe, [string[]]$a) {
    $prev = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    & $exe @a 2>&1 | ForEach-Object { "$_" }
    $ErrorActionPreference = $prev
    if ($LASTEXITCODE -ne 0) { throw "Fallo: $exe" }
}

Run "$bt\aapt2.exe" @("compile", "--dir", "$root\res", "-o", "$out\res.zip")
Run "$bt\aapt2.exe" @("link", "-o", "$out\base.apk", "-I", $jar, "--manifest", "$root\AndroidManifest.xml",
    "--java", "$out\gen", "--min-sdk-version", "26", "--target-sdk-version", "35", "$out\res.zip")

$src = @(Get-ChildItem -Recurse "$root\src", "$out\gen" -Filter *.java | ForEach-Object FullName)
Run "javac" (@("-source", "11", "-target", "11", "-nowarn", "-encoding", "UTF-8", "-classpath", $jar, "-d", "$out\classes") + $src)

$classes = @(Get-ChildItem -Recurse "$out\classes" -Filter *.class | ForEach-Object FullName)
Run "$bt\d8.bat" (@("--lib", $jar, "--min-api", "26", "--output", "$out\dex") + $classes)

Copy-Item "$out\base.apk" "$out\unsigned.apk"
Push-Location "$out\dex"
Run "jar" @("uf", "$out\unsigned.apk", "classes.dex")
Pop-Location

Run "$bt\zipalign.exe" @("-f", "-p", "4", "$out\unsigned.apk", "$out\aligned.apk")

$ks = "$root\claude.keystore"
# La contraseña de la firma está en firma.local (archivo privado, no se sube a GitHub).
$clave = (Get-Content "$root\firma.local" -Raw).Trim()
if (-not (Test-Path $ks)) {
    Run "keytool" @("-genkeypair", "-keystore", $ks, "-alias", "claude", "-keyalg", "RSA", "-keysize", "2048",
        "-validity", "10000", "-storepass", $clave, "-keypass", $clave, "-dname", "CN=Claude Widget")
}
Run "$bt\apksigner.bat" @("sign", "--ks", $ks, "--ks-pass", "pass:$clave", "--out", "$root\ClaudeWidget.apk", "$out\aligned.apk")
Write-Output "OK: $root\ClaudeWidget.apk"
