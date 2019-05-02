@echo off
rem ---------------------------------------------------------------------------
rem Start script for the webswing Server, core logic
rem
rem   isCitrix        Whether is citrix env.
rem
rem   arg             Whether is direct mode.
rem                   Only citrix env works, Only webproxyservice code allow to call.
rem ---------------------------------------------------------------------------

set currentPath=%~dp0
cd /d %currentPath%

@echo currentPath is %currentPath%

set isCitrix=%1
set arg=%2
if "%arg%"=="direct" (
    goto directMode
)
@echo isDirect is %arg%

if %isCitrix%==true (
    goto citrixMode
) else (
    goto directMode
)

:directMode
    @echo This is direct mode, start webswing directly
    cd ..\proxyService
    @call webswing.bat
    set startReturn=%errorlevel%
    @echo directMode startReturn is  %startReturn%
    goto END

:citrixMode
    @echo This is not direct mode, begin check and start WebProxyService.
    %SystemRoot%\System32\sc query WebProxyService
    %SystemRoot%\System32\net stop WebProxyService
    cd ..\proxyService
    WebProxyService.exe -start
    set startReturn=%errorlevel%
    @echo citrixMode startReturn is  %startReturn%
    goto END

:END
