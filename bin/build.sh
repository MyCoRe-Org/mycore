#! /bin/sh
#
# Start the ant building
#

# Configuration part

cd $MYCORE_HOME
. bin/setup.sh

# Start ant

$JAVA_HOME/bin/java -Dant.home=$ANT_HOME -classpath $CLASSPATH org.apache.tools.ant.Main $*

