
#
# Configuration part
#

export ANT_HOME=/usr/local/src/jakarta-ant-1.3
export CM7_HOME=/usr/lpp/cmb
export JAVA_HOME=/usr/java_dev2/jre
export MYCORE_HOME=/dlwww/mycore
export XERCES_HOME=/usr/local/src/xerces-1_4_3
#export XERCES_HOME=/usr/local/src/xerces-2_0_0_beta

export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$JAVA_HOME/bin:$JAVA_HOME/bin/classic
export LIBPATH=$LIBPATH:$LD_LIBRARY_PATH

CLASSPATH=$JAVA_HOME/lib/rt.jar
CLASSPATH=$CLASSPATH:$JAVA_HOME/../lib/idlj.jar
CLASSPATH=$CLASSPATH:$JAVA_HOME/../lib/tools.jar
CLASSPATH=$CLASSPATH:$CM7_HOME/lib/cmb30.jar
CLASSPATH=$CLASSPATH:$CM7_HOME/lib/cmbcm30.jar
CLASSPATH=$CLASSPATH:$CM7_HOME/lib/cmbdl30.jar
CLASSPATH=$CLASSPATH:$CM7_HOME/lib/cmbjdbc30.jar
CLASSPATH=$CLASSPATH:$CM7_HOME/lib/cmbfed30.jar
CLASSPATH=$CLASSPATH:$XERCES_HOME/xerces.jar
CLASSPATH=$CLASSPATH:$XERCES_HOME/xercesSamples.jar
CLASSPATH=$CLASSPATH:$ANT_HOME/lib/jaxp.jar
CLASSPATH=$CLASSPATH:$ANT_HOME/lib/parser.jar
CLASSPATH=$CLASSPATH:$ANT_HOME/dist/lib/ant.jar
CLASSPATH=$CLASSPATH:$ANT_HOME/dist/lib/optional.jar
CLASSPATH=$CLASSPATH:$MYCORE_HOME/classes
CLASSPATH=$CLASSPATH:$MYCORE_HOME/demo_dublin_core/classes
export CLASSPATH

# Print environment

echo "$CLASSPATH \n"
echo "$LD_LIBRARY_PATH \n"
$JAVA_HOME/bin/java -version

