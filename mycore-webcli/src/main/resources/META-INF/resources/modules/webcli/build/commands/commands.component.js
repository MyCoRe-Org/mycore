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
    var WebCliCommandsComponent;
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
            let WebCliCommandsComponent = class WebCliCommandsComponent {
                constructor(_restService, _comunicationService) {
                    this._restService = _restService;
                    this._comunicationService = _comunicationService;
                    this._restService.currentCommandList.subscribe(commandList => this.commandList = commandList);
                }
                ngOnInit() {
                    this._restService.getCommands();
                }
                onSelect(command) {
                    this._comunicationService.setCurrentCommand(command);
                }
                onHoverSubmenu(event) {
                    if (event.target.className == "dropdown-item") {
                        var maxHeight = window.innerHeight - event.target.parentElement.getBoundingClientRect().top - 10;
                        if (event.target.parentElement.children.length > 1 && event.target.parentElement.children[1].className == "dropdown-menu") {
                            event.target.parentElement.children[1].style.maxHeight = maxHeight;
                        }
                    }
                }
            };
            WebCliCommandsComponent = __decorate([
                core_1.Component({
                    selector: '[webcli-commands]',
                    template: `
    <li *ngFor="let command of commandList" class="dropdown-submenu" (mouseover)="onHoverSubmenu($event)">
      <a class="dropdown-item" href="#" onclick='return false;'>{{command.name}}</a>
      <ul class="dropdown-menu" *ngIf="command.commands">
        <li *ngFor="let com of command.commands"><a class="dropdown-item" href="#" (click)="onSelect(com.command)" onclick='return false;' title="{{com.help}}">{{com.command}}</a></li>
      </ul>
    </li>
  `,
                }), 
                __metadata('design:paramtypes', [rest_service_1.RESTService, communication_service_1.CommunicationService])
            ], WebCliCommandsComponent);
            exports_1("WebCliCommandsComponent", WebCliCommandsComponent);
        }
    }
});
