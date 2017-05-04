namespace mycore.viewer.widgets.canvas {
    export class DoublePageRelocatedLayout extends DoublePageLayout {

        public get relocated() : boolean {
            return true;
        }

        public getLabelKey(): string {
            return "doublePageRelocatedLayout";
        }


    }

}