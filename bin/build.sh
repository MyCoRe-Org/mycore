#! /bin/sh

#######################################################################
#
# This script sets some basic environment variables and calls Ant to
# build the MyCoRe system from sources. Use on UNIX systems.
#
# $Revision$ $Date$
#
#######################################################################

#######################################################################
#
# Change the following variables as needed for your local system:
#
#######################################################################

# Directory where JDK is installed on your machine
JAVA_HOME=/usr/java_dev2

# Directory where Ant is installed on your machine
ANT_HOME=/opt/jakarta-ant-1.5.1

#######################################################################
#
# Do not change the rest of this script!
#
#######################################################################

export JAVA_HOME
export ANT_HOME

$ANT_HOME/bin/ant -find build.xml $*

