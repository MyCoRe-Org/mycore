namespace mycore.viewer.widgets.mets {

    export class MetsStructureModel extends model.StructureModel {

        constructor(_rootChapter:model.StructureChapter,
                _imageList:Array<model.StructureImage>,
                _chapterToImageMap:MyCoReMap<string,model.StructureImage>,
                _imageToChapterMap:MyCoReMap<string,model.StructureChapter>,
                _imageHrefImageMap:MyCoReMap<string, model.StructureImage>,
                private altoPresent:boolean) {
            super(_rootChapter, _imageList, _chapterToImageMap, _imageToChapterMap, _imageHrefImageMap, altoPresent);
        }

    }

}