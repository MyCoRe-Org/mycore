#!/bin/sh

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

WHICH=/usr/bin/which

# Directory where JDK is installed on your machine, if not already set
if [ "$JAVA_HOME" = "" ]; then
  JAVA_HOME=/usr/java_dev2
fi

#######################################################################
#
# Do not change the rest of this script!
#
#######################################################################

# Look for ANT on system
if [ -z "$ANT_BIN" ] ;  then
  ANT_BIN=`$WHICH ant 2>/dev/null`
fi
if [ -x "$ANT_BIN" ] ; then
  if [ `grep -l "org.apache.tools.ant.Main" "$ANT_BIN"` ] ; then :
  else unset ANT_BIN
  fi
fi
if [ -z "$ANT_BIN" ] ; then
  if [ -z "$ANT_HOME" ] ; then
    # try to find ANT
    if [ -d /opt/ant ] ; then 
      ANT_HOME=/opt/ant
    fi
  
    if [ -z "$ANT_HOME" ] ; then
      if [ -d "${HOME}/opt/ant" ] ; then 
        ANT_HOME="${HOME}/opt/ant"
      fi
    fi

    if [ -z "$ANT_HOME" ] ; then
      echo "Need to take a deeper look in your system to find ANT, wait a minute..."
      #some brute force may help then
      ANT_BIN=`find / -type f -name "ant" -exec test -x {} \; -exec grep -l "org.apache.tools.ant.Main" {} \; 2>/dev/null |head -1`
      ANT_HOME=`. $ANT_BIN >2&>/dev/null ; echo $ANT_HOME`
    fi

  fi
  if [ -z "$ANT_BIN" ] ; then
    if [ -x "$ANT_BIN/bin/ant" ] ; then
      ANT_BIN=$ANT_HOME/bin/ant
    else
      echo "No ANT installation found!"
      exit 1
    fi
  fi
fi

#when is that really needed anyway?
export JAVA_HOME
export ANT_HOME
export ANT_BIN

$ANT_BIN -logger org.apache.tools.ant.NoBannerLogger -find build.xml $*

