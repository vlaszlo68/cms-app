param(
    [ValidateSet("NonDestructive", "Destructive")]
    [string] $Mode = "NonDestructive",

    [string] $BaseUrl = "http://localhost:8080/cms-app",

    [string] $LoginName = "tester",

    [string] $Password = "pw",

    [switch] $ConfirmDestructive,

    [switch] $Strict,

    [int] $TimeoutSec = 10,

    [string] $DbHost = "localhost",

    [int] $DbPort = 5432,

    [string] $DbName = "cms_db",

    [string] $DbUser = "cms_user",

    [string] $DbPassword = "cms_pw"
)

$ErrorActionPreference = "Stop"

if ($Mode -eq "Destructive" -and -not $ConfirmDestructive) {
    throw "Destructive mode requires -ConfirmDestructive."
}

$BaseUrl = $BaseUrl.TrimEnd("/")
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$results = New-Object System.Collections.Generic.List[object]
$csrfToken = $null
$loginUserId = $null
$created = @{
    UserId = $null
    PageId = $null
    PublicPageId = $null
    PublicPageSlug = $null
    PageBlockId = $null
    MenuId = $null
    MenuItemId = $null
    TemplateId = $null
    MediaId = $null
}
$runId = "codex-api-test-" + (Get-Date -Format "yyyyMMddHHmmss")

function Add-Result {
    param(
        [string] $Name,
        [int] $Expected,
        [Nullable[int]] $Actual,
        [bool] $Passed,
        [string] $Detail = ""
    )

    $script:results.Add([pscustomobject]@{
        Name = $Name
        Expected = $Expected
        Actual = $Actual
        Passed = $Passed
        Detail = $Detail
    })
}

function Add-AssertionResult {
    param(
        [string] $Name,
        [bool] $Passed,
        [string] $Detail = ""
    )

    $actual = 0
    if ($Passed) {
        $actual = 1
    }

    Add-Result $Name 1 $actual $Passed $Detail
}

function Test-StrictCondition {
    param(
        [string] $Name,
        [bool] $Condition,
        [string] $FailureDetail
    )

    if (-not $script:Strict) {
        return
    }

    Add-AssertionResult $Name $Condition $FailureDetail
    if (-not $Condition) {
        throw "Strict assertion failed: $Name. $FailureDetail"
    }
}

function Test-SuccessEnvelope {
    param(
        [string] $Name,
        [object] $Response
    )

    Test-StrictCondition $Name ($null -ne $Response -and $true -eq $Response.success -and $null -ne $Response.data) "Expected success=true with data."
}

function Test-ErrorEnvelope {
    param(
        [string] $Name,
        [object] $Response
    )

    Test-StrictCondition $Name ($null -ne $Response -and $false -eq $Response.success -and $null -ne $Response.error) "Expected success=false with error."
}

function Test-ResponseId {
    param(
        [string] $Name,
        [object] $Response,
        [long] $ExpectedId
    )

    $actualId = $null
    if ($null -ne $Response -and $null -ne $Response.data -and $null -ne $Response.data.id) {
        $actualId = [long] $Response.data.id
    }

    Test-StrictCondition $Name ($null -ne $actualId -and $actualId -eq $ExpectedId) "Expected data.id $ExpectedId but got $actualId."
}

function Test-ListEnvelope {
    param(
        [string] $Name,
        [object] $Response
    )

    $isList = $false
    if ($null -ne $Response -and $null -ne $Response.data) {
        $isList = ($Response.data -is [System.Array]) -or ($Response.data -is [System.Collections.IEnumerable])
    }

    Test-StrictCondition $Name ($null -ne $Response -and $true -eq $Response.success -and $isList) "Expected success=true with list-like data."
}

function Get-PostgresDriverPath {
    $driverRoot = Join-Path $env:USERPROFILE ".m2\\repository\\org\\postgresql\\postgresql"
    $driver = Get-ChildItem -LiteralPath $driverRoot -Recurse -Filter "postgresql-*.jar" -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -notlike "*-sources.jar" } |
            Sort-Object FullName -Descending |
            Select-Object -First 1

    if ($null -eq $driver) {
        throw "PostgreSQL JDBC driver not found under $driverRoot. Permanent destructive-test cleanup cannot run."
    }

    return $driver.FullName
}

function Get-CleanupMarkerValue {
    param(
        [string] $Output,
        [string] $Name
    )

    $match = [regex]::Match($Output, "$Name=(\\d+)")
    if (-not $match.Success) {
        throw "Permanent cleanup did not report $Name. Output: $Output"
    }

    return [int] $match.Groups[1].Value
}

