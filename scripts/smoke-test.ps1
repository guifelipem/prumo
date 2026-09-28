param(
    [string] $AuthBaseUrl = 'http://localhost:8081',
    [string] $AccountBaseUrl = 'http://localhost:8082',
    [string] $TransactionBaseUrl = 'http://localhost:8083'
)

$ErrorActionPreference = 'Stop'
$auth = $AuthBaseUrl.TrimEnd('/')
$accounts = $AccountBaseUrl.TrimEnd('/')
$transactions = $TransactionBaseUrl.TrimEnd('/')

function Invoke-Check {
    param(
        [string] $Method,
        [string] $Uri,
        [int] $ExpectedStatus,
        [object] $Payload,
        [string] $Token
    )

    $request = @{
        Method = $Method
        Uri = $Uri
        SkipHttpErrorCheck = $true
    }
    if ($null -ne $Payload) {
        $request.ContentType = 'application/json'
        $request.Body = ConvertTo-Json -InputObject $Payload -Compress
    }
    if ($Token) {
        $request.Headers = @{ Authorization = "Bearer $Token" }
    }

    $response = Invoke-WebRequest @request
    if ([int] $response.StatusCode -ne $ExpectedStatus) {
        throw "$Method $Uri retornou $([int] $response.StatusCode); esperado: $ExpectedStatus"
    }
    if ($response.Content) {
        $content = if ($response.Content -is [byte[]]) {
            [System.Text.Encoding]::UTF8.GetString($response.Content)
        } else {
            [string] $response.Content
        }
        return ConvertFrom-Json -InputObject $content
    }
    return $null
}

foreach ($baseUrl in @($auth, $accounts, $transactions)) {
    $health = Invoke-Check -Method Get -Uri "$baseUrl/actuator/health" -ExpectedStatus 200
    if ($health.status -ne 'UP') { throw "Health check indisponível: $baseUrl" }
}

$suffix = [guid]::NewGuid().ToString('N')
$password = 'Prumo-' + [guid]::NewGuid().ToString('N')
$email = "smoke+$suffix@prumo.test"

$user = Invoke-Check -Method Post -Uri "$auth/users" -ExpectedStatus 201 -Payload @{
    email = $email
    password = $password
}
if (-not $user.id -or $user.email -ne $email -or $user.passwordHash) {
    throw 'Resposta de cadastro inesperada.'
}

$session = Invoke-Check -Method Post -Uri "$auth/sessions" -ExpectedStatus 200 -Payload @{
    email = $email
    password = $password
}
if (-not $session.accessToken -or -not $session.expiresAt) {
    throw 'Resposta de login inesperada.'
}

$accountRequest = @{ name = 'Conta de teste'; currency = 'BRL' }
Invoke-Check -Method Post -Uri "$accounts/accounts" -ExpectedStatus 401 -Payload $accountRequest | Out-Null
$account = Invoke-Check -Method Post -Uri "$accounts/accounts" -ExpectedStatus 201 -Payload $accountRequest -Token $session.accessToken
if (-not $account.id -or $account.ownerId -ne $user.id) {
    throw 'Conta criada com proprietário inesperado.'
}
$ownedAccounts = @(Invoke-Check -Method Get -Uri "$accounts/accounts?page=0&size=1" -ExpectedStatus 200 -Token $session.accessToken)
if ($ownedAccounts.Count -ne 1 -or $ownedAccounts[0].id -ne $account.id) {
    throw 'Listagem de contas inesperada.'
}
$secondAccount = Invoke-Check -Method Post -Uri "$accounts/accounts" -ExpectedStatus 201 -Token $session.accessToken -Payload @{
    name = 'Poupança de teste'
    currency = 'BRL'
}
$nextAccounts = @(Invoke-Check -Method Get -Uri "$accounts/accounts?page=1&size=1" -ExpectedStatus 200 -Token $session.accessToken)
if ($nextAccounts.Count -ne 1 -or $nextAccounts[0].id -ne $secondAccount.id) {
    throw 'Paginação de contas inesperada.'
}
Invoke-Check -Method Get -Uri "$accounts/accounts?size=0" -ExpectedStatus 400 -Token $session.accessToken | Out-Null

$entry = Invoke-Check -Method Post -Uri "$transactions/transactions" -ExpectedStatus 201 -Token $session.accessToken -Payload @{
    accountId = $account.id
    type = 'EXPENSE'
    amount = 25.50
    description = 'Teste de integração'
}
if (-not $entry.id -or $entry.accountId -ne $account.id -or $entry.type -ne 'EXPENSE') {
    throw 'Lançamento criado com dados inesperados.'
}
$secondEntry = Invoke-Check -Method Post -Uri "$transactions/transactions" -ExpectedStatus 201 -Token $session.accessToken -Payload @{
    accountId = $account.id
    type = 'INCOME'
    amount = 10.00
    description = 'Segundo lançamento de teste'
}
$listedEntries = @(Invoke-Check -Method Get -Uri "$transactions/transactions?accountId=$($account.id)&page=0&size=1" -ExpectedStatus 200 -Token $session.accessToken)
if ($listedEntries.Count -ne 1 -or $listedEntries[0].id -ne $secondEntry.id) {
    throw 'Listagem de lançamentos inesperada.'
}
$nextEntries = @(Invoke-Check -Method Get -Uri "$transactions/transactions?accountId=$($account.id)&page=1&size=1" -ExpectedStatus 200 -Token $session.accessToken)
if ($nextEntries.Count -ne 1 -or $nextEntries[0].id -ne $entry.id) {
    throw 'Paginação de lançamentos inesperada.'
}
Invoke-Check -Method Get -Uri "$transactions/transactions?accountId=$($account.id)&size=0" -ExpectedStatus 400 -Token $session.accessToken | Out-Null

$otherEmail = "smoke+other-$suffix@prumo.test"
Invoke-Check -Method Post -Uri "$auth/users" -ExpectedStatus 201 -Payload @{
    email = $otherEmail
    password = $password
} | Out-Null
$otherSession = Invoke-Check -Method Post -Uri "$auth/sessions" -ExpectedStatus 200 -Payload @{
    email = $otherEmail
    password = $password
}
Invoke-Check -Method Get -Uri "$accounts/accounts/$($account.id)" -ExpectedStatus 404 -Token $otherSession.accessToken | Out-Null
$otherAccounts = @(Invoke-Check -Method Get -Uri "$accounts/accounts" -ExpectedStatus 200 -Token $otherSession.accessToken)
if ($otherAccounts.Count -ne 0) { throw 'Listagem de contas expôs dados de outro usuário.' }
Invoke-Check -Method Get -Uri "$transactions/transactions?accountId=$($account.id)" -ExpectedStatus 404 -Token $otherSession.accessToken | Out-Null
Invoke-Check -Method Post -Uri "$transactions/transactions" -ExpectedStatus 404 -Token $otherSession.accessToken -Payload @{
    accountId = $account.id
    type = 'EXPENSE'
    amount = 1.00
    description = 'Acesso negado'
} | Out-Null

Write-Output 'Fluxo completo, listagens e isolamento entre usuários: OK.'
