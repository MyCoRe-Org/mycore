
#
# Configuration part for Open Source under Linux
#

export ANT_HOME=/opt/jakarta/ant
export EXIST_HOME=/usr/local/src/eXist-0.9.2
export TOMCAT_HOME=/opt/jakarta/tomcat

CLASSPATH=$CLASSPATH:$ANT_HOME/lib/ant.jar
CLASSPATH=$CLASSPATH:$ANT_HOME/lib/optional.jar

CLASSPATH=$CLASSPATH:$TOMCAT_HOME/common/lib/servlet.jar

CLASSPATH=$CLASSPATH:$EXIST_HOME/exist.jar
CLASSPATH=$CLASSPATH:$EXIST_HOME/lib/core/xmlrpc-1.1.jar

CLASSPATH=$CLASSPATH:$MYCORE_HOME/lib/log4j-1_2_7.jar
CLASSPATH=$CLASSPATH:$MYCORE_HOME/lib/xerces-xml-apis-2_2_1.jar
CLASSPATH=$CLASSPATH:$MYCORE_HOME/lib/xercesImpl-2_2_1.jar
CLASSPATH=$CLASSPATH:$MYCORE_HOME/lib/xercesSamples-2_2_1.jar
CLASSPATH=$CLASSPATH:$MYCORE_HOME/lib/xalan-xml-apis-j_2_4_1.jar
CLASSPATH=$CLASSPATH:$MYCORE_HOME/lib/xalan-j_2_4_1.jar
CLASSPATH=$CLASSPATH:$MYCORE_HOME/lib/jdom-beta8.jar

CLASSPATH=$CLASSPATH:$MYCORE_HOME/lib/ftp.jar
CLASSPATH=$CLASSPATH:$MYCORE_HOME/lib/mail.jar
CLASSPATH=$CLASSPATH:$MYCORE_HOME/lib/mysql-connector-java-2.0.14-bin.jar
CLASSPATH=$CLASSPATH:$MYCORE_HOME/lib/xmldb.jar
CLASSPATH=$CLASSPATH:$MYCORE_HOME/classes

export CLASSPATH

# Print environment

echo "$CLASSPATH \n"
echo "$LD_LIBRARY_PATH \n"
$JAVA_HOME/bin/java -version