function Invoke-PermanentSoftDeleteCleanup {
    $userId = if ($null -eq $script:created.UserId) { -1 } else { [long] $script:created.UserId }
    $templateId = if ($null -eq $script:created.TemplateId) { -1 } else { [long] $script:created.TemplateId }

    if ($userId -lt 0 -and $templateId -lt 0) {
        return
    }

    $driverPath = Get-PostgresDriverPath
    $cleanupSource = @"
import java.sql.*;
Connection connection = DriverManager.getConnection("jdbc:postgresql://$script:DbHost`:$script:DbPort/$script:DbName", "$script:DbUser", "$script:DbPassword");
connection.setAutoCommit(false);
long userId = ${userId}L;
long templateId = ${templateId}L;
String marker = "$script:runId";
int deletedUser = 0;
int deletedTemplate = 0;
if (userId >= 0) {
    PreparedStatement statement = connection.prepareStatement("DELETE FROM users WHERE id = ? AND LOWER(username) LIKE ?");
    statement.setLong(1, userId);
    statement.setString(2, "%" + marker.toLowerCase() + "%");
    deletedUser = statement.executeUpdate();
    statement.close();
}
if (templateId >= 0) {
    PreparedStatement statement = connection.prepareStatement("DELETE FROM templates WHERE id = ? AND LOWER(name) LIKE ?");
    statement.setLong(1, templateId);
    statement.setString(2, "%" + marker.toLowerCase() + "%");
    deletedTemplate = statement.executeUpdate();
    statement.close();
}
PreparedStatement userCountStatement = connection.prepareStatement("SELECT COUNT(*) AS total FROM users WHERE id = ?");
userCountStatement.setLong(1, userId);
ResultSet userCountRows = userCountStatement.executeQuery();
userCountRows.next();
int remainingUser = userCountRows.getInt("total");
userCountRows.close();
userCountStatement.close();
PreparedStatement templateCountStatement = connection.prepareStatement("SELECT COUNT(*) AS total FROM templates WHERE id = ?");
templateCountStatement.setLong(1, templateId);
ResultSet templateCountRows = templateCountStatement.executeQuery();
templateCountRows.next();
int remainingTemplate = templateCountRows.getInt("total");
templateCountRows.close();
templateCountStatement.close();
if ((userId >= 0 && (deletedUser != 1 || remainingUser != 0)) || (templateId >= 0 && (deletedTemplate != 1 || remainingTemplate != 0))) {
    connection.rollback();
    connection.close();
    throw new IllegalStateException("Permanent cleanup did not remove only the current-run soft-deleted records.");
}
connection.commit();
connection.close();
System.out.println("PERMANENT_DELETED_USER=" + deletedUser);
System.out.println("PERMANENT_DELETED_TEMPLATE=" + deletedTemplate);
System.out.println("PERMANENT_REMAINING_USER=" + remainingUser);
System.out.println("PERMANENT_REMAINING_TEMPLATE=" + remainingTemplate);
/exit
"@
    $cleanupOutput = $cleanupSource | & jshell --class-path $driverPath -q 2>&1 | Out-String
    if ($LASTEXITCODE -ne 0 -or $cleanupOutput -match "Exception|ERROR:") {
        throw "Permanent cleanup failed. Output: $cleanupOutput"
    }

    $deletedUser = Get-CleanupMarkerValue $cleanupOutput "PERMANENT_DELETED_USER"
    $deletedTemplate = Get-CleanupMarkerValue $cleanupOutput "PERMANENT_DELETED_TEMPLATE"
    $remainingUser = Get-CleanupMarkerValue $cleanupOutput "PERMANENT_REMAINING_USER"
    $remainingTemplate = Get-CleanupMarkerValue $cleanupOutput "PERMANENT_REMAINING_TEMPLATE"

    if ($userId -ge 0) {
        Add-Result "Permanently delete test user" 1 $deletedUser ($deletedUser -eq 1) "Deleted only the current-run test user."
        Add-Result "Verify test user permanently removed" 0 $remainingUser ($remainingUser -eq 0) "Expected no row for the current-run test user."
    }
    if ($templateId -ge 0) {
        Add-Result "Permanently delete test template" 1 $deletedTemplate ($deletedTemplate -eq 1) "Deleted only the current-run test template."
        Add-Result "Verify test template permanently removed" 0 $remainingTemplate ($remainingTemplate -eq 0) "Expected no row for the current-run test template."
    }
}

function ConvertTo-JsonBody {
    param([object] $Value)

    return ($Value | ConvertTo-Json -Depth 10)
}

function Read-ErrorBody {
    param(
        [object] $ErrorRecord,
        [object] $Response
    )

    $hasErrorDetails = $null -ne $ErrorRecord -and
            $null -ne $ErrorRecord.ErrorDetails -and
            -not [string]::IsNullOrWhiteSpace($ErrorRecord.ErrorDetails.Message)
    if ($hasErrorDetails) {
        return [string] $ErrorRecord.ErrorDetails.Message
    }

    if ($null -eq $Response) {
        return ""
    }

    try {
        $reader = [System.IO.StreamReader]::new($Response.GetResponseStream())
        return $reader.ReadToEnd()
    } catch {
        return ""
    }
}

