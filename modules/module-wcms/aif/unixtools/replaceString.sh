#!/bin/bash
echo $1
if [ "$#" -ne "2" ]
	then echo usage: sc.sh string1 string2
		exit
		fi
if [ -d tmp ]
	then echo Please remove directory tmp!;
	exit
	fi
mkdir tmp
echo Archiving in directory archiv
if [ -d archiv ]
	then echo Please remove directory archiv
	exit
	fi
mkdir archiv
cp * archiv
for i in * 
do if [ -d $i ]
	then echo $i is a directory
	else 	out=`grep $1 $i`
		if [ -n "$out" ]
			then echo Replacing $1 in $i ... 
			sed "s/$1/$2/g" $i >tmp/$i
			fi
	fi
done
cp tmp/* .
rm -rf tmp
