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

///<reference path="ModelChange.ts"/>
///<reference path="../model/simple/MCRMetsSection.ts"/>

namespace org.mycore.mets.model.state {
    export class SectionLabelChange extends ModelChange {
        private from: string;

        constructor(private section: simple.MCRMetsSection, private to: string, from?: string) {
            super();
            this.from = from || this.section.label;
        }

        public doChange() {
            this.section.label = this.to;
        }

        public unDoChange() {
            this.section.label = this.from;
        }

        public getDescription(messages: any): string {
            const description = messages.SectionLabelChangeDescription || '???SectionLabelChangeDescription??? {from} {to}';
            return description.replace('{from}', this.from).replace('{to}', this.to);
        }
    }
}
