# Windows PowerShell helper. Never prints key bytes/passwords or commits keys.
$ErrorActionPreference = 'Stop'
$secure=$null; $confirm=$null
$Repo = 'workto0725/boardscore'
$AliasName = 'boardscoreplus-upload'
function Private-Run([string]$Exe,[string]$Arguments,[string]$Text,[string]$Password) {
    $i = New-Object Diagnostics.ProcessStartInfo
    $i.FileName=$Exe; $i.Arguments=$Arguments; $i.UseShellExecute=$false
    $i.RedirectStandardInput=$true; $i.RedirectStandardOutput=$true; $i.RedirectStandardError=$true
    $i.CreateNoWindow=$true
    if ($Password) { $i.EnvironmentVariables['BOARDSCORE_LOCAL_KEY_PASSWORD']=$Password }
    $p=New-Object Diagnostics.Process
    $p.StartInfo=$i
    try {
        [void]$p.Start()
        $out=$p.StandardOutput.ReadToEndAsync(); $err=$p.StandardError.ReadToEndAsync()
        if ($null -ne $Text) { $p.StandardInput.Write($Text) }
        $p.StandardInput.Close(); $p.WaitForExit()
        [void]$out.Result; [void]$err.Result
        if ($p.ExitCode -ne 0) { throw 'Private operation failed. Check account access/password. Secret diagnostics were not printed.' }
    } finally { $p.Dispose() }
}
function Unwrap($Value) {
    $ptr=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($Value)
    try { return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) }
}
try {
    Write-Host 'BoardScore Plus: local signing key, safe backup, GitHub Secrets and AAB build.'
    $cmd=Get-Command gh.exe -ErrorAction SilentlyContinue
    $Gh=if ($cmd) { $cmd.Source } else { Join-Path $env:ProgramFiles 'GitHub CLI\gh.exe' }
    if (-not (Test-Path $Gh)) {
        $winget=Get-Command winget.exe -ErrorAction SilentlyContinue
        if (-not $winget) { throw 'Install GitHub CLI from https://cli.github.com/ and rerun.' }
        Write-Host 'Installing official GitHub CLI with Windows Package Manager.'
        & winget.exe install --id GitHub.cli --exact --source winget
        if ($LASTEXITCODE -ne 0 -or -not (Test-Path $Gh)) { throw 'GitHub CLI install incomplete. Install from https://cli.github.com/ and rerun.' }
    }
    & $Gh auth status --hostname github.com
    if ($LASTEXITCODE -ne 0) {
        & $Gh auth login --hostname github.com --git-protocol https --web
        if ($LASTEXITCODE -ne 0) { throw 'GitHub login incomplete.' }
    }
    $login=(& $Gh api user --jq .login | Out-String).Trim()
    if ($LASTEXITCODE -ne 0 -or $login -ne 'workto0725') { throw 'Use the husband GitHub account workto0725. Run: gh auth switch --user workto0725' }
    $json=& $Gh secret list --repo $Repo --json name
    if ($LASTEXITCODE -ne 0) { throw 'Cannot read repository secret names.' }
    $names=@('BOARDSCORE_KEYSTORE_BASE64','BOARDSCORE_KEYSTORE_PASSWORD','BOARDSCORE_KEY_ALIAS','BOARDSCORE_KEY_PASSWORD')
    $found=@($json | ConvertFrom-Json | Where-Object { $_.name -in $names })
    if ($found.Count -gt 0) { throw 'Signing secrets already exist. Do not overwrite them or generate another key; verify the existing setup first.' }
    $cmd=Get-Command keytool.exe -ErrorAction SilentlyContinue
    $Keytool=if ($cmd) { $cmd.Source } else { Join-Path $env:ProgramFiles 'Android\Android Studio\jbr\bin\keytool.exe' }
    if (-not (Test-Path $Keytool)) { throw 'Android Studio JDK keytool.exe not found. Add the JDK bin folder to PATH.' }
    $dir=Join-Path $env:LOCALAPPDATA 'BoardScoreSigning'
    [void](New-Item -ItemType Directory -Force -Path $dir)
    $sid=[Security.Principal.WindowsIdentity]::GetCurrent().User.Value
    & icacls.exe $dir /inheritance:r /grant:r "*$($sid):(OI)(CI)F" '*S-1-5-18:(OI)(CI)F' | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Cannot restrict local key folder permissions.' }
    $key=Join-Path $dir 'boardscoreplus-upload.jks'
    Write-Host "Encrypted key location: $key"
    Write-Host 'Save your password in your password manager; it is not saved in a text file.'
    $secure=Read-Host 'Key password (12+ ASCII characters, no spaces; existing key: its password)' -AsSecureString
    $plain=Unwrap $secure
    if ($plain.Length -lt 12 -or $plain -match '[^\x21-\x7e]') { throw 'Use 12+ printable ASCII characters without spaces.' }
    if (-not (Test-Path $key)) {
        $confirm=Read-Host 'Repeat key password' -AsSecureString
        $again=Unwrap $confirm
        if ($plain -cne $again) { throw 'Passwords do not match; no key was created.' }
        $again=$null
        Private-Run $Keytool ('-genkeypair -noprompt -keystore "'+$key+'" -alias '+$AliasName+' -keyalg RSA -keysize 3072 -validity 10000 -storetype PKCS12 -dname "CN=BoardScore Plus Upload" -storepass:env BOARDSCORE_LOCAL_KEY_PASSWORD -keypass:env BOARDSCORE_LOCAL_KEY_PASSWORD') $null $plain
    }
    Private-Run $Keytool ('-list -keystore "'+$key+'" -alias '+$AliasName+' -storepass:env BOARDSCORE_LOCAL_KEY_PASSWORD') $null $plain
    Add-Type -AssemblyName System.Windows.Forms
    $picker=New-Object System.Windows.Forms.FolderBrowserDialog
    $picker.Description='Choose a separate PRIVATE backup folder (USB recommended; never a Git repository).'
    if ($picker.ShowDialog() -ne [Windows.Forms.DialogResult]::OK) { throw 'Backup cancelled. Local key retained; nothing uploaded.' }
    $backupDir=[IO.Path]::GetFullPath($picker.SelectedPath).TrimEnd('\')
    $picker.Dispose()
    if ($backupDir -eq [IO.Path]::GetFullPath($dir).TrimEnd('\')) { throw 'Choose a different backup location.' }
    $parent=New-Object IO.DirectoryInfo($backupDir)
    while ($null -ne $parent) {
        if (Test-Path (Join-Path $parent.FullName '.git')) { throw 'Never back up signing keys inside a Git repository.' }
        $parent=$parent.Parent
    }
    $backup=Join-Path $backupDir 'boardscoreplus-upload.jks'
    if (Test-Path $backup) {
        if ((Get-FileHash $backup).Hash -ne (Get-FileHash $key).Hash) { throw 'A different backup key exists; it will not be overwritten.' }
    } else { Copy-Item -LiteralPath $key -Destination $backup }
    if ((Get-FileHash $backup).Hash -ne (Get-FileHash $key).Hash) { throw 'Backup verification failed.' }
    Write-Host 'Encrypted key and backup verified. Uploading only to Actions Secrets.'
    Private-Run $Gh ("secret set BOARDSCORE_KEY_ALIAS --repo $Repo") $AliasName $null
    Private-Run $Gh ("secret set BOARDSCORE_KEYSTORE_PASSWORD --repo $Repo") $plain $null
    Private-Run $Gh ("secret set BOARDSCORE_KEY_PASSWORD --repo $Repo") $plain $null
    $b64=[Convert]::ToBase64String([IO.File]::ReadAllBytes($key))
    Private-Run $Gh ("secret set BOARDSCORE_KEYSTORE_BASE64 --repo $Repo") $b64 $null
    $b64=$null; $plain=$null
    Write-Host 'Four Secrets registered. No secret values printed.'
    & $Gh workflow run android-signed-aab.yml --repo $Repo --ref main
    if ($LASTEXITCODE -ne 0) { throw 'Secrets registered; start the signed AAB workflow from Actions.' }
    Write-Host 'Signed AAB build requested. Tell ChatGPT only: signing setup completed.'
    Start-Process 'https://github.com/workto0725/boardscore/actions/workflows/android-signed-aab.yml'
} catch {
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host 'Keep any generated key. If setup was partial, do not generate a different key.'
    exit 1
} finally {
    $plain=$null; $again=$null; $b64=$null
    if ($secure) { $secure.Dispose() }
    if ($confirm) { $confirm.Dispose() }
}
