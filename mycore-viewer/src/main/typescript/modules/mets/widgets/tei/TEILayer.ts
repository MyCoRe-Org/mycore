namespace mycore.viewer.widgets.tei {
    export class TEILayer implements model.Layer {

        constructor(private _id: string, private _label: string, private mapping: MyCoReMap<string,string>, private contentLocation: string, private teiStylesheet: string) {
        }

        getId():string {
            return this._id;
        }

        getLabel():string {
            return this._label;
        }

        resolveLayer(pageHref:string, callback:(success:boolean, content?:JQuery)=>void):void {
            if (this.mapping.has(pageHref)) {
                var settings:JQueryAjaxSettings = {};

                settings.async = true;
                settings.success = function (data:any, textStatus:string, jqXHR:JQueryXHR) {
                    callback(true,jQuery(data));
                };

                jQuery.ajax(this.contentLocation + this.mapping.get(pageHref) + "?XSL.Style=" + this.teiStylesheet, settings);
            } else {
                callback(false);
            }
        }
    }
}
