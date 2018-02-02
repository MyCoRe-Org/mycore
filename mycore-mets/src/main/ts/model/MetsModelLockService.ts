/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

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
        constructor(private httpService: any) {
        }

        public lock(lockURL: string, callBack: (success: boolean) => void) {
            if (lockURL !== null && typeof lockURL !== 'undefined') {
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

        public unlock(unLockURL: string) {
            if (typeof unLockURL !== 'undefined' && unLockURL !== null) {
                const promise = this.httpService.get(unLockURL);
            }
        }
    }
}
