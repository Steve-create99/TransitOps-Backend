$html = Invoke-WebRequest "https://transitops-frontend.pages.dev/login" -UseBasicParsing -TimeoutSec 30
if ($html.Content -match 'assets/index-[A-Za-z0-9_-]+\.js') { $js = $Matches[0] } else { throw 'no js' }
Write-Host "JS=$js"
$bundle = (Invoke-WebRequest "https://transitops-frontend.pages.dev/$js" -UseBasicParsing -TimeoutSec 90).Content
foreach ($p in @('web-production-f8ec21','transitops-backend-production','railway.app','localhost:8080','/api/auth/login')) {
  $c = ([regex]::Matches($bundle, [regex]::Escape($p))).Count
  Write-Host ("{0}={1}" -f $p, $c)
}
$matchesFound = [regex]::Matches($bundle, 'https://[A-Za-z0-9.-]+\.up\.railway\.app(?:/api)?')
foreach ($m in $matchesFound) { Write-Host ("URL={0}" -f $m.Value) }
# Also look for BASE patterns like "/api"
$m2 = [regex]::Match($bundle, 'VITE_API_URL|DEFAULT_PROD|web-production')
Write-Host ("HINT={0}" -f $m2.Value)
