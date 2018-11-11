@echo off
set JLINK_VM_OPTIONS={{JLINK_VM_OPTIONS}}
set DIR=%~dp0
%DIR%\java -jar %JLINK_VM_OPTIONS% -jar %DIR%\{{JAR_NAME}} $@
