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


import { ViewerComponent } from "../../base/components/ViewerComponent";
import { ChapterChangedEvent } from "../../base/components/events/ChapterChangedEvent";
import { MyCoReViewerSettings } from "../../base/MyCoReViewerSettings";
import { ButtonPressedEvent } from "../../base/widgets/toolbar/events/ButtonPressedEvent";
import { RequestStateEvent } from "../../base/components/events/RequestStateEvent";
import { RestoreStateEvent } from "../../base/components/events/RestoreStateEvent";
import { ProvideToolbarModelEvent } from "../../base/components/events/ProvideToolbarModelEvent";
import { ViewerEvent } from "../../base/widgets/events/ViewerEvent";
import { WaitForEvent } from "../../base/components/events/WaitForEvent";
import { ShowContentEvent } from "../../base/components/events/ShowContentEvent";
import { Debounce, MyCoReMap } from "../../base/Utils";
import { StructureChapter } from "../../base/components/model/StructureChapter";
import { EpubStructureChapter } from "../widgets/EpubStructureChapter";
import { EpubStructureBuilder } from "../widgets/EpubStructureBuilder";
import { StructureModel } from "../../base/components/model/StructureModel";
import { StructureModelLoadedEvent } from "../../base/components/events/StructureModelLoadedEvent";


declare function ePub(a: any, settings?: any): any;

export class MyCoReEpubDisplayComponent extends ViewerComponent {

  private rendition: any;
  private zoomInPercent: number = 100;
  private currentHref: string;

  constructor(private epubSettings: MyCoReViewerSettings, private appContainer: JQuery) {
    super();
  }

  get handlesEvents() {
    const handlesEvents = [];
    if (this.epubSettings.doctype !== 'epub') {
      return handlesEvents;
    }

    handlesEvents.push(ButtonPressedEvent.TYPE);
    handlesEvents.push(ChapterChangedEvent.TYPE);
    handlesEvents.push(RequestStateEvent.TYPE);
    handlesEvents.push(RestoreStateEvent.TYPE);
    handlesEvents.push(ProvideToolbarModelEvent.TYPE);
    return handlesEvents;
  }

  handle(e: ViewerEvent): void {
    if (this.epubSettings.doctype !== 'epub') {
      return;
    }

    if (e.type === ChapterChangedEvent.TYPE) {
      const cce = e as ChapterChangedEvent;
      this.handleChapterChangedEvent(cce);
      return;
    }

    if (e.type === ProvideToolbarModelEvent.TYPE) {
      const ptme = e as ProvideToolbarModelEvent;
      this.handleProvideToolbarModelEvent(ptme);
      return;
    }

    if (e.type === ButtonPressedEvent.TYPE) {
      const buttonPressedEvent = e as ButtonPressedEvent;
      this.handleButtonPressedEvent(buttonPressedEvent);
      return;
    }

    if (e.type === RequestStateEvent.TYPE) {
      const rse = e as RequestStateEvent;
      this.handleRequestStateEvent(rse);
      return;
    }

    if (e.type === RestoreStateEvent.TYPE) {
      const rse = e as RestoreStateEvent;
      if (rse.restoredState.has('start')) {
        this.rendition.display(rse.restoredState.get('start'));
      }
      return;
    }
  }

  public init() {
    if (this.epubSettings.doctype !== 'epub') {
      return;
    }

    this.trigger(new WaitForEvent(this, ProvideToolbarModelEvent.TYPE));

    const content = document.createElement('div');
    content.style.width = '100%';
    content.style.height = '100%';
    content.style.background = 'white';
    content.style.paddingLeft = '5%';

    this.trigger(new ShowContentEvent(this, jQuery(content), ShowContentEvent.DIRECTION_CENTER));

    const book = ePub((<any>this.epubSettings).epubPath, {
      openAs: 'directory',
      method: 'goToTheElse!'
    });

    this.rendition = book.renderTo(content, {
      manager: 'continuous',
      flow: 'scrolled',
      width: '100%',
      height: '100%',
      offset: 1 // required or the viewer will jump around on chapter change
    });
    this.rendition.display();

    const resizeBounce = new Debounce<boolean>(100, (b) => {
      this.rendition.resize();
    });
    jQuery(content).bind('iviewResize', () => {
      resizeBounce.call(false);
    });

    const idChapterMap = new MyCoReMap<string, StructureChapter>();

    book.loaded.navigation.then((toc) => {
      const children = [];
      const root = new EpubStructureChapter(
        null,
        'root',
        this.epubSettings.filePath,
        children,
        null);

      const builder = new EpubStructureBuilder();
      toc.forEach((item) => {
        children.push(builder.convertToChapter(item, root));
        return {};
      });

      const model = new StructureModel(root, [], new MyCoReMap(), new MyCoReMap(), new MyCoReMap(), false);
      let addToMap: (chapter: StructureChapter) => void;
      addToMap = (chapter: StructureChapter) => {
        idChapterMap.set(chapter.id, chapter);
        chapter.chapter.forEach((child) => {
          addToMap(child);
        });
      };
      addToMap(root);
      this.trigger(new StructureModelLoadedEvent(this, model));
    });

    this.rendition.on('relocated', (section) => {
      //let navigationPoint = book.navigation.get(section.start.href);
      if (section.start.href !== this.currentHref) {
        this.currentHref = section.start.href;
        //this.trigger(new mycore.viewer.components.events.ChapterChangedEvent(this, idChapterMap.get(section.start.href)));
      }
    });

    this.trigger(new WaitForEvent(this, RestoreStateEvent.TYPE));
  }

  private handleRequestStateEvent(rse: RequestStateEvent) {
    const location = this.rendition.currentLocation();
    rse.stateMap.set('start', location.start.cfi);
    rse.stateMap.set('end', location.end.cfi);
  }

  private handleButtonPressedEvent(buttonPressedEvent: ButtonPressedEvent) {
    if (buttonPressedEvent.button.id === 'ZoomInButton') {
      this.zoomInPercent += 25;
      this.rendition.themes.fontSize(`${this.zoomInPercent}%`);
    }

    if (buttonPressedEvent.button.id === 'ZoomOutButton') {
      this.zoomInPercent -= 25;
      this.rendition.themes.fontSize(`${this.zoomInPercent}%`);

    }
  }

  private handleProvideToolbarModelEvent(ptme: ProvideToolbarModelEvent) {
    ptme.model._zoomControllGroup.removeComponent(ptme.model._rotateButton);
    ptme.model._zoomControllGroup.removeComponent(ptme.model._zoomFitButton);
    ptme.model._zoomControllGroup.removeComponent(ptme.model._zoomWidthButton);

    ptme.model.removeGroup(ptme.model._imageChangeControllGroup);

    const lcg = ptme.model.getGroup(ptme.model._layoutControllGroup.name);
    if (lcg !== null && typeof lcg !== 'undefined') {
      ptme.model.removeGroup(ptme.model._layoutControllGroup);
    }
  }

  private handleChapterChangedEvent(cce: ChapterChangedEvent) {
    const chapter = cce.chapter as EpubStructureChapter;
    if (chapter != null || typeof chapter !== 'undefined') {
      this.currentHref = chapter.id;
      if (chapter.epubChapter !== null && cce.component !== this) {
        this.rendition.display(chapter.epubChapter.href);
      }
    }
  }
}




