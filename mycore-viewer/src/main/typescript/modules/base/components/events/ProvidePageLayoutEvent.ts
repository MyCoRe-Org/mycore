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


import { MyCoReImageViewerEvent } from "./MyCoReImageViewerEvent";
import { ViewerComponent } from "../ViewerComponent";
import { PageLayout } from "../../widgets/canvas/PageLayout";

/**
 * Used to provide a PageLayout wich can be used by the MyCoReImageScrollComponent.
 */
export class ProvidePageLayoutEvent extends MyCoReImageViewerEvent {

  constructor(component: ViewerComponent, public pageLayout: PageLayout, public isDefault = false) {
    super(component, ProvidePageLayoutEvent.TYPE);
  }

  public static TYPE = "ProvidePageLayoutEvent";

}
