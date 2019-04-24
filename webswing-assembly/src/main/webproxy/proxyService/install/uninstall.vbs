strComputer = "."
Set objWMIService = GetObject("winmgmts:{impersonationLevel=impersonate}!\\" & strComputer & "\root\cimv2")
Set colOperatingSystems = objWMIService.ExecQuery ("Select * from Win32_OperatingSystem")

For Each objOSItem in colOperatingSystems
      os = "Version " & objOSItem.Version
Next
Set fso = CreateObject("Scripting.FileSystemObject")
installRoot = Chr(34) & fso.GetAbsolutePathName("..\..\") & Chr(34)
Set WshShell = CreateObject("WScript.Shell")

jre = "..\.." + "\cau\jre"
java = jre + "\bin\javaw.exe"

absJavaPath = fso.GetAbsolutePathName(java)
If Not fso.FileExists(absJavaPath) Then
    jre = "..\.." + "\jre"
    java = jre + "\bin\javaw.exe"
End If

absJavaPath = fso.GetAbsolutePathName(java)
If Not fso.FileExists(absJavaPath) Then
    jre = "..\.." + "\client\jre"
    java = jre + "\bin\javaw.exe"
End If

If (InStr (os, "Version 6")) > 0 then

Set objShell = CreateObject("Shell.Application")

myParameter = "-Djava.library.path=..\lib  -cp ..\lib\cau.jar;..\..\webproxy\lib\client-proxy.jar;..\lib\jdom.jar;..\lib\commons-io-1.3.1.jar;..\lib\commons-codec-1.9.jar;..\lib\commons-lang-2.3.jar com.swimap.cmf.cau.uninstall.UninstallMain "  + installRoot


productPreScript="..\..\uninstall\scripts\uninstall_client_pre_product.bat"

If fso.FileExists(productPreScript) then
strArgs="cmd /c" + productPreScript

    errorCode = WshShell.Run(strArgs , 0, true)
	preprodscripterrorcode = errorCode

	If Not errorCode = 0 then
		ErrorMsgArgs = "cmd /c echo Uninstallation canceled due to the failure to execute the pre-uninstallation script. >> %temp%\cau_update.log 2>&1"
		errorCode = WshShell.Run(ErrorMsgArgs, 0, false)
		WScript.Quit preprodscripterrorcode
	End If
End If


If WScript.Arguments.Count = 0 then
	objShell.ShellExecute java , myParameter , "", "runas"
Else
       For i = 1 To WScript.Arguments.Count Step 1
                 myParameter = myParameter + " " + WScript.Arguments(i-1)
      Next
	objShell.ShellExecute java , myParameter , "", "runas"
End If

Else

myParameter = ""

If WScript.Arguments.Count = 0 then
       errorCode = WshShell.Run(java + " " + "-Djava.library.path=..\lib  -cp ..\lib\cau.jar;..\..\webproxy\lib\client-proxy.jar;..\lib\jdom.jar;..\lib\commons-io-1.3.1.jar;..\lib\commons-codec-1.9.jar;..\lib\commons-lang-2.3.jar com.swimap.cmf.cau.uninstall.UninstallMain "  + installRoot  , 1, true)
Else
       For i = 1 To WScript.Arguments.Count Step 1
                 myParameter = myParameter + " " + WScript.Arguments(i-1)
      Next
       errorCode = WshShell.Run(java + " " + "-Djava.library.path=..\lib  -cp ..\lib\cau.jar;..\..\webproxy\lib\client-proxy.jar;..\lib\jdom.jar;..\lib\commons-io-1.3.1.jar;..\lib\commons-codec-1.9.jar;..\lib\commons-lang-2.3.jar com.swimap.cmf.cau.uninstall.UninstallMain "  + installRoot  , 1, true)
End If

End If

