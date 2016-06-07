import {Component, ViewChild} from '@angular/core';
import {WebCliCommandsComponent} from './commands/commands.component';
import {WebCliCommandInputComponent} from './command-input/command-input.component';
import {WebCliLogComponent} from './log/log.component';
import {WebCliQueueComponent} from './queue/queue.component';
import {WebCliSettingsComponent} from './settings/settings.component';
import {CommunicationService} from './service/communication.service';
import {RESTService} from './service/rest.service';

@Component({
  selector: 'webcli',
  templateUrl: 'app/app.html',
  directives: [WebCliCommandsComponent, WebCliCommandInputComponent, WebCliLogComponent, WebCliSettingsComponent, WebCliQueueComponent],
  providers: [CommunicationService, RESTService]
})
export class AppComponent {
  title = 'MyCoRe Web CLI2';
  refreshRunning = true;
  currentCommand: String = "";
  @ViewChild(WebCliLogComponent)
  webCliLogComponent: WebCliLogComponent;

  constructor(private _restService: RESTService){
    this._restService.currentCommand.subscribe(
      command => {
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

  clearLog(){
    this.webCliLogComponent.clearLog();
  }

  setRefresh(refresh: boolean){
    this.refreshRunning = refresh;
    if (refresh){
      this._restService.startLogging();
    }
    else {
      this._restService.stopLogging();
    }
  }
}
