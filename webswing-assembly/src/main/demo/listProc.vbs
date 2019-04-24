file=Wscript.Arguments(0)
sId=Wscript.Arguments(1)
browser_exe=Wscript.Arguments(2)
Set shell = Wscript.createobject("wscript.shell")
cmmd="cmd /c %SystemRoot%\system32\tasklist /v /FI "&Chr(34)&"STATUS eq running"&Chr(34)&" /FI "&Chr(34)&"IMAGENAME eq "&browser_exe&Chr(34)&" /FI "&Chr(34)&"SESSION eq "&sId&Chr(34)&" /fo csv >> "&Chr(34)&file&Chr(34)
shell.run cmmd, 0