function Invoke-CmsApi {
    param(
        [string] $Name,
        [string] $Method,
        [string] $Path,
        [int] $ExpectedStatus,
        [object] $Body = $null,
        [hashtable] $Headers = @{},
        [string] $ContentType = "application/json",
        [byte[]] $RawBody = $null
    )

    $uri = $script:BaseUrl + $Path
    $actualStatus = $null
    $content = ""
    $response = $null

    try {
        $params = @{
            Uri = $uri
            Method = $Method
            WebSession = $script:session
            Headers = $Headers
            UseBasicParsing = $true
            TimeoutSec = $script:TimeoutSec
        }

        if ($null -ne $RawBody) {
            $params.Body = $RawBody
            $params.ContentType = $ContentType
        } elseif ($null -ne $Body) {
            $params.Body = ConvertTo-JsonBody $Body
            $params.ContentType = $ContentType
        }

        $response = Invoke-WebRequest @params
        $actualStatus = [int] $response.StatusCode
        $content = [string] $response.Content
    } catch {
        $response = $_.Exception.Response
        if ($null -ne $response) {
            $actualStatus = [int] $response.StatusCode
            $content = Read-ErrorBody $_ $response
        } else {
            Add-Result $Name $ExpectedStatus $null $false $_.Exception.Message
            return $null
        }
    }

    $passed = ($actualStatus -eq $ExpectedStatus)
    $detail = ""
    if (-not $passed) {
        $detail = $content
    }
    Add-Result $Name $ExpectedStatus $actualStatus $passed $detail

    if ([string]::IsNullOrWhiteSpace($content)) {
        return $null
    }

    try {
        return $content | ConvertFrom-Json
    } catch {
        return $content
    }
}

function Get-ResultCategory {
    param([object] $Result)

    $name = [string] $Result.Name
    if ($name -match "Script error|Cleanup error|Logout error") {
        return "Errors"
    }
    if ($name -match "Strict verify|absent after delete|deactivated|delete template inactive|Delete ") {
        return "Cleanup"
    }
    if ($name -like "Strict *") {
        return "Strict assertions"
    }
    if ($name -match "Health|Auth config|/me|Login|Logout") {
        return "Auth/session"
    }
    if ($name -match "^Read pages$|^Read templates$|^Read menus$|^Read media$|^Read site settings$") {
        return "Admin reads"
    }
    if ($name -match "Create |Read user|Read template|Read page|Read page block|Read menu|Read media item|Read media content|Update |Delete ") {
        return "CRUD"
    }
    return "Other"
}

function Get-ActualResultLabel {
    param([object] $Result)

    if ($null -eq $Result.Actual) {
        return "No response"
    }
    if ($Result.Name -like "Strict *" -and $Result.Expected -eq 1 -and $Result.Actual -eq 1) {
        return "Assertion passed"
    }
    if ($Result.Name -like "Strict *" -and $Result.Expected -eq 1 -and $Result.Actual -eq 0) {
        return "Assertion failed"
    }
    return [string] $Result.Actual
}

function New-StatsRow {
    param(
        [string] $Name,
        [int] $Total,
        [int] $Passed,
        [int] $Failed
    )

    $passRate = 0
    if ($Total -gt 0) {
        $passRate = [Math]::Round(($Passed / $Total) * 100, 2)
    }

    return [pscustomobject]@{
        Name = $Name
        Total = $Total
        Passed = $Passed
        Failed = $Failed
        PassRatePercent = $passRate
    }
}

