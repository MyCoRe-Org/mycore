module mycore.viewer.widgets.mets {
    export class MetsStructureModel extends model.StructureModel {

        constructor(_rootChapter:mycore.viewer.model.StructureChapter, _imageList:Array<mycore.viewer.model.StructureImage>, _chapterToImageMap:MyCoReMap<string,mycore.viewer.model.StructureImage>, _imageToChapterMap:MyCoReMap<string,mycore.viewer.model.StructureChapter>, private altoPresent:boolean) {
            super(_rootChapter, _imageList, _chapterToImageMap, _imageToChapterMap, altoPresent);
        }

    }
}