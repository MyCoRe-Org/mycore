#!/bin/ksh
#
# Shell script to start the MyCoRe Command Line Interface

mcr1=$1
mcr2=$2
mcr3=$3
mcr4=$4

cd $MYCORE_HOME
. bin/setup.sh

java mycore.commandline.MCRCommandLineInterface $mcr1 $mcr2 $mcr3 $mcr4 $5 $6 $7 $8 $9

