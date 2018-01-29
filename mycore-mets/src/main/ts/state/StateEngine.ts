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

namespace org.mycore.mets.model.state {
    export class StateEngine {
        private privateLastChanges: ModelChange[] = [];
        private privateRevertedChanges: ModelChange[] = [];
        private serverState: ModelChange = null;

        public getLastChanges(): ModelChange[] {
            return this.privateLastChanges.slice(0);
        }

        public getRevertedChanges(): ModelChange[] {
            return this.privateRevertedChanges.slice(0);
        }

        public changeModel(change: ModelChange) {
            this.clearRevertedChanges();
            change.doChange();
            this.privateLastChanges.push(change);
        }

        public back() {
            if (!this.canBack()) {
                throw new Error('privateLastChanges is empty!');
            } else {
                const lastChange = this.privateLastChanges.pop();
                lastChange.unDoChange();
                this.privateRevertedChanges.push(lastChange);
            }
        }

        public canBack() {
            return this.privateLastChanges.length > 0;
        }

        public forward() {
            if (!this.canForward()) {
                throw new Error('privateRevertedChanges is empty!');
            } else {
                const lastRevertedChange = this.privateRevertedChanges.pop();
                lastRevertedChange.doChange();
                this.privateLastChanges.push(lastRevertedChange);
            }
        }

        public canForward() {
            return this.privateRevertedChanges.length > 0;
        }

        public markServerState() {
            this.serverState = this.getLastChange();
        }

        public isServerState() {
            return this.serverState === this.getLastChange();
        }

        private getLastChange() {
            const lastChanges = this.getLastChanges();
            if (lastChanges.length === 0) {
                return null;
            }
            return lastChanges[ lastChanges.length - 1 ];
        }

        private clearRevertedChanges() {
            if (this.privateRevertedChanges.length > 0) {
                this.privateRevertedChanges = [];
            }
        }
    }
}
