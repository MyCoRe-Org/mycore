/// <reference path="../../Utils.ts" />
/// <reference path="StructureChapter.ts" />
/// <reference path="StructureImage.ts" />

module mycore.viewer.model {
    export class StructureModel {

        constructor(public _rootChapter:mycore.viewer.model.StructureChapter,
                    public _imageList: Array<mycore.viewer.model.StructureImage>,
                    public _chapterToImageMap:MyCoReMap<string,mycore.viewer.model.StructureImage>,
                    public _imageToChapterMap:MyCoReMap<string,mycore.viewer.model.StructureChapter>,
                    public _textContentPresent:boolean){
        }

        public get rootChapter() {
            return this._rootChapter;
        }

        public get imageList() {
            return this._imageList;
        }

        public get chapterToImageMap() {
            return this._chapterToImageMap;
        }

        public get imageToChapterMap() {
            return this._imageToChapterMap;
        }

        public get isTextContentPresent():boolean {
                return this._textContentPresent;
        }

        public set rootChapter(rootChapter:mycore.viewer.model.StructureChapter) {
            this._rootChapter = rootChapter;
        }

        public set imageList(imageList:Array<mycore.viewer.model.StructureImage>) {
            this._imageList = imageList;
        }

        public set chapterToImageMap(chapterToImageMap:MyCoReMap<string,mycore.viewer.model.StructureImage>) {
            this._chapterToImageMap = chapterToImageMap;
        }

        public set imageToChapterMap(imageToChapterMap:MyCoReMap<string,mycore.viewer.model.StructureChapter>) {
            this._imageToChapterMap = imageToChapterMap;
        }

        public set isTextContentPresent(textContentPresent:boolean){
            this._textContentPresent = textContentPresent;
        }

    }
}