/// <reference path="../../Utils.ts"/>
/// <reference path="ViewerEvent.ts" />

namespace mycore.viewer.widgets.events {
    export class ViewerEventManager {

        constructor() {
            this._callBackArray = new Array<any>();
        }

        private _callBackArray:Array<(e:ViewerEvent)=>void>;

        public bind(callback:(e:ViewerEvent)=>void) {
            this._callBackArray.push(callback);
        }

        public trigger(e:ViewerEvent) {
            for (var i in this._callBackArray) {
                var callback = this._callBackArray[i];
                // if one handler throws a error it will be catched
                callback(e);
            }
        }

        public unbind(callback:(e:ViewerEvent)=>void) {
            var index = this._callBackArray.lastIndexOf(callback);
            this._callBackArray.splice(index, 1);
        }

    }
}