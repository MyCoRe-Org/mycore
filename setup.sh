
#
# Configuration part
#

export ANT_HOME=/usr/local/src/jakarta-ant-1.4
export DB2_HOME=/db2admin/sqllib
export CM7_HOME=/usr/lpp/cmb
export JAVA_HOME=/usr/java_dev2/jre
export MYCORE_HOME=/dlwww/mycore
export XERCES_HOME=/usr/local/src/xerces-1_4_4
export XALAN_HOME=/usr/local/src/xalan-j_2_2_D11

export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$JAVA_HOME/bin:$JAVA_HOME/bin/classic
export LIBPATH=$LIBPATH:$LD_LIBRARY_PATH

CLASSPATH=$JAVA_HOME/lib/rt.jar
CLASSPATH=$CLASSPATH:$JAVA_HOME/../lib/idlj.jar
CLASSPATH=$CLASSPATH:$JAVA_HOME/../lib/tools.jar
CLASSPATH=$CLASSPATH:$DB2_HOME/java/db2java.zip
CLASSPATH=$CLASSPATH:$DB2_HOME/java/sqlj.zip
CLASSPATH=$CLASSPATH:$CM7_HOME/lib/cmb71.jar
CLASSPATH=$CLASSPATH:$CM7_HOME/lib/cmbcm71.jar
CLASSPATH=$CLASSPATH:$CM7_HOME/lib/cmbdl71.jar
CLASSPATH=$CLASSPATH:$CM7_HOME/lib/cmbjdbc71.jar
CLASSPATH=$CLASSPATH:$CM7_HOME/lib/cmbfed71.jar
CLASSPATH=$CLASSPATH:$ANT_HOME/lib/ant.jar
CLASSPATH=$CLASSPATH:$ANT_HOME/lib/crimson.jar
CLASSPATH=$CLASSPATH:$ANT_HOME/lib/jaxp.jar
CLASSPATH=$CLASSPATH:$XERCES_HOME/xerces.jar
CLASSPATH=$CLASSPATH:$XERCES_HOME/xercesSamples.jar
CLASSPATH=$CLASSPATH:$XALAN_HOME/bin/xalan.jar
CLASSPATH=$CLASSPATH:$XALAN_HOME/bin/xalansamples.jar
CLASSPATH=$CLASSPATH:$MYCORE_HOME/classes
export CLASSPATH

# Print environment

echo "$CLASSPATH \n"
echo "$LD_LIBRARY_PATH \n"
$JAVA_HOME/bin/java -version

