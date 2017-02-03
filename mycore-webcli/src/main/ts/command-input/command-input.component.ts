import {Component, ElementRef} from '@angular/core';
import {RESTService} from '../service/rest.service';
import {CommunicationService} from '../service/communication.service';

@Component({
  selector: 'web-cli-command-input',
  templateUrl: 'app/command-input/command-input.html'
})
export class WebCliCommandInputComponent {
  command: string;
  recentCommands: string[] = new Array<string>();
  commandIndex: number = 0;
  commmandChangend: boolean = false;
  recentCommandsMaxLength: number = 10;

  constructor(private _restService: RESTService,
              private _comunicationService: CommunicationService,
              private _elementRef: ElementRef){
    this._comunicationService.currentCommand.subscribe(
      command => {
        this.command = command;
        this.commmandChangend = true;
      }
    );
    this._comunicationService.settings.subscribe(
      settings => {
        this.recentCommandsMaxLength = settings.comHistorySize;
      }
    );
    this._comunicationService.commandHistory.subscribe(
      commands => {
        this.recentCommands = commands;
      }
    );
    var commandHistory = localStorage.getItem("commandHistory");
    if (commandHistory != undefined && commandHistory != "") {
      this.recentCommands = JSON.parse(commandHistory);
    }
    this._comunicationService.setCommandHistory(this.recentCommands);
  }

  execute(command: string) {
    if (command != undefined && command != "") {
      if (this.commandIndex != 0) {
        this.recentCommands.pop();
      }
      this._restService.executeCommand(command);
      this.addCommandToRecentCommands(command);
      this.commandIndex = 0;
      this.command = "";
    }
  }

  addCommandToRecentCommands(command: string) {
    if (this.recentCommands.length == 0 || command != this.recentCommands[this.recentCommands.length - 1]) {
      if (this.recentCommands.length + 1  > this.recentCommandsMaxLength){
        this.recentCommands.shift();
      }
      this.recentCommands.push(command);
      if (this.recentCommands.length > this.recentCommandsMaxLength) {
        this.recentCommands = this.recentCommands.splice(this.recentCommandsMaxLength * -1, this.recentCommandsMaxLength);
      }
      this._comunicationService.setCommandHistory(this.recentCommands);
      this.updateCommandListInLocalStorage();
    }
  }

  onKeyPress(event, keyCode) {
    if (keyCode == "13") {
      this.execute(this.command);
    }
    if (keyCode == "38") {
      if (this.commandIndex == 0 && this.command != undefined) {
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

  updateCommandListInLocalStorage() {
    localStorage.setItem("commandHistory", JSON.stringify(this.recentCommands));
  }
}
