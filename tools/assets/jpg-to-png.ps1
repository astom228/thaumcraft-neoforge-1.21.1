# Перевод JPEG-текстур оригинала в PNG (Minecraft 1.21 читает только PNG). Пиксели не меняются — только формат файла.
# Использование: powershell -ExecutionPolicy Bypass -File tools/assets/jpg-to-png.ps1
Add-Type -AssemblyName System.Drawing
$root = Join-Path $PSScriptRoot '..\..\src\main\resources\assets\thaumcraft\textures'
Get-ChildItem -Path $root -Recurse -Filter *.jpg | ForEach-Object {
    $png = [System.IO.Path]::ChangeExtension($_.FullName, '.png')
    $img = [System.Drawing.Image]::FromFile($_.FullName)
    $bmp = New-Object System.Drawing.Bitmap $img
    $img.Dispose()
    $bmp.Save($png, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Remove-Item $_.FullName
    Write-Output ("{0} -> {1}" -f $_.Name, [System.IO.Path]::GetFileName($png))
}
