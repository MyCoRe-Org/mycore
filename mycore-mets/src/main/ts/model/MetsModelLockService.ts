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
    export class MetsModelLock {
        constructor(private httpService) {
        }

        lock(lockURL: string, callBack: (success: boolean) => void) {
            if (lockURL !== null && typeof lockURL !== "undefined") {
                const promise = this.httpService.get(lockURL);

                promise.success((data) => {
                    callBack(data.success || false);
                });

                promise.error(() => {
                    callBack(false);
                });
            } else {
                callBack(true);
            }
        }

        unlock(unLockURL) {
            if (typeof unLockURL !== "undefined" && unLockURL !== null) {
                const promise = this.httpService.get(unLockURL);
            }
        }
    }
}


