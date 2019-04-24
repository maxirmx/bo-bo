@echo off
set webSwingVersion=2.4
set base_path="%~dp0.."
set java_path="%~dp0..\..\jre\bin\javaw"
set libs=%base_path%\lib
set class_path=%libs%\client-proxy.jar;%libs%\slf4j-simple.jar;%libs%\slf4j-api.jar;%libs%\jackson-mapper-asl.jar;%libs%\jackson-core-asl.jar;%libs%\webswing-app-toolkit.jar

set certMgr=%base_path%\lib\certMgr-7.2.exe
cd /d "%~dp0"
%SystemRoot%\System32\wscript.exe "%~dp0unInstall.vbs"

for /L %%i in (1,1,3) do (
if exist "%~dp0unInstall_tmp.bat" (
  goto next
) else (
  echo waiting 3 sec
  ping 127.0.0.1 -n 3 >nul
)
)

:next

call "%~dp0unInstall_tmp.bat"
call "%startup_path%"

%certMgr% -del -c -n "127.0.0.1" -s -r localMachine Root

%java_path% -classpath %class_path% xx.yy.zz.aa.bb "uninstall"

call "%~dp0StopWebClientProxyService.bat"
:end