function Write-TestStatistics {
    $all = @($script:results.ToArray())
    $failed = @($all | Where-Object { -not $_.Passed })
    $passed = $all.Count - $failed.Count
    $cleanupStatus = "Skipped"
    if ($script:Mode -eq "Destructive") {
        if (@($all | Where-Object { $_.Name -match "Cleanup error" }).Count -gt 0) {
            $cleanupStatus = "Failed"
        } elseif (@($all | Where-Object { $_.Name -match "^Delete " }).Count -gt 0) {
            $cleanupStatus = "Run"
        } else {
            $cleanupStatus = "Not reached"
        }
    }

    ""
    "Detailed API Test Statistics"
    [pscustomobject]@{
        Mode = $script:Mode
        Strict = [bool] $script:Strict
        RunId = $script:runId
        Total = $all.Count
        Passed = $passed
        Failed = $failed.Count
        PassRatePercent = if ($all.Count -gt 0) { [Math]::Round(($passed / $all.Count) * 100, 2) } else { 0 }
        CleanupStatus = $cleanupStatus
    } | Format-Table -AutoSize

    "By category"
    $all |
            Group-Object { Get-ResultCategory $_ } |
            Sort-Object Name |
            ForEach-Object {
                $items = @($_.Group)
                $itemFailures = @($items | Where-Object { -not $_.Passed })
                New-StatsRow $_.Name $items.Count ($items.Count - $itemFailures.Count) $itemFailures.Count
            } |
            Format-Table -AutoSize

    "By actual status or assertion result"
    $all |
            Group-Object { Get-ActualResultLabel $_ } |
            Sort-Object Name |
            ForEach-Object {
                $items = @($_.Group)
                $itemFailures = @($items | Where-Object { -not $_.Passed })
                New-StatsRow $_.Name $items.Count ($items.Count - $itemFailures.Count) $itemFailures.Count
            } |
            Format-Table -AutoSize

    if ($failed.Count -gt 0) {
        "Failures"
        $failed | Select-Object Name, Expected, Actual, Detail | Format-Table -AutoSize
    }
}

function Invoke-CmsMultipartUpload {
    param(
        [string] $Name,
        [string] $Path,
        [int] $ExpectedStatus,
        [string] $FileName,
        [string] $FileContent,
        [string] $Description,
        [hashtable] $Headers
    )

    $boundary = "----CodexApiTest" + [Guid]::NewGuid().ToString("N")
    $bodyText = "--$boundary`r`n" +
            "Content-Disposition: form-data; name=`"description`"`r`n`r`n" +
            "$Description`r`n" +
            "--$boundary`r`n" +
            "Content-Disposition: form-data; name=`"file`"; filename=`"$FileName`"`r`n" +
            "Content-Type: text/plain`r`n`r`n" +
            "$FileContent`r`n" +
            "--$boundary--`r`n"
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($bodyText)
    return Invoke-CmsApi $Name "POST" $Path $ExpectedStatus $null $Headers "multipart/form-data; boundary=$boundary" $bytes
}

function Get-DataId {
    param([object] $Response)

    if ($null -eq $Response -or $null -eq $Response.data -or $null -eq $Response.data.id) {
        throw "Expected response data.id was missing."
    }
    return [long] $Response.data.id
}

function Get-CsrfHeaders {
    if ([string]::IsNullOrWhiteSpace($script:csrfToken)) {
        throw "CSRF token is missing."
    }
    return @{ "X-CSRF-Token" = $script:csrfToken }
}

function Assert-NotLoginUser {
    param(
        [Nullable[long]] $UserId,
        [string] $Operation
    )

    if ($null -ne $UserId -and $null -ne $script:loginUserId -and $UserId -eq $script:loginUserId) {
        throw "Refusing to $Operation the login user. The login user is only for authentication."
    }
}

