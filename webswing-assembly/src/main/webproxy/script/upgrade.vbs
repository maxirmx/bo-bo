version="2.4"
set WshShell=WScript.CreateObject("WScript.Shell")  
base_path = createobject("Scripting.FileSystemObject").GetFolder(WshShell.CurrentDirectory).path
homeDir=base_path + "\..\..\client\client"
java_path=base_path + "\..\..\jre\bin\java"
Startup=WshShell.SpecialFolders("Startup")  
startServer()
startUpgradeServer()

Function startServer()
    cmds="cmd /c """ + Startup + "\clientProxy.lnk"""
    WshShell.run cmds,vbhide
End Function

Function startUpgradeServer()
	libs=base_path + "\..\lib"
	class_path=libs + "\client-proxy.jar;" + libs + "\slf4j-simple.jar;" + libs + "\slf4j-api.jar;" + libs + "\jackson-mapper-asl.jar;" + libs + "\jackson-core-asl.jar;" + libs + "\webswing-app-toolkit.jar"
	WshShell.Run("""" & java_path & """ """ & "-classpath" & """ """ & class_path & """ """ & "xx.yy.zz.aa.bb"  & """ """ & "upgrade" & """ """ &  homeDir & """"),vbhide
End Function