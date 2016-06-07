import {Component} from '@angular/core';
import {RESTService} from '../service/rest.service';

@Component({
  selector: '[web-cli-queue]',
  templateUrl: 'app/queue/queue.html'
})
export class WebCliQueueComponent {
  currentQueue: String[];
  constructor(private _restService: RESTService){
    this.currentQueue = new Array<String>();
    this._restService.currentQueue.subscribe(
      queue => {
        if (queue != undefined) {
          this.currentQueue = queue;
        }
      });
  }
}
