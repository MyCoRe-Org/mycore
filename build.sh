#! /bin/sh
#
# Start the ant building
#

# Configuration part

. ./setup.sh

# Start ant

$JAVA_HOME/bin/java -Dant.home=$ANT_HOME -classpath $CLASSPATH org.apache.tools.ant.Main $*

