@echo off
rem
rem Configuration part
rem

rem -------------- set environment variables -------------- 

set ANT_HOME=c:\program files\jakarta-ant-1.4.1
set CM7_HOME=d:\cmbroot
set JAVA_HOME=c:\program files\jdk1.2.2
set MYCORE_HOME=d:\mycore
set XERCES_HOME=c:\program files\xerces-1_4_3

set CLASSPATH=%JAVA_HOME%\jre\lib\rt.jar
set CLASSPATH=%CLASSPATH%;%JAVA_HOME%\lib\tools.jar
set CLASSPATH=%CLASSPATH%;%CM7_HOME%\lib\cmb71.jar
set CLASSPATH=%CLASSPATH%;%CM7_HOME%\lib\cmbcm71.jar
set CLASSPATH=%CLASSPATH%;%CM7_HOME%\lib\cmbdl71.jar
set CLASSPATH=%CLASSPATH%;%CM7_HOME%\lib\cmbjdbc71.jar
set CLASSPATH=%CLASSPATH%;%CM7_HOME%\lib\cmbfed71.jar
set CLASSPATH=%CLASSPATH%;%XERCES_HOME%\xerces.jar
set CLASSPATH=%CLASSPATH%;%XERCES_HOME%\xercesSamples.jar
set CLASSPATH=%CLASSPATH%;%ANT_HOME%\lib\ant.jar
set CLASSPATH=%CLASSPATH%;%ANT_HOME%\lib\crimson.jar
set CLASSPATH=%CLASSPATH%;%ANT_HOME%\lib\jaxp.jar
set CLASSPATH=%CLASSPATH%;%MYCORE_HOME%\classes
set CLASSPATH=%CLASSPATH%;%MYCORE_HOME%\demo_dublin_core\classes

rem -------------- Print environment -------------- 

echo ANT_HOME    : %ANT_HOME%
echo CM7_HOME    : %CM7_HOME%
echo JAVA_HOME   : %JAVA_HOME%
echo MYCORE_HOME : %MYCORE_HOME%
echo XERCES_HOME : %XERCES_HOME%
echo.
echo CLASSPATH   : 
echo %CLASSPATH%
echo.

rem -------------- show java version -------------- 

"%JAVA_HOME%\bin\java" -version
