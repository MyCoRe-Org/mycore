System.register(['@angular/core', '../service/rest.service', '../service/communication.service'], function(exports_1, context_1) {
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
    var core_1, rest_service_1, communication_service_1;
    var WebCliCommandInputComponent;
    return {
        setters:[
            function (core_1_1) {
                core_1 = core_1_1;
            },
            function (rest_service_1_1) {
                rest_service_1 = rest_service_1_1;
            },
            function (communication_service_1_1) {
                communication_service_1 = communication_service_1_1;
            }],
        execute: function() {
            let WebCliCommandInputComponent = class WebCliCommandInputComponent {
                constructor(_restService, _comunicationService, _elementRef) {
                    this._restService = _restService;
                    this._comunicationService = _comunicationService;
                    this._elementRef = _elementRef;
                    this.recentCommands = new Array();
                    this.commandIndex = 0;
                    this.commmandChangend = false;
                    this._comunicationService.currentCommand.subscribe(command => {
                        this.command = command;
                        this.commmandChangend = true;
                    });
                }
                execute(command) {
                    if (this.commandIndex != 0) {
                        this.recentCommands.pop();
                    }
                    this._restService.executeCommand(command);
                    this.recentCommands.push(command);
                    this.commandIndex = 0;
                    this.command = "";
                }
                onKeyPress(event, keyCode) {
                    if (keyCode == "13") {
                        this.execute(this.command);
                    }
                    if (keyCode == "38") {
                        if (this.commandIndex == 0) {
                            this.recentCommands.push(this.command);
                            this.commandIndex++;
                        }
                        if (this.recentCommands.length > this.commandIndex) {
                            this.commandIndex++;
                            this.command = this.recentCommands[this.recentCommands.length - this.commandIndex];
                        }
                    }
                    if (keyCode == "40") {
                        if (this.commandIndex > 1) {
                            this.commandIndex--;
                            this.command = this.recentCommands[this.recentCommands.length - this.commandIndex];
                        }
                        if (this.commandIndex == 1) {
                            this.recentCommands.pop();
                            this.commandIndex--;
                        }
                    }
                    if (keyCode == "9") {
                        event.preventDefault();
                        this.selectFirstPlaceHolder();
                    }
                }
                ngAfterViewChecked() {
                    if (this.commmandChangend) {
                        this.selectFirstPlaceHolder();
                        this.commmandChangend = false;
                    }
                }
                selectFirstPlaceHolder() {
                    let input = this._elementRef.nativeElement.getElementsByTagName("input")[0];
                    let match = /\{[0-9]+\}/.exec(this.command);
                    if (match != null && input != undefined) {
                        input.focus();
                        input.setSelectionRange(match.index, match.index + match[0].length);
                    }
                }
            };
            WebCliCommandInputComponent = __decorate([
                core_1.Component({
                    selector: 'web-cli-command-input',
                    template: `
    <div class="row" id="command-input">
      <div class="col-xs-12">
        <div class="input-group">
          <input type="text" class="form-control" [(ngModel)]="command" placeholder="Command..." (keydown)="onKeyPress($event, $event.keyCode)">
          <span class="input-group-btn">
            <button class="btn btn-secondary" type="button" (click)="execute(command)">Execute</button>
          </span>
        </div>
      </div>
    </div>
  `
                }), 
                __metadata('design:paramtypes', [rest_service_1.RESTService, communication_service_1.CommunicationService, core_1.ElementRef])
            ], WebCliCommandInputComponent);
            exports_1("WebCliCommandInputComponent", WebCliCommandInputComponent);
        }
    }
});
