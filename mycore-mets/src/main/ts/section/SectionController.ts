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

///<reference path="../model/MetsEditorModel.ts"/>
///<reference path="../model/simple/MCRMetsSection.ts"/>
///<reference path="../model/simple/MCRMetsSimpleModel.ts"/>
///<reference path="../model/DFGStructureSet.ts"/>
///<reference path="../model/simple/MCRMetsPage.ts"/>
///<reference path="../state/StateEngine.ts"/>
///<reference path="../state/SectionTypeChange.ts"/>
///<reference path="../state/SectionLabelChange.ts"/>
///<reference path="../state/SectionDeleteChange.ts"/>
///<reference path="../state/SectionAddChange.ts"/>
///<reference path="../state/RemoveSectionLinkChange.ts"/>
///<reference path="../state/ModelChange.ts"/>
///<reference path="../state/AddSectionLinkChange.ts"/>
///<reference path="../state/BatchChange.ts"/>
///<reference path="SectionEvents.ts"/>

namespace org.mycore.mets.controller {

    import MetsEditorModel = org.mycore.mets.model.MetsEditorModel;

    import MCRMetsSection = org.mycore.mets.model.simple.MCRMetsSection;
    import MCRMetsSimpleModel = org.mycore.mets.model.simple.MCRMetsSimpleModel;
    import StructureSetElement = org.mycore.mets.model.StructureSetElement;
    import MCRMetsPage = org.mycore.mets.model.simple.MCRMetsPage;
    import StateEngine = org.mycore.mets.model.state.StateEngine;
    import SectionTypeChange = org.mycore.mets.model.state.SectionTypeChange;
    import SectionLabelChange = org.mycore.mets.model.state.SectionLabelChange;
    import SectionDeleteChange = org.mycore.mets.model.state.SectionDeleteChange;
    import SectionAddChange = org.mycore.mets.model.state.SectionAddChange;
    import RemoveSectionLinkChange = org.mycore.mets.model.state.RemoveSectionLinkChange;
    import ModelChange = org.mycore.mets.model.state.ModelChange;
    import AddSectionLinkChange = org.mycore.mets.model.state.AddSectionLinkChange;
    import BatchChange = org.mycore.mets.model.state.BatchChange;

    /**
     * The SectionController can be used to display and edit MCRMetsSection.
     */
    export class SectionController {
        public section: MCRMetsSection = null;
        public edit: any = {type : '', label : null};
        private i18nModel: any;
        private structureSet: StructureSetElement[];
        private stateEngine: StateEngine = null;
        private simpleModel: MCRMetsSimpleModel;
        private editorModel: MetsEditorModel;

        constructor(private $scope: any, i18nModel: any, structureSet: StructureSetElement[], private $timeout: any) {
            this.structureSet = structureSet;
            this.i18nModel = i18nModel;

            $scope.$on('editSection', (event, data) => {
                if (data.section === this.section) {
                    this.startEditLabel();
                }
            });

            this.registerEventHandler($scope);
        }

        public init(section: MCRMetsSection, editorModel: MetsEditorModel) {
            if (typeof section === 'undefined' || !(section instanceof MCRMetsSection)) {
                throw new Error(`section is invalid : ${section}`);
            }

            this.editorModel = editorModel;
            this.simpleModel = editorModel.metsModel;
            this.stateEngine = editorModel.stateEngine;
            this.section = section;
        }

        public editTypeChange() {
            const newType = this.edit.type;
            const sectionTypeChange = new SectionTypeChange(this.section, newType, this.section.type);
            this.stateEngine.changeModel(sectionTypeChange);
        }

        public editLabelKeyUp(keyEvent: JQueryKeyEventObject) {
            switch (keyEvent.keyCode) {
                case 13: // enter
                    const sectionLabelChange = new SectionLabelChange(this.section, this.edit.label, this.section.label);
                    this.stateEngine.changeModel(sectionLabelChange);
                    this.edit.label = null;
                    break;
                case 27: // esc
                    this.edit.label = null;
                    break;
                default :
            }
        }

        public clickEditTreeLabel(clickEvent: JQueryMouseEventObject) {
            this.startEditLabel();
        }

