namespace mycore.viewer.widgets.chaptertree {

    export interface ChapterTreeView {
        addNode(parentId: string, id: string, label: string, childLabel: string, expandable: boolean);
        setOpened(id: string, opened: boolean);
        setSelected(id: string, selected: boolean);
        jumpTo(id:string);
    }

}