#!/bin/bash
#REQUIRES:This Script needs the inotify-tools package and a kernel above 2.6.12
#Allows it to automatically build the new iview2.js file, as soon as changes happen to any of the js files

#ADOPT:destpath to your docportal build dir
declare -a sources
sources=( "jquery.mousewheel.min.js" "jquery.tree.min.js" "PanoJS.js" "init.js"  "Event.js" "chapter.js" "overview.js" "cutOut.js" "METS.js" "scrollBars.js" "XML.js" "Utils.js" "Thumbnail.js" "../lib/fg-menu/fg.menu.js" "iview2.toolbar/ToolbarImporter.js" "iview2.toolbar/ToolbarManager.js" "iview2.toolbar/ToolbarModel.js" "iview2.toolbar/ToolbarButtonsetModel.js" "iview2.toolbar/ToolbarDividerModel.js" "iview2.toolbar/ToolbarSpringModel.js" "iview2.toolbar/ToolbarTextModel.js" "iview2.toolbar/ToolbarButtonModel.js" "iview2.toolbar/ToolbarController.js" "iview2.toolbar/ToolbarView.js" "iview2.toolbar/StandardToolbarModelProvider.js" "iview2.toolbar/PreviewToolbarModelProvider.js" "Permalink.js" )
destpath="/host/workspace/archive/docportal/build/webapps/modules/iview2/web/js/iview2.js"

path=`dirname $0`
srcpath=${path}"/../web/modules/iview2/web/js/"

#initial Creation, we dont know what happened before the start of the script
rm -f $destpath
touch $destpath
for file in ${sources[@]}
do
	cat ${srcpath}$file >> $destpath
done

while { inotifywait -r -e modify -e create -e move -e delete $srcpath; }; do
	rm -f $destpath
	touch $destpath
	for file in ${sources[@]}
	do
		cat ${srcpath}$file >> $destpath
	done
done
