@echo off
title ANTEVAULT
cd /d "%~dp0"
set JAR=lwjgl3\build\libs\antevault-1.0.0.jar

rem Needs Java 11 or newer. Prefer JAVA_HOME, otherwise whatever "java" is on the PATH.
set JAVA=java
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA=%JAVA_HOME%\bin\java.exe"

if not exist "%JAR%" (
  echo Building the game for the first time...
  call gradlew.bat lwjgl3:jar || goto failed
)

rem plain java rather than javaw, so a crash leaves a trace instead of the window just vanishing
"%JAVA%" -jar "%JAR%" 2>> antevault-crash.log
if errorlevel 1 goto failed
exit /b 0

:failed
echo.
echo ANTEVAULT could not start. It needs Java 11 or newer (check with: java -version).
echo Any details were written to antevault-crash.log
pause
