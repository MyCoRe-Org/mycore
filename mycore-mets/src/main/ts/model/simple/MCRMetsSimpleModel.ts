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

///<reference path="MCRMetsSection.ts"/>
///<reference path="MCRMetsPage.ts"/>

namespace org.mycore.mets.model.simple {
    export class MCRMetsSimpleModel {

        constructor(public rootSection: MCRMetsSection,
                    public metsPageList: MCRMetsPage[]) {
        }

        public static fromJson(json: any): MCRMetsSimpleModel {
            const serializedJson = (typeof json !== 'object') ? JSON.parse(json) : json;

            const idPageMap = {};
            const idFileMap = {};
            const idChapterMap = {};

            const metsPageList = serializedJson.metsPageList.map((pageObject, index) => {
                const files = pageObject.fileList.map((fileObject: MCRMetsFile) => {
                    return new MCRMetsFile(fileObject.id, fileObject.href, fileObject.mimeType, fileObject.use);
                });
                const mcrMetsPage = new MCRMetsPage(pageObject.id,
                    pageObject.orderLabel || null, pageObject.contentIds || null,
                    pageObject.hidden || false,
                    files);
                mcrMetsPage.fileList.forEach((file: MCRMetsFile) => {
                    idFileMap[ file.id ] = file;
                });
                idPageMap[ pageObject.id ] = mcrMetsPage;
                return mcrMetsPage;
            });

            const metsRootSection = MCRMetsSimpleModel.createSection(serializedJson.rootSection, idChapterMap, idFileMap);
            serializedJson.sectionPageLinkList.forEach((linkObject) => {
                const page = idPageMap[ linkObject.to ];
                const section = idChapterMap[ linkObject.from ];
                section.linkedPages.push(page);
            });
            return new MCRMetsSimpleModel(metsRootSection, metsPageList);
        }

        public static toJson(model: MCRMetsSimpleModel) {
            const sectionIdPageMap = [];
            const addIdsToSectionMap = (section: MCRMetsSection) => {
                section.metsSectionList.forEach(addIdsToSectionMap);
                section.linkedPages.forEach((lp: MCRMetsPage) => {
                    sectionIdPageMap.push({from : section.id, to : lp.id});
                });
            };
            addIdsToSectionMap(model.rootSection);

            const pageList = model.metsPageList.map((p: MCRMetsPage) => MCRMetsPage.copy(p));
            const root = model.rootSection.getJsonObject();

            return JSON.stringify({
                sectionPageLinkList : sectionIdPageMap,
                metsPageList : pageList,
                rootSection : root
            });
        }

        private static createSection(object: any, idChapterMap: any, idFileMap: any): MCRMetsSection {
            const section = new MCRMetsSection(object.id, object.type, object.label);
            idChapterMap[ section.id ] = section;

            object.metsSectionList.forEach((childObject) => {
                const child = MCRMetsSimpleModel.createSection(childObject, idChapterMap, idFileMap);
                section.metsSectionList.push(child);
                child.parent = section;
            });

            if ('altoLinks' in object) {
                object.altoLinks.forEach((altoLink) => {
                    if (altoLink.altoFile === null || typeof altoLink.altoFile === 'undefined' || !(altoLink.altoFile in idFileMap)
                        || altoLink.begin === null || typeof altoLink.begin === 'undefined' || altoLink.end === null ||
                        typeof altoLink.end === 'undefined') {
                        console.warn('invalid alto-link ');
                        console.warn(altoLink);
                        return;
                    }
                    const al = new MCRMetsAltoLink(idFileMap[ altoLink.altoFile ], altoLink.begin, altoLink.end);
                    section.altoLinks.push(al);
                });
            }
            return section;
        }
    }

}
