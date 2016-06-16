System.register(['@angular/core', '../settings/settings', '../service/rest.service', '../service/communication.service'], function(exports_1, context_1) {
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
    var core_1, settings_1, rest_service_1, communication_service_1;
    var WebCliQueueComponent;
    return {
        setters:[
            function (core_1_1) {
                core_1 = core_1_1;
            },
            function (settings_1_1) {
                settings_1 = settings_1_1;
            },
            function (rest_service_1_1) {
                rest_service_1 = rest_service_1_1;
            },
            function (communication_service_1_1) {
                communication_service_1 = communication_service_1_1;
            }],
        execute: function() {
            let WebCliQueueComponent = class WebCliQueueComponent {
                constructor(_restService, _comunicationService) {
                    this._restService = _restService;
                    this._comunicationService = _comunicationService;
                    this.settings = new settings_1.Settings(50, true, false);
                    this._comunicationService.settings.subscribe(settings => {
                        this.settings = settings;
                    });
                    this._restService.currentQueue.subscribe(queue => {
                        let ellipsis = "";
                        if (queue.length > this.settings.historySize) {
                            queue = queue.slice(0, this.settings.historySize);
                            ellipsis = "</br>...";
                        }
                        let queueString = queue.join("</br>") + ellipsis;
                        document.getElementsByClassName('web-cli-pre')[0].innerHTML = queueString;
                    });
                }
            };
            WebCliQueueComponent = __decorate([
                core_1.Component({
                    selector: '[web-cli-queue]',
                    template: `
    <div class="col-lg-12 web-cli-queue">
      <pre class="web-cli-pre">
      </pre>
    </div>
  `
                }), 
                __metadata('design:paramtypes', [rest_service_1.RESTService, communication_service_1.CommunicationService])
            ], WebCliQueueComponent);
            exports_1("WebCliQueueComponent", WebCliQueueComponent);
        }
    }
});
