import {Component} from '@angular/core';
import {Settings} from '../settings/settings';
import {RESTService} from '../service/rest.service';
import {CommunicationService} from '../service/communication.service';

@Component({
  selector: '[web-cli-queue]',
  templateUrl: 'app/queue/queue.html'
})
export class WebCliQueueComponent {
  settings: Settings;
  constructor(private _restService: RESTService,
              private _comunicationService: CommunicationService){
    this.settings = new Settings(50, true, false);
    this._comunicationService.settings.subscribe(
      settings =>{
        this.settings = settings;
      }
    );
    this._restService.currentQueue.subscribe(
      queue => {
        let ellipsis = "";
        if (queue.length > this.settings.historySize) {
          queue = queue.slice(0, this.settings.historySize);
          ellipsis = "</br>...";
        }
        let queueString = queue.join("</br>") + ellipsis;
        document.getElementsByClassName('web-cli-pre')[0].innerHTML = queueString;
      });
  }
}