function Test-NonDestructive {
    $health = Invoke-CmsApi "Health" "GET" "/hello" 200
    $healthText = $health | ConvertTo-Json -Depth 5 -Compress
    Test-StrictCondition "Strict health body" ($healthText.Contains("Hello CMS")) "Expected body to contain Hello CMS."

    $config = Invoke-CmsApi "Auth config" "GET" "/api/auth/config" 200
    Test-SuccessEnvelope "Strict auth config envelope" $config
    Test-StrictCondition "Strict auth config fields" ($null -ne $config.data.loginCaptchaEnabled -and $null -ne $config.data.registrationCaptchaEnabled -and $null -ne $config.data.passwordPolicy) "Expected CAPTCHA flags and passwordPolicy."

    $publicSettings = Invoke-CmsApi "Read public site settings" "GET" "/api/public/site-settings" 200
    Test-SuccessEnvelope "Strict public site settings envelope" $publicSettings
    $publicSettingsFields = @($publicSettings.data.PSObject.Properties.Name)
    $expectedPublicSettingsFields = @("siteName", "logoMediaId", "footerText", "contactEmail", "phone", "facebookUrl", "linkedinUrl")
    $hasExpectedPublicSettingsFields = $publicSettingsFields.Count -eq $expectedPublicSettingsFields.Count -and
            @($expectedPublicSettingsFields | Where-Object { $_ -notin $publicSettingsFields }).Count -eq 0
    Test-StrictCondition "Strict public site settings fields" $hasExpectedPublicSettingsFields "Expected the seven public site settings fields."

    foreach ($publicMenuCode in @("MAIN", "FOOTER")) {
        $publicMenu = Invoke-CmsApi "Read public menu $publicMenuCode" "GET" "/api/public/menus/$publicMenuCode" 200
        Test-ListEnvelope "Strict public menu $publicMenuCode envelope" $publicMenu
        $validPublicMenuItems = @($publicMenu.data | Where-Object {
                $null -ne $_.id -and -not [string]::IsNullOrWhiteSpace([string] $_.title) -and
                $null -ne $_.targetType -and $null -ne $_.children -and
                (($_.targetType -eq "PAGE" -and $null -ne $_.pageId -and
                        -not [string]::IsNullOrWhiteSpace([string] $_.pageSlug) -and
                        -not [string]::IsNullOrWhiteSpace([string] $_.path)) -or
                 ($_.targetType -eq "URL" -and -not [string]::IsNullOrWhiteSpace([string] $_.targetUrl)))
            }).Count -eq @($publicMenu.data).Count
        Test-StrictCondition "Strict public menu $publicMenuCode items" $validPublicMenuItems "Expected public menu target fields and children."
    }

    $unauthenticatedMe = Invoke-CmsApi "Unauthenticated /me" "GET" "/api/auth/me" 401
    Test-ErrorEnvelope "Strict unauthenticated /me envelope" $unauthenticatedMe

    if ($null -ne $config -and $true -eq $config.data.loginCaptchaEnabled) {
        throw "Login CAPTCHA is enabled. Start local runtime with CAPTCHA disabled or add CAPTCHA solving support before login tests."
    }

    $loginBody = @{
        loginName = $script:LoginName
        password = $script:Password
        captchaHoneypot = ""
    }
    $login = Invoke-CmsApi "Login" "POST" "/api/auth/login" 200 $loginBody
    if ($null -eq $login -or $null -eq $login.data.csrfToken) {
        throw "Login succeeded status check did not return data.csrfToken."
    }
    Test-SuccessEnvelope "Strict login envelope" $login
    Test-StrictCondition "Strict login identity" ($login.data.loginName -eq $script:LoginName -and $login.data.role -eq "ADMIN" -and -not [string]::IsNullOrWhiteSpace([string] $login.data.csrfToken)) "Expected matching admin login with csrfToken."

    $script:csrfToken = [string] $login.data.csrfToken
    if ($null -ne $login.data.id) {
        $script:loginUserId = [long] $login.data.id
    }
    $me = Invoke-CmsApi "Authenticated /me" "GET" "/api/auth/me" 200
    Test-SuccessEnvelope "Strict authenticated /me envelope" $me
    Test-StrictCondition "Strict authenticated /me identity" ($me.data.loginName -eq $script:LoginName -and $me.data.role -eq "ADMIN" -and -not [string]::IsNullOrWhiteSpace([string] $me.data.csrfToken)) "Expected matching admin session with csrfToken."

    $pages = Invoke-CmsApi "Read pages" "GET" "/api/pages" 200
    Test-ListEnvelope "Strict pages list envelope" $pages
    $templates = Invoke-CmsApi "Read templates" "GET" "/api/templates" 200
    Test-ListEnvelope "Strict templates list envelope" $templates
    $menus = Invoke-CmsApi "Read menus" "GET" "/api/menus" 200
    Test-ListEnvelope "Strict menus list envelope" $menus
    $media = Invoke-CmsApi "Read media" "GET" "/api/media" 200
    Test-ListEnvelope "Strict media list envelope" $media
    $siteSettings = Invoke-CmsApi "Read site settings" "GET" "/api/site-settings" 200
    Test-SuccessEnvelope "Strict site settings envelope" $siteSettings
}

