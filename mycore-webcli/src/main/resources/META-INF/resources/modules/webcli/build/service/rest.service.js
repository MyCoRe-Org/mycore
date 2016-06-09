System.register(['@angular/core', '@angular/http', 'rxjs/Subject'], function(exports_1, context_1) {
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
    var core_1, http_1, Subject_1;
    var RESTService;
    return {
        setters:[
            function (core_1_1) {
                core_1 = core_1_1;
            },
            function (http_1_1) {
                http_1 = http_1_1;
            },
            function (Subject_1_1) {
                Subject_1 = Subject_1_1;
            }],
        execute: function() {
            let RESTService = class RESTService {
                constructor(http) {
                    this.http = http;
                    this.socketURL = "/ws/mycore-webcli/socket";
                    this.socket = null;
                    this.retryCounter = 0;
                    this._currentCommandList = new Subject_1.Subject();
                    this._currentLog = new Subject_1.Subject();
                    this._currentQueue = new Subject_1.Subject();
                    this._currentCommand = new Subject_1.Subject();
                    this.currentCommandList = this._currentCommandList.asObservable();
                    this.currentLog = this._currentLog.asObservable();
                    this.currentQueue = this._currentQueue.asObservable();
                    this.currentCommand = this._currentCommand.asObservable();
                    var loc = window.location;
                    var protocol = "ws://";
                    if (location.protocol == "https:") {
                        protocol = "wss://";
                    }
                    this.socketURL = protocol + loc.host + this.getBasePath(loc.pathname) + this.socketURL;
                    this.openSocketConnection();
                }
                getCommands() {
                    var message = {
                        type: "getKnownCommands"
                    };
                    this.sendMessage(JSON.stringify(message));
                }
                executeCommand(command) {
                    if (command != undefined && command != "") {
                        var message = {
                            type: "run",
                            command: command
                        };
                        this.sendMessage(JSON.stringify(message));
                    }
                }
                startLogging() {
                    var message = {
                        type: "startLog"
                    };
                    this.sendMessage(JSON.stringify(message));
                }
                stopLogging() {
                    var message = {
                        type: "stopLog"
                    };
                    this.sendMessage(JSON.stringify(message));
                }
                sendMessage(message) {
                    if (message == "") {
                        return;
                    }
                    this.retryCounter++;
                    if (this.socket.readyState === 1) {
                        this.retryCounter = 0;
                        this.socket.send(message);
                        return;
                    }
                    if (this.socket == undefined || this.socket.readyState === 3) {
                        if (this.retryCounter < 5) {
                            this.openSocketConnection();
                            this.sendMessage(message);
                        }
                        return;
                    }
                    if (this.socket.readyState === 0 || this.socket.readyState === 2) {
                        if (this.retryCounter < 5) {
                            setTimeout(() => this.sendMessage(message), 500);
                        }
                        return;
                    }
                }
                openSocketConnection() {
                    this.socket = new WebSocket(this.socketURL);
                    this.socket.onmessage = event => {
                        if (event.data == "noPermission") {
                            console.log("You don't have permission to use the MyCoRe WebCLI!");
                            alert("You don't have permission to use the MyCoRe WebCLI!");
                            this.retryCounter = 5;
                            return;
                        }
                        var message = JSON.parse(event.data);
                        if (message.type == "getKnownCommands") {
                            this._currentCommandList.next(JSON.parse(message.return).commands);
                        }
                        if (message.type == "log") {
                            if (message.return != "") {
                                this._currentLog.next(JSON.parse(message.return));
                            }
                        }
                        if (message.type == "commandQueue") {
                            if (message.return != "") {
                                this._currentQueue.next(JSON.parse(message.return));
                            }
                            else {
                                this._currentQueue.next(new Array());
                            }
                        }
                        if (message.type == "currentCommand") {
                            this._currentCommand.next(message.return);
                        }
                    };
                }
                getBasePath(path) {
                    var pathArray = location.pathname.split("/");
                    pathArray.splice(-3);
                    return pathArray.join("/");
                }
            };
            RESTService = __decorate([
                core_1.Injectable(), 
                __metadata('design:paramtypes', [http_1.Http])
            ], RESTService);
            exports_1("RESTService", RESTService);
        }
    }
});
