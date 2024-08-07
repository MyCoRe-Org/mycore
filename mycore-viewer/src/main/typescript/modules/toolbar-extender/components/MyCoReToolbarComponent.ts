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


import {MyCoReViewerSettings} from "../../base/MyCoReViewerSettings";
import {ViewerComponent} from "../../base/components/ViewerComponent";
import {MyCoReMap, Utils} from "../../base/Utils";
import {ToolbarButton} from "../../base/widgets/toolbar/model/ToolbarButton";
import {ToolbarGroup} from "../../base/widgets/toolbar/model/ToolbarGroup";
import {MyCoReBasicToolbarModel} from "../../base/components/model/MyCoReBasicToolbarModel";
import {LanguageModel} from "../../base/components/model/LanguageModel";
import {ViewerEvent} from "../../base/widgets/events/ViewerEvent";
import {ComponentInitializedEvent} from "../../base/components/events/ComponentInitializedEvent";
import {WaitForEvent} from "../../base/components/events/WaitForEvent";
import {ProvideToolbarModelEvent} from "../../base/components/events/ProvideToolbarModelEvent";
import {ButtonPressedEvent} from "../../base/widgets/toolbar/events/ButtonPressedEvent";
import {LanguageModelLoadedEvent} from "../../base/components/events/LanguageModelLoadedEvent";

export interface ToolbarExtenderEntry {
    id: string;
    type: string; /* button, group */
    label?: string;
    icon?: string;
    href?: string;
    tooltip?: string;
    action?: () => void;
    inGroup?: string;
    order?: number;
}

export interface ToolbarExtenderSettings extends MyCoReViewerSettings {
    toolbar: ToolbarExtenderEntry[];
}

export class MyCoReToolbarExtenderComponent extends ViewerComponent {
    private idEntryMapping = new MyCoReMap<string, ToolbarExtenderEntry>();
    private idButtonMapping = new MyCoReMap<string, ToolbarButton>();
    private idGroupMapping = new MyCoReMap<string, ToolbarGroup>();

    constructor(private _settings: ToolbarExtenderSettings) {
        super();
    }

    private toolbarModel: MyCoReBasicToolbarModel = null;
    private languageModel: LanguageModel = null;
    private toolbarButtonSync = Utils.synchronize<MyCoReToolbarExtenderComponent>([me => me.languageModel != null,
            me => me.toolbarModel != null]
        , me => me.initLanguage());


    public init() {
        if ("toolbar" in this._settings) {
            this._settings.toolbar.forEach((settingsEntry: ToolbarExtenderEntry) => {
                if (settingsEntry.type == "group") {
                    this.idGroupMapping.set(settingsEntry.id, new ToolbarGroup(settingsEntry.id, settingsEntry.order || 70));

                } else if (settingsEntry.type == "button") {
                    this.idButtonMapping.set(settingsEntry.id, new ToolbarButton(settingsEntry.id, settingsEntry.label, settingsEntry.tooltip || settingsEntry.label, settingsEntry.icon));
                }

                this.idEntryMapping.set(settingsEntry.id, settingsEntry);
            });
        }

        this.trigger(new WaitForEvent(this, ProvideToolbarModelEvent.TYPE));
        this.trigger(new ComponentInitializedEvent(this));
    }

    public handle(e: ViewerEvent): void {
        if (e.type == ProvideToolbarModelEvent.TYPE) {
            const ptme = e as ProvideToolbarModelEvent;
            this.toolbarModel = ptme.model;

            this.idGroupMapping.forEach((k, v) => {
                ptme.model.addGroup(this.idGroupMapping.get(k));
            });

            this.idButtonMapping.forEach((k, v) => {
                const inGroup = this.idEntryMapping.get(k).inGroup;
                let group: ToolbarGroup = ptme.model.getGroups().filter(g => inGroup == g.name)[0];

                if (typeof group == "undefined") {
                    console.log("Can not find group " + inGroup + "!");
                    return;
                }

                group.addComponent(v);
            });

            this.toolbarButtonSync(this);
        }

        if (e.type == ButtonPressedEvent.TYPE) {
            let pressedID = e as ButtonPressedEvent;
            this.idEntryMapping.hasThen(pressedID.button.id, (entry) => {
                if ("action" in entry) {
                    entry.action.apply(this, e);
                }

                if ("href" in entry) {
                    window.location.href = entry.href;
                }
            });
        }

        if (e.type == LanguageModelLoadedEvent.TYPE) {
            this.languageModel = (e as LanguageModelLoadedEvent).languageModel;
            this.toolbarButtonSync(this);
        }


    }

    public get handlesEvents(): string[] {
        return [ProvideToolbarModelEvent.TYPE, ButtonPressedEvent.TYPE, LanguageModelLoadedEvent.TYPE];
    }

    public initLanguage() {
        this.idButtonMapping.forEach((k, v) => {
            v.label = this.languageModel.getTranslation(v.label);
            v.tooltip = this.languageModel.getTranslation(v.tooltip);
        });
    }
}


