@echo off
rem
rem Start the ant building
rem

rem -------------------- Configuration -----------------------------

echo.
echo ***** build.cmd: Starting configuration tasks... *****
echo.
call .\setup.cmd
echo.
echo ***** build.cmd: Configuration done. *****


rem -------------------- start build with ant ----------------------

echo ***** build.cmd: Starting ant... *****
echo.
"%JAVA_HOME%\bin\java" -Dant.home="%ANT_HOME%" -classpath "%CLASSPATH%" org.apache.tools.ant.Main %1
echo.
echo ***** build.cmd: Build done. *****
echo.



