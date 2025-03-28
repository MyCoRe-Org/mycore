/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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


import { MyCoReMap, VIEWER_COMPONENTS, ViewerFormatString } from "./Utils";
import { MyCoReViewerSettings } from "./MyCoReViewerSettings";
import { ViewerComponent } from "./components/ViewerComponent";
import { ViewerEvent } from "./widgets/events/ViewerEvent";
import { WaitForEvent } from "./components/events/WaitForEvent";


export class MyCoReViewer {

  constructor(private _container: HTMLElement, private _settings: MyCoReViewerSettings) {
    this._eventHandlerMap = new MyCoReMap<string, Array<ViewerComponent>>();
    this._components = new Array<ViewerComponent>();
    this._initializingEvents = new Array<ViewerEvent>();

    this.initialize();
  }


  private _eventHandlerMap: MyCoReMap<string, Array<ViewerComponent>>;
  private _initializing: boolean = false;
  private _initializingEvents: Array<ViewerEvent>;
  private _components: Array<ViewerComponent>;

  public get container() {
    return this._container;
  }

  public addComponent(ic: ViewerComponent) {
    ic.bind((event: ViewerEvent) => {
      this.eventTriggered(event);
    });

    const events = ic.handlesEvents;

    if (typeof events != "undefined" && events != null && events instanceof Array) {
      events.push(WaitForEvent.TYPE)

      for (const eIndex in events) {
        const e = events[eIndex];
        // initialize the array for the event first
        if (!this._eventHandlerMap.has(e)) {
          this._eventHandlerMap.set(e, new Array<ViewerComponent>());
        }

        this._eventHandlerMap.get(e).push(ic);
      }
    } else {
      console.log(ViewerFormatString("The component {comp} doesnt have a valid handlesEvents!", { comp: ic }))
    }

    this._components.push(ic);
    ic.init();
  }

  private eventTriggered(e: ViewerEvent) {
    if (this._eventHandlerMap.has(e.type)) {
      const handlers = this._eventHandlerMap.get(e.type);
      for (const component of handlers) {
        component._handle(e);
      }
    }
  }

  private initialize(): void {
    this._settings = MyCoReViewerSettings.normalize(this._settings);
    if (!this._container.classList.contains("mycoreViewer")) {
      this._container.classList.add("mycoreViewer");
    }

    for (const ic of VIEWER_COMPONENTS) {
      try {
        this.addComponent(new ic(this._settings, this._container));
      } catch (e) {
        console.log("Unable to add component");
        console.log(e);
      }
    }

  }

}





