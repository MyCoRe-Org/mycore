module mycore.viewer.widgets.chaptertree {

    export interface ChapterTreeChapter {
        parent: ChapterTreeChapter;
        id: string;
        label: string;
        chapter: Array<ChapterTreeChapter>;
    }

}