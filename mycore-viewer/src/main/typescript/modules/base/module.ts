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

import { MyCoReViewer } from "./MyCoReViewer";
import { addViewerComponent } from "./Utils";
import { ViewerComponent } from "./components/ViewerComponent";
import { MyCoReViewerContainerComponent } from "./components/MyCoReViewerContainerComponent";
import { MyCoReI18NComponent } from "./components/MyCoReI18NComponent";
import { MyCoReImageOverviewComponent } from "./components/MyCoReImageOverviewComponent";
import { MyCoReToolbarComponent } from "./components/MyCoReToolbarComponent";
import { MyCoReImageScrollComponent } from "./components/MyCoReImageScrollComponent";
import { MyCoReChapterComponent } from "./components/MyCoReChapterComponent";
import { MyCoRePermalinkComponent } from "./components/MyCoRePermalinkComponent";
import { MyCoReLayerComponent } from "./components/MyCoReLayerComponent";
import { MyCoReButtonChangeComponent } from "./components/MyCoReButtonChangeComponent";

addViewerComponent(MyCoReViewerContainerComponent);
addViewerComponent(MyCoReI18NComponent);
addViewerComponent(MyCoReImageOverviewComponent);
addViewerComponent(MyCoReToolbarComponent);
addViewerComponent(MyCoReImageScrollComponent);
addViewerComponent(MyCoReChapterComponent);
addViewerComponent(MyCoRePermalinkComponent);


addViewerComponent(MyCoReLayerComponent);
addViewerComponent(MyCoReButtonChangeComponent);

export { MyCoReViewer, addViewerComponent, ViewerComponent }
