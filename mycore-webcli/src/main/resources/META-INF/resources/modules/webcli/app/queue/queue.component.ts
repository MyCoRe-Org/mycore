import {Component} from '@angular/core';
import {Settings} from '../settings/settings';
import {RESTService} from '../service/rest.service';

@Component({
  selector: '[web-cli-queue]',
  templateUrl: 'app/queue/queue.html'
})
export class WebCliQueueComponent {
  constructor(private _restService: RESTService){
    this._restService.currentQueue.subscribe(
      queue => {
        let ellipsis = "";
        if (queue.length > 99) {
          ellipsis = "</br>...";
        }
        let queueString = queue.join("</br>") + ellipsis;
        document.getElementsByClassName('web-cli-pre')[0].innerHTML = queueString;
      });
  }
}
