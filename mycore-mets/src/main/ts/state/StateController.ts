///<reference path="StateEngine.ts"/>

namespace org.mycore.mets.model.state {
    export class MetsEditorStateController {
        constructor(private $modal, public i18nModel, hotkeys) {
            this.initHotkeys(hotkeys);
        }

        private initHotkeys(hotkeys) {
            hotkeys.add({
                combo : "ctrl+z",
                description : "",
                callback : () => {
                    this.backClicked();
                }
            });
            hotkeys.add({
                combo : "command+z",
                description : "",
                callback : () => {
                    this.backClicked();
                }
            });
            hotkeys.add({
                combo : "cmd+shift+z",
                description : "",
                callback : () => {
                    this.nextClicked();
                }
            });
            hotkeys.add({
                combo : "command+shift+z",
                description : "",
                callback : () => {
                    this.nextClicked();
                }
            });
        }

        private stateEngine: StateEngine;

        public init(stateEngine: StateEngine) {
            this.stateEngine = stateEngine;
        }

        public backClicked() {
            if (this.canBack()) {
                this.stateEngine.back();
            }
        }

        public nextClicked() {
            if (this.stateEngine.canForward()) {
                this.stateEngine.forward();
            }
        }

        public canForward() {
            return this.stateEngine.canForward();
        }

        public canBack() {
            return this.stateEngine.canBack();
        }

        public listClicked() {
            const options = {
                templateUrl : "state/changeListModal.html",
                controller : "MetsEditorChangeListController",
                size : "lg"
            };
            const modal = this.$modal.open(options);
            modal.changes = this.stateEngine.getLastChanges();

            const emptyCallback = () => {/* */
            };

            modal.result.then(emptyCallback, emptyCallback);
        }
    }
}


