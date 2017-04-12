namespace org.mycore.mets.controller {

    import MetsEditorModel = org.mycore.mets.model.MetsEditorModel;
    import MetsEditorModelFactory = org.mycore.mets.model.MetsEditorModelFactory;

    export class MetsEditorController {
        constructor(private $scope,
                    private metsEditorModelFactory: MetsEditorModelFactory,
                    private i18nModel,
                    hotkeys,
                    $timeout,
                    private metsModelLockService: org.mycore.mets.model.MetsModelLock,
                    private $modal,
                    private $window) {
            this.initHotkeys(hotkeys);

            $scope.$on("AddedSection", (event, data) => {
                $timeout(() => {
                    $scope.$broadcast("editSection", {
                        section : data.addedSection
                    });
                });

            });
        }

        private privateErrorModal;

        private initHotkeys(hotkeys) {
            hotkeys.add({
                combo : "ctrl+1",
                description : "",
                callback : () => {
                    this.model.mode = MetsEditorModel.EDITOR_PAGINATION;
                }
            });

            hotkeys.add({
                combo : "ctrl+2",
                description : "",
                callback : () => {
                    this.model.mode = MetsEditorModel.EDITOR_STRUCTURING;
                }
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
                    let options = {
                        templateUrl : "error/modal.html",
                        controller : "ErrorModalController",
                        size : "sm"
                    };

                    this.privateErrorModal = this.$modal.open(options);
                    this.privateErrorModal.errorModel = new org.mycore.mets.model.ErrorModalModel(
                        this.i18nModel.messages[ "noLockTitle" ] || "???noLockTitle???",
                        this.i18nModel.messages[ "noLockMessage" ] || "???noLockMessage???"
                    );

                    this.privateErrorModal.result.then(emptyCallback, emptyCallback);
                } else {
                    this.model.locked = true;

                    this.$window.onbeforeunload = () => {
                        this.metsModelLockService.unlock(this.model.unLockURL);
                        if (!this.model.stateEngine.isServerState()) {
                            return this.i18nModel.messages[ "notSaved" ];
                        }
                    };
                }
            });

        }

        public close() {
            this.$window.close();
        }

        public viewOptionClicked(option: string) {
            this.model.middleView = option;
        }


        private validate(parameter: MetsEditorParameter): void {
            this.checkParameter("metsId", parameter);
            this.checkParameter("sourceMetsURL", parameter);
            this.checkParameter("targetServletURL", parameter);

        }

        private checkParameter(parameterToCheck: string, parameters: MetsEditorParameter) {
            if (!(parameterToCheck in parameters) ||
                parameters[ parameterToCheck ] === null ||
                typeof parameters[ parameterToCheck ] === "undefined") {
                throw `MetsEditorParameter does not have a valid ${parameterToCheck}`;
            }
        }

        private model: MetsEditorModel;
    }

}

