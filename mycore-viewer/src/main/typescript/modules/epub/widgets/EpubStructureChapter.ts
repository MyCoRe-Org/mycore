namespace mycore.viewer.widgets.epub {

    export class EpubStructureChapter extends mycore.viewer.model.StructureChapter {

        constructor(parent: mycore.viewer.model.StructureChapter,
                    type: string,
                    label: string,
                    chapter: mycore.viewer.model.StructureChapter[],
                    public epubChapter: any) {
            super(parent, type, (epubChapter != null) ? epubChapter.href : 'root', label, chapter, new MyCoReMap(), (c) => c(null));
        }

    }

}