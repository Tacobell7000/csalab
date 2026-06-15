@echo off
REM 1. No DISPLAY export needed for Windows (usually)
REM 2. Move to project root
cd /d "%~dp0\.."

REM 3. Create bin folder
if not exist bin mkdir bin

REM 4. Compile all root .java files into bin
echo 🔨 Compiling classes...
javac -cp "lib/core/core.jar;." -d bin *.java

REM 5. If successful, run
if %ERRORLEVEL% EQU 0 (
    echo 🚀 Launching Game...
    java -cp "lib/core/core.jar;bin;." TDMain
) else (
    echo ❌ Compilation failed.
)
