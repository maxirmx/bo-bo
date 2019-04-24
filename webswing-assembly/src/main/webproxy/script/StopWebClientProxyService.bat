@echo off
setlocal enabledelayedexpansion
for /f "tokens=5 delims= " %%i in ('%SystemRoot%\System32\netstat -aon ^| %SystemRoot%\System32\findstr "127.0.0.1:31941"') do (
set pid=%%i
goto js
)
:js
%SystemRoot%\System32\taskkill /F /pid !pid!


for /f "tokens=5 delims= " %%i in ('%SystemRoot%\System32\netstat -aon ^| %SystemRoot%\System32\findstr "127.0.0.1:31942"') do (
set pid=%%i
goto js1
)
:js1
%SystemRoot%\System32\taskkill /F /pid !pid!