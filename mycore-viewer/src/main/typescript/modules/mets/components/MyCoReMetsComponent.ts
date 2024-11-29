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

import { Utils } from "../../base/Utils";
import { MyCoReStructFileComponent } from "../../base/components/MyCoReStructFileComponent";
import { MetsSettings } from "./MetsSettings";
import { MetsLoadedEvent } from "./events/MetsLoadedEvent";
import { StructureModel } from "../../base/components/model/StructureModel";
import { WaitForEvent } from "../../base/components/events/WaitForEvent";
import { LanguageModelLoadedEvent } from "../../base/components/events/LanguageModelLoadedEvent";
import { ViewerEvent } from "../../base/widgets/events/ViewerEvent";
import { ComponentInitializedEvent } from "../../base/components/events/ComponentInitializedEvent";
import { IviewMetsProvider } from "../widgets/IviewMetsProvider";

export class MyCoReMetsComponent extends MyCoReStructFileComponent {

  constructor(protected settings: MetsSettings, protected container: JQuery) {
    super(settings, container);
  }

  private structFileAndLanguageSync: any = Utils.synchronize<MyCoReMetsComponent>([
    (context: MyCoReMetsComponent) => context.mm != null,
    (context: MyCoReMetsComponent) => context.lm != null
  ], (context: MyCoReMetsComponent) => {
    this.structFileLoaded(this.mm.model);
    this.trigger(new MetsLoadedEvent(this, this.mm));
  });

  public init() {
    const settings = this.settings;
    if (settings.doctype === 'mets') {
      if ((settings.imageXmlPath.charAt(settings.imageXmlPath.length - 1) != '/')) {
        settings.imageXmlPath = settings.imageXmlPath + '/';
      }

      if ((settings.tileProviderPath.charAt(settings.tileProviderPath.length - 1) != '/')) {
        settings.tileProviderPath = settings.tileProviderPath + '/';
      }

      this.vStructFileLoaded = false;
      const tilePathBuilder = (image: string) => {
        return this.settings.tileProviderPath.split(',')[0]
          + this.settings.derivate + '/' + image + '/0/0/0.jpg';
      };

      const metsPromise = IviewMetsProvider.loadModel(this.settings.metsURL,
        tilePathBuilder);
      metsPromise.then((resolved: { model: StructureModel; document: Document }) => {
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

      metsPromise.onreject(() => {
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

}
