System.register(['@angular/core', '../service/rest.service'], function(exports_1, context_1) {
    "use strict";
    var __moduleName = context_1 && context_1.id;
    var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
        var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
        if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
        else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
        return c > 3 && r && Object.defineProperty(target, key, r), r;
    };
    var __metadata = (this && this.__metadata) || function (k, v) {
        if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
    };
    var core_1, rest_service_1;
    var WebCliQueueComponent;
    return {
        setters:[
            function (core_1_1) {
                core_1 = core_1_1;
            },
            function (rest_service_1_1) {
                rest_service_1 = rest_service_1_1;
            }],
        execute: function() {
            let WebCliQueueComponent = class WebCliQueueComponent {
                constructor(_restService) {
                    this._restService = _restService;
                    this.currentQueue = new Array();
                    this._restService.currentQueue.subscribe(queue => {
                        if (queue != undefined) {
                            this.currentQueue = queue;
                        }
                    });
                }
            };
            WebCliQueueComponent = __decorate([
                core_1.Component({
                    selector: '[web-cli-queue]',
                    template: `
    <div class="col-lg-12 web-cli-queue">
      <pre class="web-cli-pre">
        <template ngFor let-command [ngForOf]="currentQueue" let-i="index"><!--
        --><br *ngIf="i != 0">{{command}}<!--
        --></template><!--
      --></pre>
    </div>
  `
                }), 
                __metadata('design:paramtypes', [rest_service_1.RESTService])
            ], WebCliQueueComponent);
            exports_1("WebCliQueueComponent", WebCliQueueComponent);
        }
    }
});
