///<reference path="ModelChange.ts"/>

namespace org.mycore.mets.controller {

    export class MetsEditorChangeListController {
        constructor($scope, $modalInstance, public i18nModel) {
            $scope.ctrl = this;
            this.modalInstance = $modalInstance;
        }

        private modalInstance: any;

        public get changes(): Array<model.state.ModelChange> {
            return this.modalInstance.changes;
        }


        public closeClicked() {
            this.modalInstance.close({});
        }
    }

}



