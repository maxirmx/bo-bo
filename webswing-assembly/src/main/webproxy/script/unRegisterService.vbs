set WshShell=WScript.CreateObject("WScript.Shell")  
base_path = WshShell.SpecialFolders("Startup") + "\WebProxy.lnk"
set fso=WScript.CreateObject("scripting.filesystemobject")
If fso.fileExists(base_path) Then
	set sfile=WScript.CreateObject("scripting.filesystemobject").getfile(base_path)
	sfile.attributes=0
	sfile.delete
End If
