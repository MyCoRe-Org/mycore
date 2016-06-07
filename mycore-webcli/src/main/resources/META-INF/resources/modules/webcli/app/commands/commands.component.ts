import {Component, OnInit} from '@angular/core';
import {RESTService} from '../service/rest.service';
import {CommunicationService} from '../service/communication.service';
import {Commands} from './commands';

@Component({
  selector: '[webcli-commands]',
  templateUrl: 'app/commands/commands.html',
})
export class WebCliCommandsComponent implements OnInit {
  commandList: Commands[];
  currentCommand: string;

  constructor(private _restService: RESTService,
              private _comunicationService: CommunicationService){
                this._restService.currentCommandList.subscribe(
                  commandList => this.commandList = commandList
                );
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
}
