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

namespace org.mycore.mets.controller {

    import MetsEditorModel = org.mycore.mets.model.MetsEditorModel;
    import MetsEditorModelFactory = org.mycore.mets.model.MetsEditorModelFactory;
    import I18nModel = org.mycore.mets.model.I18nModel;
    import EditorMode = org.mycore.mets.model.EditorMode;
    import ViewOption = org.mycore.mets.model.ViewOptions;

    export class MetsEditorController {
        private privateErrorModal: any;
        private model: MetsEditorModel;

        constructor(private $scope: any,
                    private metsEditorModelFactory: MetsEditorModelFactory,
                    private i18nModel: I18nModel,
                    hotkeys: any,
                    $timeout: any,
                    private metsModelLockService: org.mycore.mets.model.MetsModelLock,
                    private $modal: any,
                    private $window: any) {
            this.initHotkeys(hotkeys);

            $scope.$on('AddedSection', (event, data) => {
                $timeout(() => {
                    $scope.$broadcast('editSection', {
                        section : data.addedSection
                    });
                });

            });
        }

        public init(parameter: MetsEditorParameter) {
            const emptyCallback = () => {
                // do nothing
            };
            this.validate(parameter);
            this.model = this.metsEditorModelFactory.getInstance(parameter);
            this.metsModelLockService.lock(this.model.lockURL, (success: boolean) => {
                if (!success) {
                    const options = {
                        templateUrl : 'error/modal.html',
                        controller : 'ErrorModalController',
                        size : 'sm'
                    };

                    this.privateErrorModal = this.$modal.open(options);
                    this.privateErrorModal.errorModel = new org.mycore.mets.model.ErrorModalModel(
                        this.i18nModel.messages.noLockTitle || '???noLockTitle???',
                        this.i18nModel.messages.noLockMessage || '???noLockMessage???'
                    );

                    this.privateErrorModal.result.then(emptyCallback, emptyCallback);
                } else {
                    this.model.locked = true;

                    this.$window.onbeforeunload = () => {
                        this.metsModelLockService.unlock(this.model.unLockURL);
                        if (!this.model.stateEngine.isServerState()) {
                            return this.i18nModel.messages.notSaved;
                        }
                    };
                }
            });

        }

        public close() {
            if (this.$window.history.length > 1) {
                this.$window.history.back();
            } else {
                this.$window.close();
            }
        }

        public viewOptionClicked(option: ViewOption) {
            this.model.middleView = option;
        }

        private initHotkeys(hotkeys: any) {
            hotkeys.add({
                combo : 'ctrl+1',
                description : '',
                callback : () => {
                    this.model.mode = EditorMode.Pagination;
                }
            });

            hotkeys.add({
                combo : 'ctrl+2',
                description : '',
                callback : () => {
                    this.model.mode = EditorMode.Structuring;
                }
            });

        }

        private validate(parameter: MetsEditorParameter): void {
            this.checkParameter('metsId', parameter);
            this.checkParameter('sourceMetsURL', parameter);
            this.checkParameter('targetServletURL', parameter);

        }

        private checkParameter(parameterToCheck: string, parameters: MetsEditorParameter) {
            if (!(parameterToCheck in parameters) ||
                parameters[ parameterToCheck ] === null ||
                typeof parameters[ parameterToCheck ] === 'undefined') {
                throw new Error(`MetsEditorParameter does not have a valid ${parameterToCheck}`);
            }
        }
    }

}
