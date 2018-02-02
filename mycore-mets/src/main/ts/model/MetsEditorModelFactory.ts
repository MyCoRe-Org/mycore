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
///<reference path="MetsModelLoaderService.ts"/>
///<reference path="MetsEditorConfiguration.ts"/>
///<reference path="MetsEditorModel.ts"/>
///<reference path="MetsEditorParameter.ts"/>

namespace org.mycore.mets.model {

    import MetsModel = org.mycore.mets.model.simple.MCRMetsSimpleModel;

    /**
     * This is the service which loads the mets file and parses it into simple model.
     */
    export class MetsEditorModelFactory {
        private metsModelLoaderService: MetsModelLoader;
        private metsEditorConfiguration: MetsEditorConfiguration;

        constructor(modelLoader: MetsModelLoader, editorConfiguration: MetsEditorConfiguration) {
            this.metsModelLoaderService = modelLoader;
            this.metsEditorConfiguration = editorConfiguration;
        }

        public getInstance(metsEditorParameter: MetsEditorParameter): MetsEditorModel {
            const metsEditorModel = new MetsEditorModel(this.metsEditorConfiguration);

            metsEditorModel.metsId = metsEditorParameter.metsId;
            metsEditorModel.targetServlet = metsEditorParameter.targetServletURL;
            this.metsModelLoaderService.load(metsEditorParameter.sourceMetsURL, (metsModel: MetsModel) => {
                metsEditorModel.onModelLoad(metsModel);
            });

            metsEditorModel.lockURL = metsEditorParameter.lockURL;
            metsEditorModel.unLockURL = metsEditorParameter.unLockURL;

            return metsEditorModel;
        }

    }
}
