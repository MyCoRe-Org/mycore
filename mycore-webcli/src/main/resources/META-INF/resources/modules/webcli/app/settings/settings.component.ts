import {Component, OnInit} from '@angular/core';
import {CommunicationService} from '../service/communication.service';
import {Settings} from './settings';

@Component({
  selector: 'web-cli-settings',
  templateUrl: 'app/settings/settings.html'
})
export class WebCliSettingsComponent {
  settings: Settings;

  constructor(private _communicationService: CommunicationService){}

  ngOnInit() {
    this.settings = this.getSettingsFromCookie(50, true);
    this._communicationService.setSettings(this.settings);
  }

  onHistoryChange() {
    if (localStorage.getItem("historySize") != this.settings.historySize + "") {
      localStorage.setItem("historySize", this.settings.historySize + "");
    }
  }

  onAutoScrollChange(event) {
    if (localStorage.getItem("autoScroll") != event.srcElement.checked + "") {
      localStorage.setItem("autoScroll", event.srcElement.checked);
    }
  }

  private getSettingsFromCookie(defaultHSize: number, defautlAutoScroll: boolean) {
    var storageHSize = localStorage.getItem("historySize");
    if (storageHSize != undefined && storageHSize != ""){
      defaultHSize = parseInt(storageHSize);
    }
    else {
      localStorage.setItem("historySize", defaultHSize + "");
    }
    var storageAutoScroll = localStorage.getItem("autoScroll");
    if (storageAutoScroll != undefined && storageAutoScroll != ""){
      defautlAutoScroll = (storageAutoScroll == "true");
    }
    else {
      localStorage.setItem("autoScroll", defautlAutoScroll +  "");
    }
    return new Settings(defaultHSize, defautlAutoScroll);
  }
}