function Test-Destructive {
    $headers = Get-CsrfHeaders

    $userBody = @{
        loginName = "$script:runId-user"
        userName = "$script:runId User"
        emailAddress = "$script:runId@example.com"
        password = "Password123!"
        role = "USER"
        active = $true
        registrationStatus = "COMPLETED"
    }
    $user = Invoke-CmsApi "Create user" "POST" "/api/users" 201 $userBody $headers
    $script:created.UserId = Get-DataId $user
    Test-SuccessEnvelope "Strict create user envelope" $user
    Assert-NotLoginUser $script:created.UserId "update or delete"
    $readUser = Invoke-CmsApi "Read user" "GET" "/api/users/$($script:created.UserId)" 200
    Test-ResponseId "Strict read user id" $readUser $script:created.UserId
    $userBody.userName = "$script:runId User Updated"
    Assert-NotLoginUser $script:created.UserId "update"
    $updatedUser = Invoke-CmsApi "Update user" "PUT" "/api/users/$($script:created.UserId)" 200 $userBody $headers
    Test-ResponseId "Strict update user id" $updatedUser $script:created.UserId
    Test-StrictCondition "Strict update user value" ($updatedUser.data.userName -eq $userBody.userName) "Expected updated userName."

    $templateBody = @{
        code = ($script:runId + "-template").ToUpperInvariant().Replace("-", "_")
        name = "$script:runId template"
        description = "Temporary API test template"
        previewImageMediaId = $null
        active = $true
    }
    $template = Invoke-CmsApi "Create template" "POST" "/api/templates" 201 $templateBody $headers
    $script:created.TemplateId = Get-DataId $template
    Test-SuccessEnvelope "Strict create template envelope" $template
    $readTemplate = Invoke-CmsApi "Read template" "GET" "/api/templates/$($script:created.TemplateId)" 200
    Test-ResponseId "Strict read template id" $readTemplate $script:created.TemplateId
    $templateBody.name = "$script:runId template updated"
    $updatedTemplate = Invoke-CmsApi "Update template" "PUT" "/api/templates/$($script:created.TemplateId)" 200 $templateBody $headers
    Test-ResponseId "Strict update template id" $updatedTemplate $script:created.TemplateId
    Test-StrictCondition "Strict update template value" ($updatedTemplate.data.name -eq $templateBody.name) "Expected updated template name."

    $pageBody = @{
        title = "$script:runId page"
        slug = "$script:runId-page"
        content = $null
        pageType = "BLOCK"
        status = "DRAFT"
        metaTitle = "$script:runId page"
        metaDescription = "Temporary API test page"
        homepage = $false
        menuVisible = $false
        templateId = $null
    }
    $page = Invoke-CmsApi "Create page" "POST" "/api/pages" 201 $pageBody $headers
    $script:created.PageId = Get-DataId $page
    Test-SuccessEnvelope "Strict create page envelope" $page
    $readPage = Invoke-CmsApi "Read page" "GET" "/api/pages/$($script:created.PageId)" 200
    Test-ResponseId "Strict read page id" $readPage $script:created.PageId
    $pageBody.title = "$script:runId page updated"
    $updatedPage = Invoke-CmsApi "Update page" "PUT" "/api/pages/$($script:created.PageId)" 200 $pageBody $headers
    Test-ResponseId "Strict update page id" $updatedPage $script:created.PageId
    Test-StrictCondition "Strict update page value" ($updatedPage.data.title -eq $pageBody.title) "Expected updated page title."

    $publicPageBody = @{
        title = "$script:runId public page"
        slug = "$script:runId-public-page"
        content = "Temporary public API test page"
        pageType = "CONTENT"
        status = "PUBLISHED"
        metaTitle = $null
        metaDescription = $null
        homepage = $false
        menuVisible = $false
        templateId = $null
    }
    $publicPage = Invoke-CmsApi "Create public page fixture" "POST" "/api/pages" 201 $publicPageBody $headers
    $script:created.PublicPageId = Get-DataId $publicPage
    $script:created.PublicPageSlug = $publicPageBody.slug
    Test-SuccessEnvelope "Strict create public page fixture envelope" $publicPage
    $readPublicPage = Invoke-CmsApi "Read public page" "GET" "/api/public/pages/$($script:created.PublicPageSlug)" 200
    Test-SuccessEnvelope "Strict public page envelope" $readPublicPage
    $publicPageFields = @($readPublicPage.data.PSObject.Properties.Name)
    $expectedPublicPageFields = @("id", "title", "slug", "pageType", "templateCode", "content")
    $hasExpectedPublicPageFields = $publicPageFields.Count -eq $expectedPublicPageFields.Count -and
            @($expectedPublicPageFields | Where-Object { $_ -notin $publicPageFields }).Count -eq 0
    Test-StrictCondition "Strict public page fields" ($hasExpectedPublicPageFields -and
            $readPublicPage.data.slug -eq $publicPageBody.slug -and
            $readPublicPage.data.pageType -eq "CONTENT" -and
            $readPublicPage.data.content -eq $publicPageBody.content) "Expected the limited published CONTENT page response."

    $blockBody = @{
        pageId = $script:created.PageId
        blockType = "TEXT"
        title = "$script:runId block"
        sortOrder = 1
        visible = $true
        configJson = "{`"text`":`"Temporary API test block`"}"
    }
    $block = Invoke-CmsApi "Create page block" "POST" "/api/page-blocks" 201 $blockBody $headers
    $script:created.PageBlockId = Get-DataId $block
    Test-SuccessEnvelope "Strict create page block envelope" $block
    $readBlock = Invoke-CmsApi "Read page block" "GET" "/api/page-blocks/$($script:created.PageBlockId)" 200
    Test-ResponseId "Strict read page block id" $readBlock $script:created.PageBlockId
    $blockBody.title = "$script:runId block updated"
    $updatedBlock = Invoke-CmsApi "Update page block" "PUT" "/api/page-blocks/$($script:created.PageBlockId)" 200 $blockBody $headers
    Test-ResponseId "Strict update page block id" $updatedBlock $script:created.PageBlockId
    Test-StrictCondition "Strict update page block value" ($updatedBlock.data.title -eq $blockBody.title) "Expected updated page block title."

    $menuBody = @{
        name = "$script:runId menu"
        code = ($script:runId + "-menu").ToUpperInvariant().Replace("-", "_")
        active = $true
    }
    $menu = Invoke-CmsApi "Create menu" "POST" "/api/menus" 201 $menuBody $headers
    $script:created.MenuId = Get-DataId $menu
    Test-SuccessEnvelope "Strict create menu envelope" $menu
    $readMenu = Invoke-CmsApi "Read menu" "GET" "/api/menus/$($script:created.MenuId)" 200
    Test-ResponseId "Strict read menu id" $readMenu $script:created.MenuId
    $menuBody.name = "$script:runId menu updated"
    $updatedMenu = Invoke-CmsApi "Update menu" "PUT" "/api/menus/$($script:created.MenuId)" 200 $menuBody $headers
    Test-ResponseId "Strict update menu id" $updatedMenu $script:created.MenuId
    Test-StrictCondition "Strict update menu value" ($updatedMenu.data.name -eq $menuBody.name) "Expected updated menu name."

    $menuItemBody = @{
        menuId = $script:created.MenuId
        parentId = $null
        pageId = $null
        targetType = "URL"
        targetUrl = "https://example.com/$script:runId"
        title = "$script:runId link"
        sortOrder = 1
        visible = $true
    }
    $menuItem = Invoke-CmsApi "Create menu item" "POST" "/api/menu-items" 201 $menuItemBody $headers
    $script:created.MenuItemId = Get-DataId $menuItem
    Test-SuccessEnvelope "Strict create menu item envelope" $menuItem
    $readMenuItems = Invoke-CmsApi "Read menu items" "GET" "/api/menus/$($script:created.MenuId)/items" 200
    Test-ListEnvelope "Strict read menu items envelope" $readMenuItems
    $createdMenuItemId = $script:created.MenuItemId
    $hasMenuItem = @($readMenuItems.data | Where-Object { [long] $_.id -eq $createdMenuItemId }).Count -gt 0
    Test-StrictCondition "Strict read menu item presence" $hasMenuItem "Expected created menu item in menu item list."
    $menuItemBody.title = "$script:runId link updated"
    $updatedMenuItem = Invoke-CmsApi "Update menu item" "PUT" "/api/menu-items/$($script:created.MenuItemId)" 200 $menuItemBody $headers
    Test-ResponseId "Strict update menu item id" $updatedMenuItem $script:created.MenuItemId
    Test-StrictCondition "Strict update menu item value" ($updatedMenuItem.data.title -eq $menuItemBody.title) "Expected updated menu item title."

    $media = Invoke-CmsMultipartUpload "Create media" "/api/media" 201 "$script:runId.txt" "Temporary CMS API test media." "$script:runId media" $headers
    $script:created.MediaId = Get-DataId $media
    Test-SuccessEnvelope "Strict create media envelope" $media
    $readMedia = Invoke-CmsApi "Read media item" "GET" "/api/media/$($script:created.MediaId)" 200
    Test-ResponseId "Strict read media id" $readMedia $script:created.MediaId
    $mediaContent = Invoke-CmsApi "Read media content" "GET" "/api/media/$($script:created.MediaId)/content" 200
    Test-StrictCondition "Strict media content body" (-not [string]::IsNullOrWhiteSpace([string] $mediaContent)) "Expected non-empty media content body."

}

