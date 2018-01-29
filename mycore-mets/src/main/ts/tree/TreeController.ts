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

///<reference path="../model/simple/MCRMetsSection.ts"/>
///<reference path="MetsEditorTreeModel.ts"/>
///<reference path="../state/StateEngine.ts"/>
///<reference path="../model/simple/MCRMetsSimpleModel.ts"/>
///<reference path="../model/MetsEditorModel.ts"/>
///<reference path="../error/ErrorModalModel.ts"/>
///<reference path="../state/SectionMoveChange.ts"/>

import MCRMetsSection = org.mycore.mets.model.simple.MCRMetsSection;
import MetsEditorTreeModel = org.mycore.mets.model.MetsEditorTreeModel;
import DropTarget = org.mycore.mets.model.DropTarget;
import StateEngine = org.mycore.mets.model.state.StateEngine;
import MCRMetsSimpleModel = org.mycore.mets.model.simple.MCRMetsSimpleModel;
import MetsEditorModel = org.mycore.mets.model.MetsEditorModel;

namespace org.mycore.mets.controller {
    /**
     * The MCRTreeController is used to display a tree of elements which can be sorted.
     * You nee to call the init method.
     */
    export class MCRTreeController {
        public treeModel: MetsEditorTreeModel;
        private privateChildFieldName: string;
        private privateParentFieldName: string;
        private privateErrorModal: any;
        private messageModel: any;
        private metsConfiguration: MetsEditorConfiguration;
        private stateEngine: StateEngine;
        private simpleModel: MCRMetsSimpleModel;
        private editorModel: MetsEditorModel;

        constructor(private $scope: any,
                    i18nModel: any,
                    ngDraggable: any,
                    private $modal: any,
                    editorConfiguration: MetsEditorConfiguration) {
            this.treeModel = new MetsEditorTreeModel();
            this.messageModel = i18nModel;
            this.metsConfiguration = editorConfiguration;
        }

        public init(root: any, childFieldName: string, parentFieldName: string, metsEditorModel: MetsEditorModel) {
            if (typeof root === 'undefined' || root === null) {
                throw new Error(`invalid
                root
                parameter
                ${root}
            `);
            } else if (typeof childFieldName === 'undefined' || childFieldName === null || childFieldName === '') {
                throw new Error(`invalid
                childFieldName
                parameter
                ${childFieldName}
            `);
            } else if (typeof parentFieldName === 'undefined' || parentFieldName === null || parentFieldName === '') {
                throw new Error(`invalid
                parentFieldName
                parameter
                ${parentFieldName}
            `);
            }

            this.treeModel.root = root;
            this.privateChildFieldName = childFieldName;
            this.privateParentFieldName = parentFieldName;
            this.stateEngine = metsEditorModel.stateEngine;
            this.simpleModel = metsEditorModel.metsModel;
            this.editorModel = metsEditorModel;
        }

        public getChildren(child: any) {
            const childList = child[ this.privateChildFieldName ];
            return childList;
        }

        public getChildrenCount(child: any) {
            const length = this.getChildren(child).length;
            return length;
        }

        public clickFolder(element: any, event: JQueryMouseEventObject) {
            event.stopImmediatePropagation();
            event.stopPropagation();
            this.treeModel.setElementOpen(element, !this.treeModel.getElementOpen(element));
        }

        public buildDropTarget(element: any, position: any): DropTarget {
            return new DropTarget(element, position);
        }

        public dropSuccess(target: DropTarget, sourceElement: MCRMetsSection, event: JQueryEventObject) {
            let realTargetElement;

            if (target.position === 'after' || target.position === 'before') {
                realTargetElement = target.element[ this.privateParentFieldName ];
            } else {
                realTargetElement = target.element;
            }

            if (target.element === sourceElement || realTargetElement === sourceElement) {
                return true;
            }

            if (!this.checkConsistent(realTargetElement, sourceElement) && target.element) {
                const options = {
                    templateUrl : 'error/modal.html',
                    controller : 'ErrorModalController',
                    size : 'lg'
                };

                this.privateErrorModal = this.$modal.open(options);
                this.privateErrorModal.errorModel = new org.mycore.mets.model.ErrorModalModel(
                    this.messageModel.messages.errorMoveChildTitle || '???errorMoveChildTitle???',
                    this.messageModel.messages.errorMoveChildMessage || '???errorMoveChildMessage???',
                    this.metsConfiguration.resources + 'img/move_parent_to_child.png'
                );
                const emptyCallback = () => {
                    // do nothing
                };
                this.privateErrorModal.result.then(emptyCallback, emptyCallback);

                return true;
            }

            const sectionMoveChange = new org.mycore.mets.model.state.SectionMoveChange(sourceElement, target);
            this.stateEngine.changeModel(sectionMoveChange);

            return true;
        }

        private checkConsistent(path: any, source: any) {
            if (path[ this.privateParentFieldName ] === source) {
                return false;
            } else {
                return path[ this.privateParentFieldName ] === null || this.checkConsistent(path[ this.privateParentFieldName ], source);
            }
        }
    }
}
