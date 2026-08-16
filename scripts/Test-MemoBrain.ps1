param(
  [Parameter(Mandatory=$true)][string]$ApiBase,
  [Parameter(Mandatory=$true)][string]$AppApiKey,
  [string]$Message = "[MB:TEXT] PowerShell 5.x からのテストメモ"
)
$ErrorActionPreference = "Stop"
$ApiBase = $ApiBase.TrimEnd('/')
$headers = @{ Authorization = "Bearer $AppApiKey"; "Content-Type" = "application/json" }
$body = @{ inputs=@{}; query=$Message; response_mode="blocking"; conversation_id=""; user="memobrain-powershell5"; files=@() } | ConvertTo-Json -Depth 8
Write-Host "=== MemoBrain Dify API Test ===" -ForegroundColor Cyan
Write-Host "POST $ApiBase/chat-messages"
try {
  $r = Invoke-RestMethod -Method Post -Uri "$ApiBase/chat-messages" -Headers $headers -Body ([Text.Encoding]::UTF8.GetBytes($body)) -ContentType "application/json; charset=utf-8"
  Write-Host "OK" -ForegroundColor Green
  Write-Host $r.answer
} catch {
  Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
  if ($_.Exception.Response) {
    $sr = New-Object IO.StreamReader($_.Exception.Response.GetResponseStream())
    Write-Host $sr.ReadToEnd()
  }
  exit 1
}
