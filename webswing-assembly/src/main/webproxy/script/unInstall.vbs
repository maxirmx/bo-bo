set WshShell=WScript.CreateObject("WScript.Shell")
base_path = createobject("Scripting.FileSystemObject").GetFolder(WshShell.CurrentDirectory).path
startup_path = WshShell.SpecialFolders("Startup") + "\clientProxy.lnk"

Set fso=createObject("Scripting.FileSystemObject")
if Not IsExitAFile(base_path + "\unInstall_tmp.bat") Then 
set ttfile=fso.createtextfile(base_path + "\unInstall_tmp.bat" ,ture)
ttfile.writeline("set startup_path=" + startup_path)
ttfile.close
Else
End If


Function IsExitAFile(filespec)
        Dim fso
        Set fso=CreateObject("Scripting.FileSystemObject")        
        If fso.fileExists(filespec) Then         
        IsExitAFile=True        
        Else IsExitAFile=False        
        End If
End Function 
