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

import {Injectable} from '@angular/core';
import {Http, Response} from '@angular/http';
import {Commands} from '../commands/commands';
import {Log} from '../log/log';
import {Observable} from 'rxjs/Observable';
import {Subject} from 'rxjs/Subject';

@Injectable()
export class RESTService {
  socketURL: string = "/ws/mycore-webcli/socket";
  socket = null;
  retryCounter: number = 0;
  private _currentCommandList = new Subject<Commands[]>();
  private _currentLog = new Subject<Log>();
  private _currentQueue = new Subject<String[]>();
  private _currentCommand = new Subject<String>();
  private _currentQueueLength = new Subject<number>();
  private _continueIfOneFails = new Subject<boolean>();

  currentCommandList = this._currentCommandList.asObservable();
  currentLog = this._currentLog.asObservable();
  currentQueue = this._currentQueue.asObservable();
  currentCommand = this._currentCommand.asObservable();
  currentQueueLength = this._currentQueueLength.asObservable();
  continueIfOneFails = this._continueIfOneFails.asObservable();

  constructor(private http: Http) {
    var loc = window.location;
    var protocol = "ws://";
    if (location.protocol == "https:"){
      protocol = "wss://";
    }
    this.socketURL = protocol + loc.host + this.getBasePath(loc.pathname) + this.socketURL;
    this.openSocketConnection();
  }

  getCommands() {
    var message = {
      type: "getKnownCommands"
    }
    this.sendMessage(JSON.stringify(message));
  }

  executeCommand(command: string) {
    if (command != undefined && command != "") {
      var message = {
        type: "run",
        command: command
      }
      this.sendMessage(JSON.stringify(message));
    }
  }

  startLogging() {
    var message = {
      type: "startLog"
    }
    this.sendMessage(JSON.stringify(message));
  }

  stopLogging() {
    var message = {
      type: "stopLog"
    }
    this.sendMessage(JSON.stringify(message));
  }

  clearCommandList() {
    var message = {
      type: "clearCommandList"
    }
    this.sendMessage(JSON.stringify(message));
  }

  setContinueIfOneFails(con: boolean) {
    var message = {
      type: "continueIfOneFails",
      value: con
    }
    this.sendMessage(JSON.stringify(message));
  }

  private sendMessage(message: String) {
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

  private openSocketConnection() {
    this.socket = new WebSocket(this.socketURL);
    this.socket.onmessage = event => {
      if (event.data == "noPermission"){
        console.log("You don't have permission to use the MyCoRe WebCLI!");
        alert("You don't have permission to use the MyCoRe WebCLI!");
        this.retryCounter = 5;
        return;
      }
      var message = JSON.parse(event.data);
      if (message.type == "getKnownCommands"){
        this._currentCommandList.next(<Commands[]> JSON.parse(message.return).commands);
      }
      if (message.type == "log"){
        if(message.return != "") {
          this._currentLog.next(<Log> JSON.parse(message.return));
        }
      }
      if (message.type == "commandQueue"){
        if(message.return != "") {
          this._currentQueue.next(<String[]> JSON.parse(message.return));
        }
        else {
          this._currentQueue.next(new Array<String>());
        }
        this._currentQueueLength.next(message.size);
      }
      if (message.type == "currentCommand"){
          this._currentCommand.next(message.return);
      }
      if (message.type == "continueIfOneFails"){
          if(message.value != undefined) {
            this._continueIfOneFails.next(message.value);
          }
      }
    }
  }

  private getBasePath(path: String) {
    var pathArray = location.pathname.split("/")
    pathArray.splice(-3);
    return pathArray.join("/");
  }
}
