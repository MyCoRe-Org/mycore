namespace mycore.viewer.widgets.epub {

    export class EpubStructureBuilder {

        public convertToChapter(item: any, parent?: EpubStructureChapter): EpubStructureChapter {
            const chapters: mycore.viewer.model.StructureChapter[] = [];
            const chapter = new EpubStructureChapter(
                parent,
                typeof parent === 'undefined' ? 'root' : '',
                item.label,
                chapters,
                item);

            if (item.subitems != null) {
                item.subitems
                    .map((childItem) => {
                        return this.convertToChapter(childItem, chapter);
                    })
                    .forEach((childChapter) => {
                        chapters.push(childChapter);
                    });
            }

            return chapter;
        }

    }

}