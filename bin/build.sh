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

# Directory where JDK is installed on your machine, if not already set
if [ "$JAVA_HOME" = "" ]; then
  JAVA_HOME=/usr/java_dev2
fi

# Directory where Ant is installed on your machine, if not already set
if [ "$ANT_HOME" = "" ]; then
  ANT_HOME=/usr/share/ant
fi
if [ -d $ANT_HOME/bin ]; then
  PATH=$PATH:$ANT_HOME
  export PATH
fi

#######################################################################
#
# Do not change the rest of this script!
#
#######################################################################

export JAVA_HOME
export ANT_HOME

ant -logger org.apache.tools.ant.NoBannerLogger -find build.xml $*

