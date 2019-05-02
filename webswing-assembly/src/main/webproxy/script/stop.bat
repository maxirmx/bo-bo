@echo off
rem ---------------------------------------------------------------------------
rem Stop script for the webswing Server, core logic
rem ---------------------------------------------------------------------------
setlocal enabledelayedexpansion
set pid=
for /f "tokens=5 delims= " %%i in ('%SystemRoot%\System32\netstat -aon ^|%SystemRoot%\System32\findstr 127.0.0.1:31941 ^|%SystemRoot%\System32\findstr LISTENING') do (
set pid=%%i
if defined pid (
%SystemRoot%\System32\taskkill /F /pid !pid!)
)

set pid=
for /f "tokens=5 delims= " %%i in ('%SystemRoot%\System32\netstat -aon ^|%SystemRoot%\System32\findstr 127.0.0.1:31942 ^|%SystemRoot%\System32\findstr LISTENING') do (
set pid=%%i
if defined pid (
%SystemRoot%\System32\taskkill /F /pid !pid!)
)

set pid=
for /f "tokens=5 delims= " %%i in ('%SystemRoot%\System32\netstat -aon ^|%SystemRoot%\System32\findstr 127.0.0.1:34455 ^|%SystemRoot%\System32\findstr LISTENING') do (
set pid=%%i
if defined pid (
%SystemRoot%\System32\taskkill /F /pid !pid!)
)

%SystemRoot%\System32\net stop WebProxyService