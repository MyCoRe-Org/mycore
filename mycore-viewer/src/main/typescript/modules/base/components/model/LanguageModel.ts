/// <reference path="../../Utils.ts" />

namespace mycore.viewer.model {

    export class LanguageModel {

        constructor(private _keyTranslationMap: MyCoReMap<string, string>) {
        }

        public getTranslation(key: string) {
            return this._keyTranslationMap.has(key) ? this._keyTranslationMap.get(key) : "???" + key + "???";
        }

        public getFormatedTranslation(key: string, ...format: string[]) {
            return this._keyTranslationMap.has(key) ? ViewerFormatString(this._keyTranslationMap.get(key), format) : "???" + key + "??? " + format.join(" ");
        }

        public hasTranslation(key: string) {
            return this._keyTranslationMap.has(key);
        }

        public translate(element:JQuery) {
            let that = this;
            element.find("[data-i18n]").each(function() {
                let sub = $(this);
                let key:string = sub.data("i18n");
                if(!that.hasTranslation(key)) {
                    return;
                }
                sub.html(that.getTranslation(key));
            });
        }

    }

}