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
    var WebCliLogComponent;
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
            let WebCliLogComponent = class WebCliLogComponent {
                constructor(_restService, _comunicationService) {
                    this._restService = _restService;
                    this._comunicationService = _comunicationService;
                    this.log = new Array();
                    this.settings = new settings_1.Settings(50, true);
                    this._comunicationService.settings.subscribe(settings => {
                        this.settings = settings;
                    });
                    this._restService.currentLog.subscribe(log => {
                        if (log != undefined) {
                            this.log.push(log);
                            this.log = this.log.splice(this.settings.historySize * -1, this.settings.historySize);
                        }
                    });
                }
                clearLog() {
                    this.log.splice(0, this.log.length);
                }
                ngAfterViewChecked() {
                    this.scrollLog();
                }
                scrollLog() {
                    if (this.settings.autoscroll) {
                        var elem = document.getElementsByClassName('web-cli-log-pre');
                        elem[0].scrollTop = elem[0].scrollHeight;
                    }
                }
            };
            WebCliLogComponent = __decorate([
                core_1.Component({
                    selector: '[web-cli-log]',
                    template: `
    <div class="col-xs-12 web-cli-log">
      <pre class="web-cli-log-pre web-cli-pre">
        <template ngFor let-logEntry [ngForOf]="log" let-i="index"><!--
        --><br *ngIf="i != 0">{{logEntry.logLevel}}: {{logEntry.message}}<!--
        --><br *ngIf="logEntry.exception">{{logEntry.exception}}<!--
        --></template><!--
      --></pre>
    </div>
  `
                }), 
                __metadata('design:paramtypes', [rest_service_1.RESTService, communication_service_1.CommunicationService])
            ], WebCliLogComponent);
            exports_1("WebCliLogComponent", WebCliLogComponent);
        }
    }
});
