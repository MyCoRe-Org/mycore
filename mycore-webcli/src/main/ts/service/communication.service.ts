import {Injectable} from '@angular/core';
import {Settings} from '../settings/settings';
import {Observable} from 'rxjs/Observable';
import {Subject} from 'rxjs/Subject';


@Injectable()
export class CommunicationService {
  private _currentCommand = new Subject<string>();
  private _settings = new Subject<Settings>();
  private _commandHistory = new Subject<string[]>();

  currentCommand = this._currentCommand.asObservable();
  settings = this._settings.asObservable();
  commandHistory = this._commandHistory.asObservable();

  setCurrentCommand(command: string) {
    this._currentCommand.next(command);
  }

  setSettings(setting: Settings) {
    this._settings.next(setting);
  }

  setCommandHistory(history: string[]) {
    this._commandHistory.next(history);
  }
}
