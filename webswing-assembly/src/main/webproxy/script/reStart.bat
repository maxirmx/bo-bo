@echo off
cd /d "%~dp0"

call "%~dp0StopWebClientProxyService.bat"
ping 127.0.0.1 -n 3 >nul
rd /s /q "%~dp0..\proxyService\tmp"

call "%~dp0StartWebClientProxyService.bat"
