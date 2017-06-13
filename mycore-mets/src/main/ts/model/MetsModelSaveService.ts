///<reference path="simple/MCRMetsSimpleModel.ts"/>
///<reference path="simple/MCRMetsPage.ts"/>
///<reference path="simple/MCRMetsSection.ts"/>


namespace org.mycore.mets.model {

    import MetsModel = org.mycore.mets.model.simple.MCRMetsSimpleModel;
    import MCRMetsPage = org.mycore.mets.model.simple.MCRMetsPage;
    import MCRMetsSection = org.mycore.mets.model.simple.MCRMetsSection;
    /**
     * This is the service which loads the mets file and parses it into simple model.
     */
    export class MetsModelSave {
        constructor(private httpService) {
        }

        save(url: string, model: MetsModel, callBack: (success: boolean) => void) {
            const jsonData = MetsModel.toJson(model);
            const promise = this.httpService.post(url, jsonData);

            promise.success(() => {
                callBack(true);
            });

            promise.error(() => {
                callBack(false);
            });
        }
    }
}

