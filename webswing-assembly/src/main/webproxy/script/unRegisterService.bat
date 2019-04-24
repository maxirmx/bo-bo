@echo off
cd /d "%~dp0"
call "%~dp0StopWebClientProxyService.bat" 1>nul 2>nul
%SystemRoot%\System32\wscript.exe "%~dp0unRegisterService.vbs" 1>nul 2>nul
