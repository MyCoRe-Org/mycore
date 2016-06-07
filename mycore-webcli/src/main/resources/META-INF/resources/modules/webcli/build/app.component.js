System.register(['@angular/core', './commands/commands.component', './command-input/command-input.component', './log/log.component', './queue/queue.component', './settings/settings.component', './service/communication.service', './service/rest.service'], function(exports_1, context_1) {
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
    var core_1, commands_component_1, command_input_component_1, log_component_1, queue_component_1, settings_component_1, communication_service_1, rest_service_1;
    var AppComponent;
    return {
        setters:[
            function (core_1_1) {
                core_1 = core_1_1;
            },
            function (commands_component_1_1) {
                commands_component_1 = commands_component_1_1;
            },
            function (command_input_component_1_1) {
                command_input_component_1 = command_input_component_1_1;
            },
            function (log_component_1_1) {
                log_component_1 = log_component_1_1;
            },
            function (queue_component_1_1) {
                queue_component_1 = queue_component_1_1;
            },
            function (settings_component_1_1) {
                settings_component_1 = settings_component_1_1;
            },
            function (communication_service_1_1) {
                communication_service_1 = communication_service_1_1;
            },
            function (rest_service_1_1) {
                rest_service_1 = rest_service_1_1;
            }],
        execute: function() {
            let AppComponent = class AppComponent {
                constructor(_restService) {
                    this._restService = _restService;
                    this.title = 'MyCoRe Web CLI2';
                    this.refreshRunning = true;
                    this.currentCommand = "";
                    this._restService.currentCommand.subscribe(command => {
                        this.currentCommand = command;
                    });
                }
                onClickCommandDropDown(event) {
                    // if (event.target.className.indexOf("dropdown-toggle") > -1) {
                    //   var maxHeight = window.innerHeight - event.target.getBoundingClientRect().bottom - 10;
                    //   if (event.target.parentElement.children.length > 1 && event.target.parentElement.children[1].className == "dropdown-menu") {
                    //     event.target.parentElement.children[1].style.maxHeight = maxHeight;
                    //   }
                    // }
                }
                clearLog() {
                    this.webCliLogComponent.clearLog();
                }
                setRefresh(refresh) {
                    this.refreshRunning = refresh;
                    if (refresh) {
                        this._restService.startLogging();
                    }
                    else {
                        this._restService.stopLogging();
                    }
                }
            };
            __decorate([
                core_1.ViewChild(log_component_1.WebCliLogComponent), 
                __metadata('design:type', log_component_1.WebCliLogComponent)
            ], AppComponent.prototype, "webCliLogComponent", void 0);
            AppComponent = __decorate([
                core_1.Component({
                    selector: 'webcli',
                    template: `
    <div id="web-cli-header">
      <web-cli-settings></web-cli-settings>
      <nav class="navbar navbar-dark bg-inverse">
        <ul class="nav navbar-nav">
          <li class="nav-item dropdown">
            <a class="nav-link dropdown-toggle" data-toggle="dropdown" href="#" role="button" aria-haspopup="true" aria-expanded="false" (click)="onClickCommandDropDown($event)">Command</a>
            <div class="dropdown-menu" webcli-commands></div>
          </li>
          <li class="nav-item">
            <span class="nav-link" (click)="clearLog()">
              <i class="fa fa-eraser"></i>
              Clear Logs
            </span>
          </li>
          <li class="nav-item" [hidden]="refreshRunning">
            <span class="nav-link" (click)="setRefresh(true)">
              <i class="fa fa-play"></i>
              Refresh
            </span>
          </li>
          <li class="nav-item" [hidden]="!refreshRunning">
            <span class="nav-link with-spinner" (click)="setRefresh(false)">
              <i class="fa fa-pause"></i>
              Stop Refresh
            </span>
          </li>
          <li class="nav-item" data-toggle="collapse" data-target="#exCollapsingNavbar">
            <span class="nav-link">
              <i class="fa fa-cog"></i>
              Settings
            </span>
          </li>
        </ul>
      </nav>
    </div>
    <web-cli-command-input></web-cli-command-input>
    <div class="row" id="current-command">
      <div [hidden]="!currentCommand">
        <div class="col-lg-3 col-md-4 col-sm-5 current-command-label">
          Currently running command:
        </div>
        <div class="col-lg-9 col-md-8 col-sm-7" id="current-command-running" title="{{currentCommand}}">
          {{currentCommand}}
        </div>
        <div class="col-sm-12">
          <div class="loader"></div>
        </div>
      </div>
      <div [hidden]="currentCommand">
        <div class="col-sm-12 current-command-label">
          No command running currently.
        </div>
        <div class="col-sm-12">
          <div class="loader-no-animation"></div>
        </div>
      </div>
    </div>
    <ul class="nav nav-tabs" role="tablist">
      <li class="nav-item active">
        <a class="nav-link" href="#log" data-toggle="tab" role="tab" onclick='return false;'>Log</a>
      </li>
      <li class="nav-item">
        <a class="nav-link" href="#queue" data-toggle="tab" role="tab" onclick='return false;'>Command Queue</a>
      </li>
    </ul>
    <div class="tab-content">
      <div class="row tab-pane fade in active" id="log" role="tabpanel" web-cli-log></div>
      <div class="row tab-pane fade" id="queue" role="tabpanel" web-cli-queue></div>
    </div>
  `,
                    directives: [commands_component_1.WebCliCommandsComponent, command_input_component_1.WebCliCommandInputComponent, log_component_1.WebCliLogComponent, settings_component_1.WebCliSettingsComponent, queue_component_1.WebCliQueueComponent],
                    providers: [communication_service_1.CommunicationService, rest_service_1.RESTService]
                }), 
                __metadata('design:paramtypes', [rest_service_1.RESTService])
            ], AppComponent);
            exports_1("AppComponent", AppComponent);
        }
    }
});