function Cleanup-Destructive {
    $headers = Get-CsrfHeaders

    if ($null -ne $script:created.MediaId) {
        $deletedId = $script:created.MediaId
        Invoke-CmsApi "Delete media" "DELETE" "/api/media/$($script:created.MediaId)" 200 $null $headers | Out-Null
        if ($script:Strict) {
            Invoke-CmsApi "Strict verify media deleted" "GET" "/api/media/$deletedId" 404 | Out-Null
        }
    }
    if ($null -ne $script:created.MenuItemId) {
        $deletedId = $script:created.MenuItemId
        $menuId = $script:created.MenuId
        Invoke-CmsApi "Delete menu item" "DELETE" "/api/menu-items/$($script:created.MenuItemId)" 200 $null $headers | Out-Null
        if ($script:Strict -and $null -ne $menuId) {
            $remainingItems = Invoke-CmsApi "Strict verify menu item deleted" "GET" "/api/menus/$menuId/items" 200
            $stillPresent = @($remainingItems.data | Where-Object { [long] $_.id -eq $deletedId }).Count -gt 0
            Test-StrictCondition "Strict menu item absent after delete" (-not $stillPresent) "Expected deleted menu item to be absent from menu item list."
        }
    }
    if ($null -ne $script:created.MenuId) {
        $deletedId = $script:created.MenuId
        Invoke-CmsApi "Delete menu" "DELETE" "/api/menus/$($script:created.MenuId)" 200 $null $headers | Out-Null
        if ($script:Strict) {
            Invoke-CmsApi "Strict verify menu deleted" "GET" "/api/menus/$deletedId" 404 | Out-Null
        }
    }
    if ($null -ne $script:created.PageBlockId) {
        $deletedId = $script:created.PageBlockId
        Invoke-CmsApi "Delete page block" "DELETE" "/api/page-blocks/$($script:created.PageBlockId)" 200 $null $headers | Out-Null
        if ($script:Strict) {
            Invoke-CmsApi "Strict verify page block deleted" "GET" "/api/page-blocks/$deletedId" 404 | Out-Null
        }
    }
    if ($null -ne $script:created.PageId) {
        $deletedId = $script:created.PageId
        Invoke-CmsApi "Delete page" "DELETE" "/api/pages/$($script:created.PageId)" 200 $null $headers | Out-Null
        if ($script:Strict) {
            Invoke-CmsApi "Strict verify page deleted" "GET" "/api/pages/$deletedId" 404 | Out-Null
        }
    }
    if ($null -ne $script:created.PublicPageId) {
        $deletedSlug = $script:created.PublicPageSlug
        Invoke-CmsApi "Delete public page fixture" "DELETE" "/api/pages/$($script:created.PublicPageId)" 200 $null $headers | Out-Null
        if ($script:Strict) {
            Invoke-CmsApi "Strict verify public page deleted" "GET" "/api/public/pages/$deletedSlug" 404 | Out-Null
        }
    }
    if ($null -ne $script:created.TemplateId) {
        $deletedId = $script:created.TemplateId
        $deletedTemplate = Invoke-CmsApi "Delete template" "DELETE" "/api/templates/$($script:created.TemplateId)" 200 $null $headers
        if ($script:Strict) {
            Test-ResponseId "Strict delete template id" $deletedTemplate $deletedId
            Test-StrictCondition "Strict delete template inactive" ($false -eq $deletedTemplate.data.active) "Expected deleted template to be inactive."
            $readDeletedTemplate = Invoke-CmsApi "Strict verify template deactivated" "GET" "/api/templates/$deletedId" 200
            Test-ResponseId "Strict deactivated template id" $readDeletedTemplate $deletedId
            Test-StrictCondition "Strict deactivated template inactive" ($false -eq $readDeletedTemplate.data.active) "Expected deactivated template to remain inactive."
        }
    }
    if ($null -ne $script:created.UserId) {
        $deletedId = $script:created.UserId
        Assert-NotLoginUser $script:created.UserId "delete"
        $deletedUser = Invoke-CmsApi "Delete user" "DELETE" "/api/users/$($script:created.UserId)" 200 $null $headers
        if ($script:Strict) {
            Test-ResponseId "Strict delete user id" $deletedUser $deletedId
            Test-StrictCondition "Strict delete user inactive" ($false -eq $deletedUser.data.active) "Expected deleted user to be inactive."
            $readDeletedUser = Invoke-CmsApi "Strict verify user deactivated" "GET" "/api/users/$deletedId" 200
            Test-ResponseId "Strict deactivated user id" $readDeletedUser $deletedId
            Test-StrictCondition "Strict deactivated user inactive" ($false -eq $readDeletedUser.data.active) "Expected deactivated user to remain inactive."
        }
    }

}

