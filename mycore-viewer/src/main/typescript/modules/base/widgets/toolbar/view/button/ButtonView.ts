/// <reference path="../../../../definitions/jquery.d.ts" />

namespace mycore.viewer.widgets.toolbar {

    export interface ButtonView {
        updateButtonLabel(label: string): void;
        updateButtonTooltip(tooltip: string): void;
        updateButtonIcon(icon: string): void;
        updateButtonClass(buttonClass: string): void;
        updateButtonActive(active: boolean): void;
        updateButtonDisabled(disabled: boolean): void;
        getElement(): JQuery;
    }

}