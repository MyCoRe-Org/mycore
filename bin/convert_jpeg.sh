#!/bin/bash

. ./setup.sh

cd $MYCORE_HOME/documentation
for filename in *.jpg  ; do
  echo "$filename" | grep -i "jpg$" > /dev/null
  bb=`echo "$filename" | sed 's/jpg$/bb/i'`
  echo "exctracting BoundingBox  $filname to $bb ..."
  convert $filename eps:- | head -20 |  grep -e "^%!" -e "%%BoundingBox:" > $bb
done

