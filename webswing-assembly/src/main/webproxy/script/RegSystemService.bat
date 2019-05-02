@echo off
rem ---------------------------------------------------------------------------
rem  Install & Start script for the webswing Server, core logic
rem
rem   return 0        success
rem
rem   return 1        register Certificate fail
rem
rem   return 2        weswing install fail
rem
rem   return 3        weswing start fail
rem ---------------------------------------------------------------------------
set currentPath=%~dp0
cd /d %currentPath%
%SystemRoot%\System32\net stop WebProxyService
%SystemRoot%\System32\sc delete WebProxyService
cd ..
cd ..
cd ..
call regCert.bat
set regCertResult=%regCertReturn%
@echo regCertResult is %regCertResult%

cd /d %currentPath%
cd ..\proxyService
WebProxyService.exe -install auto
set WebProxyServiceInstallResult=%errorlevel%
@echo WebProxyServiceInstallResult is %WebProxyServiceInstallResult%
WebProxyService.exe -start
set WebProxyServiceStartResult=%errorlevel%
@echo WebProxyServiceStartResult is %WebProxyServiceStartResult%

if %regCertResult% equ 0 if %WebProxyServiceInstallResult% equ 0 if %WebProxyServiceStartResult% equ 0   (
set res=0
goto END
)
if %regCertResult% neq 0 (
set res=1
goto END
)
if %WebProxyServiceInstallResult% neq 0 (
set res=2
goto END
)
if %WebProxyServiceStartResult% neq 0 (
set res=3
goto END
)

:END
