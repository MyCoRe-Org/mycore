@echo off
rem ######################################################################
rem #
rem # This script sets some basic environment variables and calls Ant to
rem # build the MyCoRe system from sources. Use on Windows systems.
rem # 
rem # $Revision$ $Date$
rem #
rem #######################################################################

rem #######################################################################
rem #
rem # Change the following variables as needed for your local system:
rem #
rem #######################################################################

rem Directory where JDK is installed on your machine
set JAVA_HOME=C:\Java\j2sdk1.4.0_01

rem Directory where Ant is installed on your machine
set ANT_HOME=C:\Java\apache-ant-1.5.3-1

rem Persistence implementation to use [cm7 / cm8 / xmldb]
set MCR_PERSISTENCE=all

rem If you use cm7 or cm8, directory where IBM Content Manager is installed
set CM_HOME=c:\java\cmbroot

rem #######################################################################
rem #
rem # Do not change the rest of this script!
rem #
rem #######################################################################

%ANT_HOME%\bin\ant -find build.xml %1 %2 %3 %4 %5 %6 %7 %8 %9
