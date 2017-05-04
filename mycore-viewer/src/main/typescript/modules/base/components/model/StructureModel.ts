/// <reference path="../../Utils.ts" />
/// <reference path="StructureChapter.ts" />
/// <reference path="StructureImage.ts" />

namespace mycore.viewer.model {
    export class StructureModel {

        constructor(public _rootChapter:model.StructureChapter,
                    public _imageList: Array<model.StructureImage>,
                    public _chapterToImageMap:MyCoReMap<string, model.StructureImage>,
                    public _imageToChapterMap:MyCoReMap<string, model.StructureChapter>,
                    public _imageHrefImageMap:MyCoReMap<string, model.StructureImage>,
                    public _textContentPresent:boolean){
        }

        /**
         * will be used to calculate the default aspect ratio
         */
        public defaultPageDimension:Size2D;

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

        public get imageHrefImageMap() {
            return this._imageHrefImageMap;
        }

        public get isTextContentPresent():boolean {
                return this._textContentPresent;
        }

        public set rootChapter(rootChapter:model.StructureChapter) {
            this._rootChapter = rootChapter;
        }

        public set imageList(imageList:Array<model.StructureImage>) {
            this._imageList = imageList;
        }

        public set chapterToImageMap(chapterToImageMap:MyCoReMap<string,model.StructureImage>) {
            this._chapterToImageMap = chapterToImageMap;
        }

        public set imageToChapterMap(imageToChapterMap:MyCoReMap<string,model.StructureChapter>) {
            this._imageToChapterMap = imageToChapterMap;
        }

        public set imageHrefImageMap(imageHrefImageMap:MyCoReMap<string,model.StructureImage>) {
            this._imageHrefImageMap = imageHrefImageMap;
        }

        public set isTextContentPresent(textContentPresent:boolean){
            this._textContentPresent = textContentPresent;
        }

    }
}
