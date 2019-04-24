set WshShell=WScript.CreateObject("WScript.Shell")  
Startup=WshShell.SpecialFolders("Startup")  
cmds="cmd /c """ + Startup + "\clientProxy.lnk"""
WshShell.run cmds,vbhide