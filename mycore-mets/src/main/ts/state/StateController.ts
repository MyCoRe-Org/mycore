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

///<reference path="StateEngine.ts"/>

namespace org.mycore.mets.model.state {
    export class MetsEditorStateController {
        private stateEngine: StateEngine;

        constructor(private $modal: any, public i18nModel: any, hotkeys: any) {
            this.initHotkeys(hotkeys);
        }

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
                templateUrl : 'state/changeListModal.html',
                controller : 'MetsEditorChangeListController',
                size : 'lg'
            };
            const modal = this.$modal.open(options);
            modal.changes = this.stateEngine.getLastChanges();

            const emptyCallback = () => {/* */
            };

            modal.result.then(emptyCallback, emptyCallback);
        }

        private initHotkeys(hotkeys: any) {
            hotkeys.add({
                combo : 'ctrl+z',
                description : '',
                callback : () => {
                    this.backClicked();
                }
            });
            hotkeys.add({
                combo : 'command+z',
                description : '',
                callback : () => {
                    this.backClicked();
                }
            });
            hotkeys.add({
                combo : 'cmd+shift+z',
                description : '',
                callback : () => {
                    this.nextClicked();
                }
            });
            hotkeys.add({
                combo : 'command+shift+z',
                description : '',
                callback : () => {
                    this.nextClicked();
                }
            });
        }
    }
}
