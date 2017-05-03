/// <reference path="../definitions/pdf.d.ts" />


namespace mycore.viewer.widgets.pdf {

    export class PDFStructureModel extends model.StructureModel {
        /**
        * The Structure of a PDFDocument
        * @param _rootChapter The Root Chapter of a PDF ///TODO: should be named after the PDF name
        * @param _imageList The list of all Rendered Pages of the PDF
        * @param _chapterToImageMap A map to resolve the first image of the Chapter
        * @param _imageToChapterMap A map to resolve the Chapter of a Image
        * @param refPageMap
        * @param idPdfPageMap
        */
        constructor( _rootChapter: mycore.viewer.model.StructureChapter,
            _imageList: Array<model.StructureImage>,
            _chapterToImageMap: MyCoReMap<string, model.StructureImage>,
            _imageToChapterMap: MyCoReMap<string, model.StructureChapter>,
            _imageHrefImageMap:MyCoReMap<string, model.StructureImage>,
            public refPageMap: MyCoReMap<string, PDFPageProxy> ) {
            super( _rootChapter, _imageList, _chapterToImageMap, _imageToChapterMap, _imageHrefImageMap, true );
        }

    }

}
