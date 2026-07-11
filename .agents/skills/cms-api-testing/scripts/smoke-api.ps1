param(
    [ValidateSet("NonDestructive", "Destructive")]
    [string] $Mode = "NonDestructive",

    [string] $BaseUrl = "http://localhost:8080/cms-app",

    [string] $LoginName = "tester",

    [string] $Password = "pw",

    [switch] $ConfirmDestructive,

    [int] $TimeoutSec = 10
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

function ConvertTo-JsonBody {
    param([object] $Value)

    return ($Value | ConvertTo-Json -Depth 10)
}

function Read-ErrorBody {
    param([object] $Response)

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
            $content = Read-ErrorBody $response
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
    Invoke-CmsApi "Health" "GET" "/hello" 200 | Out-Null
    $config = Invoke-CmsApi "Auth config" "GET" "/api/auth/config" 200
    Invoke-CmsApi "Unauthenticated /me" "GET" "/api/auth/me" 401 | Out-Null

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

    $script:csrfToken = [string] $login.data.csrfToken
    if ($null -ne $login.data.id) {
        $script:loginUserId = [long] $login.data.id
    }
    Invoke-CmsApi "Authenticated /me" "GET" "/api/auth/me" 200 | Out-Null
    Invoke-CmsApi "Read pages" "GET" "/api/pages" 200 | Out-Null
    Invoke-CmsApi "Read templates" "GET" "/api/templates" 200 | Out-Null
    Invoke-CmsApi "Read menus" "GET" "/api/menus" 200 | Out-Null
    Invoke-CmsApi "Read media" "GET" "/api/media" 200 | Out-Null
    Invoke-CmsApi "Read site settings" "GET" "/api/site-settings" 200 | Out-Null
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
    Assert-NotLoginUser $script:created.UserId "update or delete"
    Invoke-CmsApi "Read user" "GET" "/api/users/$($script:created.UserId)" 200 | Out-Null
    $userBody.userName = "$script:runId User Updated"
    Assert-NotLoginUser $script:created.UserId "update"
    Invoke-CmsApi "Update user" "PUT" "/api/users/$($script:created.UserId)" 200 $userBody $headers | Out-Null

    $templateBody = @{
        code = ($script:runId + "-template").ToUpperInvariant().Replace("-", "_")
        name = "$script:runId template"
        description = "Temporary API test template"
        previewImageMediaId = $null
        active = $true
    }
    $template = Invoke-CmsApi "Create template" "POST" "/api/templates" 201 $templateBody $headers
    $script:created.TemplateId = Get-DataId $template
    Invoke-CmsApi "Read template" "GET" "/api/templates/$($script:created.TemplateId)" 200 | Out-Null
    $templateBody.name = "$script:runId template updated"
    Invoke-CmsApi "Update template" "PUT" "/api/templates/$($script:created.TemplateId)" 200 $templateBody $headers | Out-Null

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
    Invoke-CmsApi "Read page" "GET" "/api/pages/$($script:created.PageId)" 200 | Out-Null
    $pageBody.title = "$script:runId page updated"
    Invoke-CmsApi "Update page" "PUT" "/api/pages/$($script:created.PageId)" 200 $pageBody $headers | Out-Null

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
    Invoke-CmsApi "Read page block" "GET" "/api/page-blocks/$($script:created.PageBlockId)" 200 | Out-Null
    $blockBody.title = "$script:runId block updated"
    Invoke-CmsApi "Update page block" "PUT" "/api/page-blocks/$($script:created.PageBlockId)" 200 $blockBody $headers | Out-Null

    $menuBody = @{
        name = "$script:runId menu"
        code = ($script:runId + "-menu").ToUpperInvariant().Replace("-", "_")
        active = $true
    }
    $menu = Invoke-CmsApi "Create menu" "POST" "/api/menus" 201 $menuBody $headers
    $script:created.MenuId = Get-DataId $menu
    Invoke-CmsApi "Read menu" "GET" "/api/menus/$($script:created.MenuId)" 200 | Out-Null
    $menuBody.name = "$script:runId menu updated"
    Invoke-CmsApi "Update menu" "PUT" "/api/menus/$($script:created.MenuId)" 200 $menuBody $headers | Out-Null

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
    Invoke-CmsApi "Read menu items" "GET" "/api/menus/$($script:created.MenuId)/items" 200 | Out-Null
    $menuItemBody.title = "$script:runId link updated"
    Invoke-CmsApi "Update menu item" "PUT" "/api/menu-items/$($script:created.MenuItemId)" 200 $menuItemBody $headers | Out-Null

    $media = Invoke-CmsMultipartUpload "Create media" "/api/media" 201 "$script:runId.txt" "Temporary CMS API test media." "$script:runId media" $headers
    $script:created.MediaId = Get-DataId $media
    Invoke-CmsApi "Read media item" "GET" "/api/media/$($script:created.MediaId)" 200 | Out-Null
    Invoke-CmsApi "Read media content" "GET" "/api/media/$($script:created.MediaId)/content" 200 | Out-Null

}

function Cleanup-Destructive {
    $headers = Get-CsrfHeaders

    if ($null -ne $script:created.MediaId) {
        Invoke-CmsApi "Delete media" "DELETE" "/api/media/$($script:created.MediaId)" 200 $null $headers | Out-Null
    }
    if ($null -ne $script:created.MenuItemId) {
        Invoke-CmsApi "Delete menu item" "DELETE" "/api/menu-items/$($script:created.MenuItemId)" 200 $null $headers | Out-Null
    }
    if ($null -ne $script:created.MenuId) {
        Invoke-CmsApi "Delete menu" "DELETE" "/api/menus/$($script:created.MenuId)" 200 $null $headers | Out-Null
    }
    if ($null -ne $script:created.PageBlockId) {
        Invoke-CmsApi "Delete page block" "DELETE" "/api/page-blocks/$($script:created.PageBlockId)" 200 $null $headers | Out-Null
    }
    if ($null -ne $script:created.PageId) {
        Invoke-CmsApi "Delete page" "DELETE" "/api/pages/$($script:created.PageId)" 200 $null $headers | Out-Null
    }
    if ($null -ne $script:created.TemplateId) {
        Invoke-CmsApi "Delete template" "DELETE" "/api/templates/$($script:created.TemplateId)" 200 $null $headers | Out-Null
    }
    if ($null -ne $script:created.UserId) {
        Assert-NotLoginUser $script:created.UserId "delete"
        Invoke-CmsApi "Delete user" "DELETE" "/api/users/$($script:created.UserId)" 200 $null $headers | Out-Null
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
    }

    if (-not [string]::IsNullOrWhiteSpace($csrfToken)) {
        try {
            Invoke-CmsApi "Logout" "POST" "/api/auth/logout" 200 $null (Get-CsrfHeaders) | Out-Null
        } catch {
            $hadFailure = $true
            Add-Result "Logout error" 0 $null $false $_.Exception.Message
        }
    }
}

$results | Format-Table -AutoSize

$failed = @($results | Where-Object { -not $_.Passed })
if ($failed.Count -gt 0 -or $hadFailure) {
    exit 1
}

exit 0
