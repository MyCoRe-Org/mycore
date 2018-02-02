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

namespace org.mycore.mets.model {
    export class StructureSetElement {

        public static DFG_STRUCTURE_SET: StructureSetElement[] = (() => {
            return [
                new StructureSetElement('section'),
                new StructureSetElement('annotation'),
                new StructureSetElement('bachelor_thesis'),
                new StructureSetElement('address'),
                new StructureSetElement('article'),
                new StructureSetElement('volume'),
                new StructureSetElement('contained_work'),
                new StructureSetElement('additional'),
                new StructureSetElement('report'),
                new StructureSetElement('provenance'),
                new StructureSetElement('collation'),
                new StructureSetElement('ornament'),
                new StructureSetElement('letter'),
                new StructureSetElement('cover'),
                new StructureSetElement('- cover_front'),
                new StructureSetElement('- cover_back'),
                new StructureSetElement('diploma_thesis'),
                new StructureSetElement('doctoral_thesis'),
                new StructureSetElement('printers_mark'),
                new StructureSetElement('binding'),
                new StructureSetElement('entry'),
                new StructureSetElement('corrigenda'),
                new StructureSetElement('bookplate'),
                new StructureSetElement('fascicle'),
                new StructureSetElement('research_paper'),
                new StructureSetElement('fragment'),
                new StructureSetElement('habilitation_thesis'),
                new StructureSetElement('manuscript'),
                new StructureSetElement('issue'),
                new StructureSetElement('illustration'),
                new StructureSetElement('imprint'),
                new StructureSetElement('contents'),
                new StructureSetElement('initial_decoration'),
                new StructureSetElement('year'),
                new StructureSetElement('chapter'),
                new StructureSetElement('map'),
                new StructureSetElement('colophon'),
                new StructureSetElement('engraved_titlepage'),
                new StructureSetElement('magister_thesis'),
                new StructureSetElement('master_thesis'),
                new StructureSetElement('multivolume_work'),
                new StructureSetElement('month'),
                new StructureSetElement('monograph'),
                new StructureSetElement('musical_notation'),
                new StructureSetElement('periodical'),
                new StructureSetElement('privileges'),
                new StructureSetElement('index'),
                new StructureSetElement('spine'),
                new StructureSetElement('scheme'),
                new StructureSetElement('edge'),
                new StructureSetElement('paste_down'),
                new StructureSetElement('stamp'),
                new StructureSetElement('study'),
                new StructureSetElement('table'),
                new StructureSetElement('day'),
                new StructureSetElement('proceeding'),
                new StructureSetElement('text'),
                new StructureSetElement('title_page'),
                new StructureSetElement('verse'),
                new StructureSetElement('preprint'),
                new StructureSetElement('lecture'),
                new StructureSetElement('endsheet'),
                new StructureSetElement('paper'),
                new StructureSetElement('preface'),
                new StructureSetElement('dedication'),
                new StructureSetElement('newspaper') ];
        })();

        constructor(public id: string) {
        }

    }

}
