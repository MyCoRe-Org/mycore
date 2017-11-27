/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

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
