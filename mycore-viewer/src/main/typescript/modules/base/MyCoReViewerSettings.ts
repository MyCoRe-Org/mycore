namespace mycore.viewer {

    export class MyCoReViewerSettings {
        mobile: boolean;
        doctype: string;
        tileProviderPath: string;
        filePath: string;
        derivate: string;
        i18nURL: string;
        lang: string;
        webApplicationBaseURL: string;
        derivateURL: string;
        onClose:()=>void;
        adminMail:string;

        static normalize(settings: MyCoReViewerSettings): MyCoReViewerSettings {
            var parameter = ViewerParameterMap.fromCurrentUrl();

            if (typeof settings.filePath != "undefined" && settings.filePath != null && settings.filePath.charAt(0) == '/') {
                settings.filePath = settings.filePath.substring(1);
            }

            if (typeof settings.derivateURL != "undefined" &&
                settings.derivateURL != null &&
                settings.derivateURL.charAt(settings.derivateURL.length - 1) != '/') {
                settings.derivateURL += "/";
            }

            settings.filePath = encodeURI(settings.filePath);

            if(settings.webApplicationBaseURL.lastIndexOf("/")==settings.webApplicationBaseURL.length-1){
                settings.webApplicationBaseURL = settings.webApplicationBaseURL.substring(0, settings.webApplicationBaseURL.length-1);
            }

            return settings;
        }

    }
}