$hadFailure = $false

try {
    Test-NonDestructive
    if ($Mode -eq "Destructive") {
        Test-Destructive
    }
} catch {
    $hadFailure = $true
    Add-Result "Script error" 0 $null $false $_.Exception.Message
} finally {
    if ($Mode -eq "Destructive" -and -not [string]::IsNullOrWhiteSpace($csrfToken)) {
        try {
            Cleanup-Destructive
        } catch {
            $hadFailure = $true
            Add-Result "Cleanup error" 0 $null $false $_.Exception.Message
        }
        try {
            Invoke-PermanentSoftDeleteCleanup
        } catch {
            $hadFailure = $true
            Add-Result "Permanent cleanup error" 0 $null $false $_.Exception.Message
        }
    }

    if (-not [string]::IsNullOrWhiteSpace($csrfToken)) {
        try {
            $logout = Invoke-CmsApi "Logout" "POST" "/api/auth/logout" 200 $null (Get-CsrfHeaders)
            Test-SuccessEnvelope "Strict logout envelope" $logout
            Test-StrictCondition "Strict logout message" ($logout.data.message -eq "Logged out") "Expected logout message."
        } catch {
            $hadFailure = $true
            Add-Result "Logout error" 0 $null $false $_.Exception.Message
        }
    }
}

$results | Format-Table -AutoSize
Write-TestStatistics

$failed = @($results | Where-Object { -not $_.Passed })
if ($failed.Count -gt 0 -or $hadFailure) {
    exit 1
}

exit 0
