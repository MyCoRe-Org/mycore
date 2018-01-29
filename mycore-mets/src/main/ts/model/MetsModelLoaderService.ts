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

namespace org.mycore.mets.model {

    import MetsModel = org.mycore.mets.model.simple.MCRMetsSimpleModel;

    /**
     * This is the service which loads the mets file and parses it into simple model.
     */
    export class MetsModelLoader {
        constructor(private httpService: any) {
        }

        public load(url: string, callBack: (model: MetsModel) => void) {
            const promise = this.httpService.get(url);

            promise.success((metsData) => {
                callBack(MetsModel.fromJson(metsData));
            });

            promise.error(() => {
                // TODO: ERROR HANDLING
            });
        }
    }
}
