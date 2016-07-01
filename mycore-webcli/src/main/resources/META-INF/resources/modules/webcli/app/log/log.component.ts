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
  timeout: number;
  settings: Settings;

  constructor(private _restService: RESTService,
              private _comunicationService: CommunicationService){
                this.settings = new Settings(500, 10, true, false);
                this._comunicationService.settings.subscribe(
                  settings =>{
                    this.settings = settings;
                  }
                );
                this._restService.currentLog.subscribe(
                  log => {
                    if (log != undefined) {
                      if (document.getElementsByClassName('web-cli-log')[0].childNodes.length + 1 > this.settings.historySize) {
                        document.getElementsByClassName('web-cli-log')[0].removeChild(document.getElementsByClassName('web-cli-log')[0].childNodes[0]);
                      }
                      var node = document.createElement("pre");
                      var text = document.createTextNode(log.logLevel + ": " + log.message);
                      node.appendChild(text);
                      document.getElementsByClassName('web-cli-log')[0].appendChild(node);
                      if(log.exception != undefined) {
                        var nodeEx = document.createElement("pre");
                        var textEx = document.createTextNode(log.exception);
                        nodeEx.appendChild(textEx);
                        document.getElementsByClassName('web-cli-log')[0].appendChild(nodeEx);
                      }
                    }
                  });
              }

  public clearLog() {
    document.getElementsByClassName('web-cli-log')[0].innerHTML = "";
  }

  ngAfterViewChecked() {
    this.scrollLog()
  }

  scrollLog() {
    if (this.settings.autoscroll) {
        var elem = document.getElementsByClassName('web-cli-log');
        elem[0].scrollTop = elem[0].scrollHeight;
    }
  }
}
