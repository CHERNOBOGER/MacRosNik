[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$CertificatePath,

    [Parameter(Mandatory = $true)]
    [string]$CertificatePassword,

    [Parameter(Mandatory = $true)]
    [string[]]$Files,

    [string]$TimestampUrl = "http://timestamp.digicert.com"
)

$ErrorActionPreference = "Stop"

function Find-SignTool {
    $command = Get-Command signtool.exe -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $searchRoots = @(
        "C:\Program Files (x86)\Windows Kits",
        "C:\Program Files (x86)\Microsoft SDKs"
    )

    $candidates = foreach ($root in $searchRoots) {
        if (Test-Path -LiteralPath $root) {
            Get-ChildItem -Path $root -Recurse -Filter signtool.exe -ErrorAction SilentlyContinue
        }
    }

    $preferred = $candidates |
        Sort-Object `
            @{ Expression = { $_.FullName -match "\\x64\\" }; Descending = $true }, `
            @{ Expression = { $_.FullName }; Descending = $true } |
        Select-Object -First 1

    if (-not $preferred) {
        throw "signtool.exe not found on this runner."
    }

    return $preferred.FullName
}

if (-not (Test-Path -LiteralPath $CertificatePath)) {
    throw "Certificate file not found: $CertificatePath"
}

$resolvedFiles = @()
foreach ($file in $Files) {
    if ([string]::IsNullOrWhiteSpace($file)) {
        continue
    }
    $resolvedFiles += (Resolve-Path -LiteralPath $file).Path
}

if ($resolvedFiles.Count -eq 0) {
    throw "No files provided for signing."
}

$signTool = Find-SignTool

foreach ($file in $resolvedFiles) {
    Write-Host "Signing $file"
    & $signTool sign `
        /fd SHA256 `
        /td SHA256 `
        /tr $TimestampUrl `
        /f $CertificatePath `
        /p $CertificatePassword `
        $file

    if ($LASTEXITCODE -ne 0) {
        throw "signtool.exe failed for $file with exit code $LASTEXITCODE"
    }
}
