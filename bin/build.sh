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

# Persistence implementation to use [cm7 | cm8 | xmldb]
PERSISTENCE=cm7

# If you use cm7 or cm8, directory where IBM Content Manager is installed
CM_HOME=/usr/lpp/cmb/

#######################################################################
#
# Do not change the rest of this script!
#
#######################################################################

export JAVA_HOME
export ANT_HOME
export PATH=$ANT_HOME/bin:$PATH

cd ..
ant -DCM_HOME=$CM_HOME -DPERSISTENCE=$PERSISTENCE $*