        public clickDeleteSection(clickEvent: JQueryMouseEventObject) {
            clickEvent.stopPropagation();

            this.stateEngine.changeModel(new SectionDeleteChange(this.section));
        }

        public clickAddSection(clickEvent: JQueryMouseEventObject) {
            clickEvent.stopPropagation();

            const newCreatedSection = new MCRMetsSection(MCRMetsSection.createRandomId(), this.structureSet[ 0 ].id, 'new');
            const sectionAddChange = new SectionAddChange(newCreatedSection, this.section);
            this.stateEngine.changeModel(sectionAddChange);
            this.$scope.$emit('AddedSection', {
                parent : this.section,
                addedSection : newCreatedSection
            });
        }

        public inputClicked(clickEvent: JQueryMouseEventObject) {
            /* needed because event will bubble to drag&drop handler*/
            clickEvent.stopPropagation();
            clickEvent.stopImmediatePropagation();
        }

        public isDeletable() {
            return this.section !== null && typeof this.section !== 'undefined' && this.section.parent !== null;
        }

        public getPageChildren() {
            const indexLookup = {};
            this.section.linkedPages.forEach((element) => {
                indexLookup[ element.id ] = this.simpleModel.metsPageList.indexOf(element);
            });

            return this.section.linkedPages.sort((page1, page2) => {
                return indexLookup[ page1.id ] - indexLookup[ page2.id ];
            });
        }

        public hasPageChildren() {
            return this.section.linkedPages.length > 0;
        }

        public removeLink(section: MCRMetsSection, page: MCRMetsPage, clickEvent: JQueryMouseEventObject) {
            clickEvent.preventDefault();
            clickEvent.stopPropagation();

            this.stateEngine.changeModel(new RemoveSectionLinkChange(section, page));
        }

        public canLink() {
            return this.getNotLinkedFromSelection(this.section).length > 0;
        }

        public canUnlink() {
            return true;
        }

        public childHasLink(curChild: MCRMetsSection, page: MCRMetsPage) {
            const thisHasChild = curChild.linkedPages.indexOf(page) !== -1;

            if (thisHasChild) {
                return true;
            }

            for (const cur in curChild.metsSectionList) {
                if (this.childHasLink(curChild.metsSectionList[ cur ], page)) {
                    return true;
                }
            }

            return false;
        }

        public link() {
            let change: ModelChange;

            const changes = this.getNotLinkedFromSelection(this.section).map((page) => {
                return new AddSectionLinkChange(this.section, page);
            });

            if (changes.length === 1) {
                change = changes[ 0 ];
            } else if (changes.length > 1) {
                change = new BatchChange(changes);
            } else {
                return;
            }

            this.stateEngine.changeModel(change);
        }

        public getNotLinkedFromSelection(section: MCRMetsSection) {
            const hasLink = (s: MCRMetsSection, p) => {
                const linkedPageIndex = s.linkedPages.indexOf(p);
                if (linkedPageIndex >= 0) {
                    return linkedPageIndex >= 0;
                }
                for (const cs of s.metsSectionList) {
                    const has = hasLink(cs, p);
                    if (has) {
                        return true;
                    }
                }
                return false;
            };

            return (this.editorModel.pageSelection.from !== null) ?
                this.simpleModel.metsPageList
                    .slice(this.editorModel.pageSelection.from, this.editorModel.pageSelection.to + 1)
                    .filter((p) => !hasLink(section, p))
                : [];
        }

        public getAlternateOrderlabel(page: MCRMetsPage) {
            return this.editorModel.metsModel.metsPageList.indexOf(page) + 1;
        }

        private startEditLabel() {
            this.edit.label = this.section.label || this.i18nModel.noOrderLabel;
            this.$scope.$emit('EditLabelStart', {
                ofSection : this.section
            });
        }

        private registerEventHandler($scope: any) {
            $scope.$on('startEditLabel', (event: any, data: StartEditLabel) => {
                if (data.ofSection === this.section) {
                    this.startEditLabel();
                }
            });
        }

        private editInit($event: any) {
            /**/
        }
    }
}
