/// <reference path="../../Utils.ts" />
/// <reference path="MyCoReImageViewerEvent.ts" />
/// <reference path="../ViewerComponent.ts" />
/// <reference path="../model/LanguageModel.ts" />

namespace mycore.viewer.components.events {
    export class LanguageModelLoadedEvent extends MyCoReImageViewerEvent {
        constructor(component: ViewerComponent,private _languageModel:model.LanguageModel) {
            super(component, LanguageModelLoadedEvent.TYPE);
        }

        public get languageModel():model.LanguageModel {
            return this._languageModel;
        }

        public static TYPE:string = "LanguageModelLoadedEvent";



    }

}