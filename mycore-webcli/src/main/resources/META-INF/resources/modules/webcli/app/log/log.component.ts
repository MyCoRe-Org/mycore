import {Component, OnInit} from '@angular/core';
import {Log} from './log';
import {Settings} from '../settings/settings';
import {RESTService} from '../service/rest.service';
import {CommunicationService} from '../service/communication.service';

@Component({
  selector: '[web-cli-log]',
  templateUrl: 'app/log/log.html'
})
export class WebCliLogComponent {
  log: Log[];
  timeout: number;
  settings: Settings;

  constructor(private _restService: RESTService,
              private _comunicationService: CommunicationService){
                this.log = new Array<Log>();
                this.settings = new Settings(50,true);
                this._comunicationService.settings.subscribe(
                  settings =>{
                    this.settings = settings;
                  }
                );
                this._restService.currentLog.subscribe(
                  log => {
                    if (log != undefined) {
                      this.log.push(log);
                      this.log = this.log.splice(this.settings.historySize * -1, this.settings.historySize);
                    }
                  });
              }

  public clearLog() {
    this.log.splice(0, this.log.length);
  }

  ngAfterViewChecked() {
    this.scrollLog()
  }

  scrollLog() {
    if (this.settings.autoscroll) {
        var elem = document.getElementsByClassName('web-cli-log-pre');
        elem[0].scrollTop = elem[0].scrollHeight;
    }
  }
}
