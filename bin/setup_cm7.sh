
#
# Configuration part
#

export ANT_HOME=/usr/local/src/jakarta-ant-1.5.1
export DB2_HOME=/db2admin/sqllib
export CM7_HOME=/usr/lpp/cmb
export JAVA_HOME=/usr/java_dev2/jre
export SERVLET_HOME=/usr/local/src/tomcat

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

CLASSPATH=$CLASSPATH:$SERVLET_HOME/lib/common/servlet.jar

CLASSPATH=$CLASSPATH:$ANT_HOME/lib/ant.jar
CLASSPATH=$CLASSPATH:$ANT_HOME/lib/optional.jar

CLASSPATH=$CLASSPATH:$MYCORE_HOME/lib/xerces-xml-apis-2_2_1.jar
CLASSPATH=$CLASSPATH:$MYCORE_HOME/lib/xercesImpl-2_2_1.jar
CLASSPATH=$CLASSPATH:$MYCORE_HOME/lib/xercesSamples-2_2_1.jar
CLASSPATH=$CLASSPATH:$MYCORE_HOME/lib/xalan-xml-apis-j_2_4_1.jar
CLASSPATH=$CLASSPATH:$MYCORE_HOME/lib/xalan-j_2_4_1.jar
CLASSPATH=$CLASSPATH:$MYCORE_HOME/lib/jdom-beta8.jar

CLASSPATH=$CLASSPATH:$MYCORE_HOME/lib/ftp.jar
CLASSPATH=$CLASSPATH:$MYCORE_HOME/lib/mail.jar
CLASSPATH=$CLASSPATH:$MYCORE_HOME/classes

export CLASSPATH

# Print environment

echo "$CLASSPATH \n"
echo "$LD_LIBRARY_PATH \n"
$JAVA_HOME/bin/java -version

