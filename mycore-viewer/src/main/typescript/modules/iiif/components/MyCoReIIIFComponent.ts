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


import { MyCoReStructFileComponent } from "../../base/components/MyCoReStructFileComponent";
import { IIIFSettings } from "./IIIFSettings";
import { Utils } from "../../base/Utils";
import { IviewIIIFProvider } from "../widgets/IviewIIIFProvider";
import { StructureModel } from "../../base/components/model/StructureModel";
import { LanguageModelLoadedEvent } from "../../base/components/events/LanguageModelLoadedEvent";
import { WaitForEvent } from "../../base/components/events/WaitForEvent";
import { ComponentInitializedEvent } from "../../base/components/events/ComponentInitializedEvent";
import { ViewerEvent } from "../../base/widgets/events/ViewerEvent";

export class MyCoReIIIFComponent extends MyCoReStructFileComponent {

  constructor(protected settings: IIIFSettings, protected container: HTMLElement) {
    super(settings, container);
  }

  private structFileAndLanguageSync: any = Utils.synchronize<MyCoReIIIFComponent>([
    (context: MyCoReIIIFComponent) => context.mm != null,
    (context: MyCoReIIIFComponent) => context.lm != null
  ], (context: MyCoReIIIFComponent) => {
    this.structFileLoaded(this.mm.model);
  });

  public init() {
    const settings = this.settings;
    if (settings.doctype === 'manifest') {

      this.vStructFileLoaded = false;
      const tilePathBuilder = (imageUrl: string, width: number, height: number) => {
        const scaleFactor = this.getScaleFactor(width, height);
        return imageUrl + '/full/' + Math.floor(width / scaleFactor) + ','
          + Math.floor(height / scaleFactor) + '/0/default.jpg';
      };

      const manifestPromise = IviewIIIFProvider
        .loadModel(this.settings.manifestURL, this.settings.imageAPIURL, tilePathBuilder);
      manifestPromise.then((resolved: { model: StructureModel; document: Document }) => {
        const model = resolved.model;
        this.trigger(new WaitForEvent(this, LanguageModelLoadedEvent.TYPE));

        if (model === null) {
          this.error = true;
          this.errorSync(this);
          return;
        }

        this.mm = resolved;

        this.structFileAndLanguageSync(this);
      });

      manifestPromise.onreject(() => {
        this.trigger(new WaitForEvent(this, LanguageModelLoadedEvent.TYPE));
        this.error = true;
        this.errorSync(this);
      });

      this.trigger(new ComponentInitializedEvent(this));
    }
  }

  public handle(e: ViewerEvent): void {

    if (e.type === LanguageModelLoadedEvent.TYPE) {
      const languageModelLoadedEvent = e as LanguageModelLoadedEvent;
      this.lm = languageModelLoadedEvent.languageModel;
      this.errorSync(this);
      this.structFileAndLanguageSync(this);
    }

    return;
  }

  private getScaleFactor(width: number, height: number) {
    const largestScaling = Math.min(256 / width, 256 / height); //TODO make smallest size dynamic
    return Math.pow(2, Math.ceil(Math.log(largestScaling) / Math.log(1 / 2)));
  }

}
