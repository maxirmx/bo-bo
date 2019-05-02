set WshShell=WScript.CreateObject("WScript.Shell")  
base_path = createobject("Scripting.FileSystemObject").GetFolder(WshShell.CurrentDirectory).path
java_path=base_path + "\..\..\jre\bin\java"
jvm_dump="-XX:HeapDumpPath=" + base_path

WshShell.Run("""" & java_path & """ """ & "-jar" & """ """ & "-Xms512m" & """ """ & "-Xmx512m" & """ """ & "-XX:+HeapDumpOnOutOfMemoryError" & """ """ & jvm_dump & """ """ & "webswing-server.war" & """ """ & "-j"  & """ """ & "jetty.properties" & """"),vbhide