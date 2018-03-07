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
import {Observable} from 'rxjs/Observable';
import {Observer} from 'rxjs/Observer';
import {Util} from '../util';

@Injectable()
export class ProcessingService {

    protected path: string = '/ws/mycore-webtools/processing';

    protected socketURL: string = null;
    protected retryCounter: number;

    protected socket: WebSocket = null;
    public observable: Observable<MessageEvent> = null;

    constructor() {
        const loc = window.location;
        let protocol = 'ws://';
        if (location.protocol === 'https:') {
            protocol = 'wss://';
        }
        this.socketURL = protocol + loc.host + Util.getBasePath(loc.pathname) + this.path;
    }

    public send(message: String) {
        if (message === '') {
            return;
        }
        this.retryCounter++;
        if (this.socket.readyState === 1) {
            this.retryCounter = 0;
            this.socket.send(message);
            return;
        }
        if (this.socket === null || this.socket === undefined || this.socket.readyState === 3) {
            if (this.retryCounter < 5) {
                this.connect();
                this.send(message);
            }
            return;
        }
        if (this.socket.readyState === 0 || this.socket.readyState === 2) {
            if (this.retryCounter < 5) {
                setTimeout(() => this.send(message), 500);
            }
            return;
        }
    }

    public connect() {
        this.retryCounter = 0;
        this.socket = new WebSocket(this.socketURL);

        this.observable = Observable.create((observer: Observer<MessageEvent>) => {
            this.socket.onmessage = observer.next.bind(observer);
            this.socket.onerror = observer.error.bind(observer);
            this.socket.onclose = observer.complete.bind(observer);
            return this.socket.close.bind(this.socket);
        });

        this.socket.onopen = () => {
            this.send(JSON.stringify({
                type: 'connect'
            }));
        };
    }

}
