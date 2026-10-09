Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName PresentationCore
Add-Type -AssemblyName WindowsBase

$icoPath = (Resolve-Path "Camion-icono.ico").Path
$fileStream = [System.IO.File]::OpenRead($icoPath)
$decoder = New-Object System.Windows.Media.Imaging.IconBitmapDecoder(
    $fileStream,
    [System.Windows.Media.Imaging.BitmapCreateOptions]::PreservePixelFormat,
    [System.Windows.Media.Imaging.BitmapCacheOption]::None
)

$bestFrame = $null
$maxRes = 0

foreach ($frame in $decoder.Frames) {
    if ($frame.PixelWidth -ge $maxRes) {
        $maxRes = $frame.PixelWidth
        $bestFrame = $frame
    }
}

$encoder = New-Object System.Windows.Media.Imaging.PngBitmapEncoder
$encoder.Frames.Add($bestFrame)
$masterPngPath = "$PSScriptRoot\master_icon.png"
$outStream = [System.IO.File]::Create($masterPngPath)
$encoder.Save($outStream)
$outStream.Close()
$fileStream.Close()

function Resize-Png($srcPath, $dstPath, $targetWidth, $targetHeight, $padRatio) {
    $srcBmp = [System.Drawing.Image]::FromFile($srcPath)
    $dstBmp = New-Object System.Drawing.Bitmap($targetWidth, $targetHeight)
    $g = [System.Drawing.Graphics]::FromImage($dstBmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)
    
    if ($padRatio -gt 0) {
        # Centered with padding
        $drawW = [int]($targetWidth * $padRatio)
        $drawH = [int]($targetHeight * $padRatio)
        $posX = [int](($targetWidth - $drawW) / 2)
        $posY = [int](($targetHeight - $drawH) / 2)
        $g.DrawImage($srcBmp, $posX, $posY, $drawW, $drawH)
    } else {
        $g.DrawImage($srcBmp, 0, 0, $targetWidth, $targetHeight)
    }
    
    $g.Dispose()
    $srcBmp.Dispose()
    
    $parent = [System.IO.Path]::GetDirectoryName($dstPath)
    if (-not (Test-Path $parent)) { New-Item -ItemType Directory -Path $parent -Force | Out-Null }
    $dstBmp.Save($dstPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $dstBmp.Dispose()
}

$mipmaps = @{
    "app\src\main\res\mipmap-mdpi\ic_launcher.png" = 48
    "app\src\main\res\mipmap-mdpi\ic_launcher_round.png" = 48
    "app\src\main\res\mipmap-hdpi\ic_launcher.png" = 72
    "app\src\main\res\mipmap-hdpi\ic_launcher_round.png" = 72
    "app\src\main\res\mipmap-xhdpi\ic_launcher.png" = 96
    "app\src\main\res\mipmap-xhdpi\ic_launcher_round.png" = 96
    "app\src\main\res\mipmap-xxhdpi\ic_launcher.png" = 144
    "app\src\main\res\mipmap-xxhdpi\ic_launcher_round.png" = 144
    "app\src\main\res\mipmap-xxxhdpi\ic_launcher.png" = 192
    "app\src\main\res\mipmap-xxxhdpi\ic_launcher_round.png" = 192
}

foreach ($target in $mipmaps.Keys) {
    $fullPath = Join-Path (Get-Location) $target
    Resize-Png $masterPngPath $fullPath $mipmaps[$target] $mipmaps[$target] 0
}

# Adaptive foreground: 432x432 (108dp on xxxhdpi) with ~70% safe-zone scaling
$fgPath = Join-Path (Get-Location) "app\src\main\res\drawable\ic_launcher_foreground.png"
Resize-Png $masterPngPath $fgPath 432 432 0.70

# In-app Compose drawable logo
$composeLogoPath = Join-Path (Get-Location) "shared\src\commonMain\composeResources\drawable\ic_app_logo.png"
Resize-Png $masterPngPath $composeLogoPath 256 256 0

# Remove obsolete XML foregrounds to prevent duplicate resource conflicts
Remove-Item "app\src\main\res\drawable\ic_launcher_foreground.xml" -ErrorAction SilentlyContinue
Remove-Item "shared\src\commonMain\composeResources\drawable\ic_launcher_foreground.xml" -ErrorAction SilentlyContinue
Remove-Item $masterPngPath -ErrorAction SilentlyContinue
Remove-Item "generate_icons.ps1" -ErrorAction SilentlyContinue

Write-Host "Application logo setup completed successfully!"
