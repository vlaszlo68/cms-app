@echo off
setlocal

rem Stop the standalone local CMS Tomcat instance.
set "TOMCAT_HOME=C:\Tomcat 9.0"

if not exist "%TOMCAT_HOME%\bin\catalina.bat" set "TOMCAT_HOME=C:\tomcat9"
if not exist "%TOMCAT_HOME%\bin\catalina.bat" (
    echo Tomcat 9 was not found in C:\Tomcat 9.0 or C:\tomcat9.
    exit /b 1
)

set "CATALINA_HOME=%TOMCAT_HOME%"
set "CATALINA_BASE=%TOMCAT_HOME%"
call "%TOMCAT_HOME%\bin\catalina.bat" stop
if errorlevel 1 exit /b 1

echo Local CMS Tomcat stopped.
