$html = Invoke-WebRequest "https://transitops-frontend.pages.dev/login" -UseBasicParsing -TimeoutSec 30
if ($html.Content -match 'assets/index-[A-Za-z0-9_-]+\.js') { $js = $Matches[0] } else { $js = $null }
Write-Host "JS=$js"
$jsUrl = "https://transitops-frontend.pages.dev/$js"
$bundle = (Invoke-WebRequest $jsUrl -UseBasicParsing -TimeoutSec 90).Content
foreach ($p in @('web-production-f8ec21','transitops-backend-production','railway.app','localhost:8080')) {
  $c = ([regex]::Matches($bundle, [regex]::Escape($p))).Count
  Write-Host ("{0}={1}" -f $p, $c)
}
$m = [regex]::Match($bundle, 'https://[A-Za-z0-9.-]+\.up\.railway\.app(?:/api)?')
if ($m.Success) { Write-Host ("FOUND={0}" -f $m.Value) } else { Write-Host "FOUND=none" }
