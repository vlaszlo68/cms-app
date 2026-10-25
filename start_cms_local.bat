@echo off
setlocal

rem Build and start the CMS in standalone local Tomcat with local PostgreSQL.
set "PROJECT_ROOT=%~dp0"
set "TOMCAT_HOME=C:\Tomcat 9.0"

if not exist "%TOMCAT_HOME%\bin\catalina.bat" set "TOMCAT_HOME=C:\tomcat9"
if not exist "%TOMCAT_HOME%\bin\catalina.bat" (
    echo Tomcat 9 was not found in C:\Tomcat 9.0 or C:\tomcat9.
    exit /b 1
)

pushd "%PROJECT_ROOT%" || exit /b 1
call mvn package -DskipTests
if errorlevel 1 (
    popd
    exit /b 1
)

if exist "%TOMCAT_HOME%\webapps\cms-app" rmdir /s /q "%TOMCAT_HOME%\webapps\cms-app"
if exist "%TOMCAT_HOME%\webapps\cms-app.war" del /q "%TOMCAT_HOME%\webapps\cms-app.war"
copy /y "target\cms-app.war" "%TOMCAT_HOME%\webapps\cms-app.war" >nul
if errorlevel 1 (
    popd
    exit /b 1
)

set "CATALINA_HOME=%TOMCAT_HOME%"
set "CATALINA_BASE=%TOMCAT_HOME%"
set "DB_HOST=localhost"
set "DB_PORT=5432"
set "DB_NAME=cms_db"
set "DB_USER=cms_user"
set "DB_PASSWORD=cms_pw"

popd
start "CMS Tomcat" cmd.exe /k call "%TOMCAT_HOME%\bin\catalina.bat" run
