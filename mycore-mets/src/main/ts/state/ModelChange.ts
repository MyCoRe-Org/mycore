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

namespace org.mycore.mets.model.state {
    export class ModelChange {
        private id: string;
        private date: Date;

        constructor() {
            this.id = this.createRandomId();
            this.date = new Date();
        }

        public doChange(): void {
            throw new Error('doChange is not implemened!');
        }

        public unDoChange(): void {
            throw new Error('unDoChange is not implemened!');
        }

        public getDescription(messages: any): string {
            throw new Error('getDescription is not implemened!');
        }

        public getDate() {
            return this.date;
        }

        private createRandomId() {
            return 'nnnnnn-nnnn-nnnn-nnnnnnnn'.split('n').map((n) => n + Math.ceil(15 * Math.random()).toString(36)).join('');
        }
    }
}
