
#
# Configuration part for IBM Content Manager 8
#

export ANT_HOME=/usr/local/src/jakarta-ant-1.5
export DB2_HOME=/db2/db2inst1/sqllib
export CM8_HOME=/usr/lpp/cmb
export JAVA_HOME=/usr/java130/jre
export JDOM_HOME=/usr/local/src/jdom-1.0beta8
export SERVLET_HOME=/usr/WebSphere/AppServer

export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$JAVA_HOME/bin:$JAVA_HOME/bin/classic
export LIBPATH=$LIBPATH:$LD_LIBRARY_PATH

CLASSPATH=$JAVA_HOME/lib/rt.jar
CLASSPATH=$CLASSPATH:$JAVA_HOME/../lib/dt.jar
CLASSPATH=$CLASSPATH:$JAVA_HOME/../lib/tools.jar
CLASSPATH=$CLASSPATH:$DB2_HOME/java/sqlj.zip
CLASSPATH=$CLASSPATH:$DB2_HOME/function
CLASSPATH=$CLASSPATH:$DB2_HOME/java12/db2java.zip
CLASSPATH=$CLASSPATH:$DB2_HOME/java/runtime.zip
CLASSPATH=$CLASSPATH:$CM8_HOME/cmgmt
CLASSPATH=$CLASSPATH:$CM8_HOME/lib
CLASSPATH=$CLASSPATH:$CM8_HOME/lib/dtappsrv.jar
CLASSPATH=$CLASSPATH:$CM8_HOME/lib/cmbcm81.jar
CLASSPATH=$CLASSPATH:$CM8_HOME/lib/cmbicm81.jar
CLASSPATH=$CLASSPATH:$CM8_HOME/lib/cmbicmc81.jar
CLASSPATH=$CLASSPATH:$CM8_HOME/lib/cmbjdbc81.jar
CLASSPATH=$CLASSPATH:$CM8_HOME/lib/cmblog4j81.jar
CLASSPATH=$CLASSPATH:$CM8_HOME/lib/log4j.jar
CLASSPATH=$CLASSPATH:$ANT_HOME/lib/ant.jar
CLASSPATH=$CLASSPATH:$ANT_HOME/lib/crimson.jar
CLASSPATH=$CLASSPATH:$ANT_HOME/lib/jaxp.jar
CLASSPATH=$CLASSPATH:$JDOM_HOME/lib/xerces.jar
CLASSPATH=$CLASSPATH:$JDOM_HOME/lib/xalan.jar
CLASSPATH=$CLASSPATH:$JDOM_HOME/build/jdom.jar
CLASSPATH=$CLASSPATH:$SERVLET_HOME/lib/j2ee.jar
CLASSPATH=$CLASSPATH:$MYCORE_HOME/classes
export CLASSPATH

# Print environment

echo "$CLASSPATH \n"
echo "$LD_LIBRARY_PATH \n"
$JAVA_HOME/bin/java -version

