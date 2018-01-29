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

///<reference path='model/simple/MCRMetsSimpleModel.ts'/>
///<reference path='model/MetsModelLoaderService.ts'/>
///<reference path='model/MetsModelSaveService.ts'/>

///<reference path='model/utils/IndexSet.ts'/>
///<reference path='model/utils/Edit.ts'/>

///<reference path='model/MetsEditorI18NModel.ts'/>
///<reference path='model/MetsEditorModel.ts'/>

///<reference path='model/DFGStructureSet.ts'/>

///<reference path='model/MetsEditorConfiguration.ts'/>
///<reference path='model/MetsEditorParameter.ts'/>
///<reference path='model/MetsEditorModelFactory.ts'/>

///<reference path='tree/MetsEditorTreeModel.ts'/>
///<reference path='error/ErrorModalModel.ts'/>
///<reference path='pageList/PaginationModalModel.ts'/>

///<reference path='model/Pagination.ts'/>

///<reference path='state/StateEngine.ts'/>
///<reference path='state/ModelChange.ts'/>

///<reference path='state/SectionLabelChange.ts'/>
///<reference path='state/SectionTypeChange.ts'/>
///<reference path='state/SectionAddChange.ts'/>
///<reference path='state/SectionDeleteChange.ts'/>
///<reference path='state/SectionMoveChange.ts'/>
///<reference path='state/PageLabelChange.ts'/>
///<reference path='state/BatchChange.ts'/>
///<reference path='state/PagesMoveChange.ts'/>
///<reference path='state/RemoveSectionLinkChange.ts'/>
///<reference path='state/AddSectionLinkChange.ts'/>

///<reference path='image/PagePresenter.ts'/>
///<reference path='MetsEditorController.ts'/>
///<reference path='tree/TreeController.ts'/>
///<reference path='section/SectionController.ts'/>
///<reference path='section/SectionEvents.ts'/>
///<reference path='error/ErrorModalController.ts'/>

///<reference path='pageList/PageController.ts'/>
///<reference path='pageList/PageListController.ts'/>

///<reference path='pageList/PaginationController.ts'/>
///<reference path='pageList/PaginationModalController.ts'/>

///<reference path='state/StateController.ts'/>
///<reference path='state/ChangeListController.ts'/>

///<reference path='state/SaveController.ts'/>
///<reference path='tree/NotLinkedController.ts'/>
///<reference path='model/MetsModelLockService.ts'/>
angular.module('MetsEditorApp',
    [ 'MetsEditorI18NModel',
        'fa.directive.borderLayout',
        'MetsEditorTemplates',
        'ngDraggable',
        'ui.bootstrap',
        'ErrorModal',
        'MetsEditorSectionModule',
        'MetsEditorModelFactory',
        'MetsModelSaveService',
        'MetsEditorPresenterModule',
        'ng-image-zoom',
        'cfp.hotkeys',
        'afkl.lazyImage',
        'MetsModelLockService'
    ]);

angular.module('MetsModelLoaderService', []).service('MetsModelLoaderService',
    [ '$http', ($http) => new org.mycore.mets.model.MetsModelLoader($http) ]);

angular.module('MetsModelSaveService', []).service('MetsModelSaveService',
    [ '$http', ($http) => {
        return new org.mycore.mets.model.MetsModelSave($http);
    } ]);
angular.module('MetsEditorI18NModel', [ 'MetsEditorConfiguration' ])
    .factory('MetsEditorI18NModel', [ '$http', '$location', '$log', 'MetsEditorConfiguration',
        org.mycore.mets.model.i18n ]);
angular.module('StructureSetConfiguration', []).config([ '$provide', ($provide) => {
    $provide.constant('StructureSet', org.mycore.mets.model.StructureSetElement.DFG_STRUCTURE_SET);
} ]);

angular.module('MetsEditorModelFactory', [ 'MetsModelLoaderService', 'MetsEditorConfiguration' ])
    .service('MetsEditorModelFactory',
        [ 'MetsModelLoaderService',
            'MetsEditorConfiguration',
            (modelLoader: org.mycore.mets.model.MetsModelLoader,
             editorConfiguration: MetsEditorConfiguration) =>
                new org.mycore.mets.model.MetsEditorModelFactory(modelLoader, editorConfiguration) ]
    );
angular.module('MetsEditorPresenterModule', [])
    .controller('DefaultPagePresenter', <any> org.mycore.mets.controller.DefaultPagePresenter);
angular.module('MetsEditorApp')
    .controller('MetsEditorTreeController',
        <any> [ '$scope',
            'MetsEditorI18NModel',
            'ngDraggable',
            '$modal',
            'MetsEditorConfiguration',
            org.mycore.mets.controller.MCRTreeController ]);

