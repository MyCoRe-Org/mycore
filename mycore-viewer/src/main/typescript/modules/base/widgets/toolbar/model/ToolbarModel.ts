/// <reference path="../../../Utils.ts" />
/// <reference path="ToolbarComponent.ts" />
/// <reference path="ToolbarGroup.ts" />

namespace mycore.viewer.widgets.toolbar {

    export class ToolbarModel extends widgets.toolbar.ToolbarGroup implements ContainerObserver<ToolbarGroup, ToolbarComponent> {

        constructor(name: string) {
            super(name);
            this._children = new MyCoReMap<string, ToolbarGroup>();
            this._groupObserverArray = new Array<ContainerObserver<ToolbarModel, ToolbarGroup>>();
        }

        private _children: MyCoReMap<string, ToolbarGroup>;
        private _groupObserverArray: Array<ContainerObserver<ToolbarModel, ToolbarGroup>>;

        /**
         * Adds a group to the Toolbar and redirect to global observer. 
         */
        public addGroup(group: ToolbarGroup) {
            if (this._children.has(group.name)) {
                throw new Error("Group : " + group.name + " already exists in " + this.name);
            } else {
                this._children.set(group.name, group);
                group.addObserver(this);
                this.notifyGroupAdded(this, group);
            }
        }

        /**
         * Removes a group from the Toolbar and stop redirecting to global observers.
         */
        public removeGroup(group: ToolbarGroup) {
            if (this._children.has(group.name)) {
                this._children.remove(group.name);
                group.removeObserver(this);
                this.notifyGroupRemoved(this, group);
            } else {
                throw new Error("Group : " + group.name + " doesnt exists in " + this.name);
            }
        }

        /**
         * Receives a previous added group from the Toolbar.
         */
        public getGroup(name: string): ToolbarGroup {
            return this._children.get(name);
        }

        /**
         * Receives a list of all previous added groups (id).
         */
        public getGroupIDs() {
            return this._children.keys;
        }

        /**
         * Receives a list of all previous added groups 
         */
        public getGroups() {
            return this._children.values;
        }

        /**
         * [DO NOT CALL THIS] (Protected)
         * Called by the Groups if a ToolbarComponent is added to a group, because Toolbar is registered as a Observer.
         */
        public childAdded(group: ToolbarGroup, component: ToolbarComponent): void {
            this.notifyObserverChildAdded(group, component);
        }

        /**
         * [DO NOT CALL THIS] (Protected)
         * Called by the Groups if a Toolbar Component is removed from a group, because Toolbar is registered as a Observer.
         */
        public childRemoved(group: ToolbarGroup, component: ToolbarComponent): void {
            this.notifyObserverChildRemoved(group, component);
        }

        /**
         * Adds a Observer that will be called if a group add/remove a ToolbarComponent
         */
        public addGroupObserver(observer: ContainerObserver<ToolbarModel, ToolbarGroup>) {
            this._groupObserverArray.push(observer);
        }


        /**
         * removes a previous added Observer.
         */
        public removeGroupObserver(observer: ContainerObserver<ToolbarModel, ToolbarGroup>) {
            var index = this._groupObserverArray.indexOf(observer);
            this._groupObserverArray.splice(index, 1);
        }

        private notifyGroupAdded(toolbar: ToolbarModel, group: ToolbarGroup) {
            this._groupObserverArray.forEach(function(elem) {
                elem.childAdded(toolbar, group);
            });
        }

        private notifyGroupRemoved(toolbar: ToolbarModel, group: ToolbarGroup) {
            this._groupObserverArray.forEach(function(elem) {
                elem.childRemoved(toolbar, group);
            });
        }

        public addComponent(component: ToolbarComponent) {
            throw "Not added jet";
        }

        /**
         * Removes a IviewComponent from this Group and notify all registered observers.
         */
        public removeComponent(component: ToolbarComponent) {
            throw "Not added jet";
        }

    }



}