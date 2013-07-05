#!/bin/bash
#REQUIRES:This Script needs the inotify-tools package and a kernel above 2.6.12
#Allows it to automatically build the new iview2.js file, as soon as changes happen to any of the js files

declare -a sources
sources=( "Utils.js" "cache.js" "jquery.mousewheel.min.js" "jquery.tree.min.js" "jquery.simplemodal.js" "jquery.mcri18n.js" "ecma5compat.js" "xdr.js" "i18n.js" "PanoJS.js" "preload.js" "iviewInstance.js" "properties.js" "context.js" "currentImage.js" "chapter.js" "overview.js" "METS.js" "XML.js" "../lib/fg-menu/fg.menu.js" "iview2.toolbar/ToolbarManager.js" "iview2.toolbar/ToolbarModel.js" "iview2.toolbar/ToolbarButtonsetModel.js" "iview2.toolbar/ToolbarDividerModel.js" "iview2.toolbar/ToolbarSpringModel.js" "iview2.toolbar/ToolbarTextModel.js" "iview2.toolbar/ToolbarButtonModel.js" "iview2.toolbar/ToolbarImageModel.js" "iview2.toolbar/ToolbarController.js" "iview2.toolbar/ToolbarView.js" "iview2.toolbar/StandardToolbarModelProvider.js" "iview2.toolbar/PreviewToolbarModelProvider.js" "Permalink.js" "createPdf.js" "canvas.js" "canvas.rotate.js" "urnOverlay.js" "storage.js" "iview2.METS/ChapterModel.js" "iview2.METS/ChapterModelProvider.js" "iview2.METS/PhysicalEntry.js" "iview2.METS/PhysicalModel.js" "iview2.METS/PhysicalModelProvider.js" "iview2.thumbnailPanel/thumbnailPanelController.js" "iview2.thumbnailPanel/thumbnailPanelView.js" "iview2.scrollbar/scrollbarController.js" "iview2.scrollbar/scrollbarEvent.js" "iview2.scrollbar/scrollbarView.js" "jgestures.js")
destpath=${1%/*}
destfile=$1
path=`dirname $0`

srcpath=${path}"/../../../META-INF/resources/modules/iview2/js/"
mkdir -p $destpath

#initial Creation, we dont know what happened before the start of the script
rm -f $destfile
touch $destfile
for file in ${sources[@]}
do
	cat ${srcpath}$file >> $destfile
done

while { inotifywait -r -e modify -e create -e move -e delete $srcpath; }; do
	echo -n "building new version of ${destfile} "
	rm -f $destfile
	touch $destfile
	for file in ${sources[@]}
	do
		echo -n "."
		cat ${srcpath}$file >> $destfile
	done
	echo "done."
done