angular.module('MetsEditorSectionModule', [ 'MetsEditorI18NModel', 'StructureSetConfiguration' ]);
angular.module('MetsEditorSectionModule').controller('SectionController',
    <any> [
        '$scope',
        'MetsEditorI18NModel',
        'StructureSet',
        '$timeout',
        org.mycore.mets.controller.SectionController
    ]);

angular.module('MetsEditorApp').directive('selectImmediately', [ '$timeout', ($timeout) => {
    return {
        restrict : 'A',
        link : (scope, iElement: any) => {
            $timeout(() => { // Using timeout to let template to be appended to DOM prior to select action.
                iElement[ 0 ].select();
            });
        }
    };
} ]);

angular.module('MetsEditorApp').controller('MetsEditorController',
    <any> [ '$scope', 'MetsEditorModelFactory', 'MetsEditorI18NModel', 'hotkeys', '$timeout', 'MetsModelLockService',
        '$modal', '$window',
        org.mycore.mets.controller.MetsEditorController ]);

angular.module('ErrorModal', [ 'ui.bootstrap', 'MetsEditorI18NModel' ]).controller('ErrorModalController',
    <any> [ '$scope', '$modalInstance', 'MetsEditorI18NModel', org.mycore.mets.controller.ErrorModalController ]);

angular.module('MetsEditorApp');
angular.module('MetsEditorApp').controller('MetsEditorPageListController',
    <any> [ 'ngDraggable', '$timeout', '$modal', 'hotkeys', 'MetsEditorI18NModel', org.mycore.mets.controller.PageListController ]);

angular.module('MetsEditorApp').directive('jumpToElement', [ '$timeout',
    ($timeout: (callback: () => void) => void) => {
        return {
            restrict : 'A',
            link : (scope, iElement, attr) => {
                const findOverflowScrollParent = (elem) => {
                    const ngElement = elem;
                    if (ngElement.css('overflow') === 'scroll' || ngElement.css('overflow-y') === 'scroll') {
                        return ngElement;
                    } else if (ngElement.parent() !== null) {
                        return findOverflowScrollParent(ngElement.parent());
                    } else {
                        return null;
                    }
                };
                const jumpToElement = () => {
                    $timeout(() => { // Using timeout to let template to be appended to DOM prior to select action.
                        const elemToScrollTo = iElement;
                        const scrollParent = findOverflowScrollParent(iElement);
                        if (scrollParent !== null) {
                            const ngScrollParent = scrollParent;
                            const elementTop = elemToScrollTo.position().top;
                            const scrollTop = ngScrollParent[ 0 ].scrollTop;
                            let offset = -1 * (scrollTop - elementTop);

                            const parentHeight = ngScrollParent.height();

                            if (offset > parentHeight - elemToScrollTo.height()) {
                                offset -= (parentHeight - 2 * elemToScrollTo.height());
                            } else if (offset < parentHeight && offset > 0) {
                                return;
                            }
                            ngScrollParent[ 0 ].scrollTop = ngScrollParent[ 0 ].scrollTop + offset;
                        }
                    });
                };

                const checkAndJump = () => {
                    if ((<any> attr).jumpToElement === 'true') {
                        jumpToElement();
                    }
                };

                checkAndJump();
                attr.$observe('jumpToElement', () => {
                    checkAndJump();
                });
            }
        };
    } ]);

angular.module('MetsEditorApp').controller('MetsEditorPageController',
    <any> [ 'MetsEditorI18NModel', org.mycore.mets.controller.PageController ]);

angular.module('MetsEditorApp')
    .controller('MetsEditorPaginationController',
        <any> [ '$modal', 'MetsEditorI18NModel', 'hotkeys', org.mycore.mets.controller.PaginationController ]);

angular.module('MetsEditorApp')
    .controller('PaginationModalController',
        <any> [ '$scope', '$modalInstance', org.mycore.mets.controller.Pagination.PaginationModalController ]);

{
    const metsEditorStateController = angular.module('MetsEditorApp');
    metsEditorStateController.controller('MetsEditorStateController',
        <any> [ '$modal', 'MetsEditorI18NModel', 'hotkeys', org.mycore.mets.model.state.MetsEditorStateController ]);
}

angular.module('MetsEditorApp').controller('MetsEditorChangeListController',
    <any> [ '$scope', '$modalInstance', 'MetsEditorI18NModel', org.mycore.mets.controller.MetsEditorChangeListController ]);

{
    const saveController = angular.module('MetsEditorApp');
    saveController.controller('SaveController',
        <any> [ 'MetsEditorI18NModel', 'MetsModelSaveService', org.mycore.mets.model.state.SaveController ]);
}

angular.module('MetsEditorApp')
    .controller('NotLinkedController', <any> [ org.mycore.mets.controller.NotLinkedController ]);

angular.module('MetsModelLockService', []).service('MetsModelLockService',
    [ '$http', ($http) => new org.mycore.mets.model.MetsModelLock($http) ]);
