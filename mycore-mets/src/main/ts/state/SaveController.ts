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

///<reference path="../model/MetsModelSaveService.ts"/>
///<reference path="../model/MetsEditorModel.ts"/>
namespace org.mycore.mets.model.state {

    import MetsModelSave = org.mycore.mets.model.MetsModelSave;

    export class SaveController {

        private metsEditorModel: MetsEditorModel;

        constructor(public i18nModel: any, private saveService: MetsModelSave) {

        }

        public init(editorModel: MetsEditorModel) {
            this.metsEditorModel = editorModel;
        }

        public canSave() {
            return !this.metsEditorModel.stateEngine.isServerState();
        }

        public saveClicked() {
            this.saveService.save(this.metsEditorModel.targetServlet, this.metsEditorModel.metsModel,
                (success: boolean) => {
                    if (success) {
                        this.metsEditorModel.stateEngine.markServerState();
                        alert(this.i18nModel.messages.save.success);
                    } else {
                        alert(this.i18nModel.messages.save.fail);
                    }
                });
        }

    }
}
