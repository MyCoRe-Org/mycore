System.register(['@angular/core', '../service/communication.service', '../service/rest.service', './settings'], function(exports_1, context_1) {
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
    var core_1, communication_service_1, rest_service_1, settings_1;
    var WebCliSettingsComponent;
    return {
        setters:[
            function (core_1_1) {
                core_1 = core_1_1;
            },
            function (communication_service_1_1) {
                communication_service_1 = communication_service_1_1;
            },
            function (rest_service_1_1) {
                rest_service_1 = rest_service_1_1;
            },
            function (settings_1_1) {
                settings_1 = settings_1_1;
            }],
        execute: function() {
            let WebCliSettingsComponent = class WebCliSettingsComponent {
                constructor(_communicationService, _restService) {
                    this._communicationService = _communicationService;
                    this._restService = _restService;
                }
                ngOnInit() {
                    this.settings = this.getSettingsFromCookie(500, 10, true, false);
                    this._communicationService.setSettings(this.settings);
                    this._restService.setContinueIfOneFails(this.settings.continueIfOneFails);
                }
                onHistoryChange() {
                    if (localStorage.getItem("historySize") != this.settings.historySize + "") {
                        localStorage.setItem("historySize", this.settings.historySize + "");
                    }
                }
                onComHistoryChange() {
                    if (localStorage.getItem("comHistorySize") != this.settings.comHistorySize + "") {
                        localStorage.setItem("comHistorySize", this.settings.comHistorySize + "");
                    }
                }
                onAutoScrollChange(event) {
                    if (localStorage.getItem("autoScroll") != event.srcElement.checked + "") {
                        localStorage.setItem("autoScroll", event.srcElement.checked);
                    }
                }
                onContinueIfOneFailsChange(event) {
                    if (localStorage.getItem("continueIfOneFails") != event.srcElement.checked + "") {
                        localStorage.setItem("continueIfOneFails", event.srcElement.checked);
                    }
                    this._restService.setContinueIfOneFails(event.srcElement.checked);
                }
                getSettingsFromCookie(defaultHSize, defaultComHSize, defaultAutoScroll, defaultContinueIfOneFails) {
                    var storageHSize = localStorage.getItem("historySize");
                    if (storageHSize != undefined && storageHSize != "") {
                        defaultHSize = parseInt(storageHSize);
                    }
                    else {
                        localStorage.setItem("historySize", defaultHSize + "");
                    }
                    var storageComHSize = localStorage.getItem("comHistorySize");
                    if (storageComHSize != undefined && storageComHSize != "") {
                        defaultComHSize = parseInt(storageComHSize);
                    }
                    else {
                        localStorage.setItem("comHistorySize", defaultComHSize + "");
                    }
                    var storageAutoScroll = localStorage.getItem("autoScroll");
                    if (storageAutoScroll != undefined && storageAutoScroll != "") {
                        defaultAutoScroll = (storageAutoScroll == "true");
                    }
                    else {
                        localStorage.setItem("autoScroll", defaultAutoScroll + "");
                    }
                    var storageContinueIfOneFails = localStorage.getItem("continueIfOneFails");
                    if (storageContinueIfOneFails != undefined && storageContinueIfOneFails != "") {
                        defaultContinueIfOneFails = (storageContinueIfOneFails == "true");
                    }
                    else {
                        localStorage.setItem("defaultContinueIfOneFails", defaultContinueIfOneFails + "");
                    }
                    return new settings_1.Settings(defaultHSize, defaultComHSize, defaultAutoScroll, defaultContinueIfOneFails);
                }
            };
            WebCliSettingsComponent = __decorate([
                core_1.Component({
                    selector: 'web-cli-settings',
                    template: `
    <div class="collapse" id="exCollapsingNavbar">
      <div class="bg-inverse p-a-1">
          <div class="form-group row">
            <label class="form-control-label col-xs-3">Log History Size:</label>
            <div class="col-xs-2">
              <input type="text" class="form-control" [(ngModel)]="settings.historySize" (change)="onHistoryChange()">
            </div>
          </div>
          <div class="form-group row">
            <label class="form-control-label col-xs-3">Command History Size:</label>
            <div class="col-xs-2">
              <input type="text" class="form-control" [(ngModel)]="settings.comHistorySize" (change)="onComHistoryChange()">
            </div>
          </div>
          <div class="form-group row">
            <label class="form-control-label col-xs-3">AutoScroll Logs:</label>
            <div class="col-xs-2">
               <input type="checkbox" [(ngModel)]="settings.autoscroll" (change)="onAutoScrollChange($event)">
            </div>
          </div>
          <div class="form-group row">
            <label class="form-control-label col-xs-3">Continue if one fails:</label>
            <div class="col-xs-2">
               <input type="checkbox" [(ngModel)]="settings.continueIfOneFails" (change)="onContinueIfOneFailsChange($event)">
            </div>
          </div>
      </div>
    </div>
  `
                }), 
                __metadata('design:paramtypes', [communication_service_1.CommunicationService, rest_service_1.RESTService])
            ], WebCliSettingsComponent);
            exports_1("WebCliSettingsComponent", WebCliSettingsComponent);
        }
    }
});
