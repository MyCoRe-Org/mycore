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

import {addViewerComponent} from "../base/Utils";
import {MyCoReFrameToolbarProviderComponent} from "./components/MyCoReFrameToolbarProviderComponent";
import {MyCoReImageInformationComponent} from "../desktop/components/MyCoReImageInformationComponent";
import {MyCoRePageDesktopLayoutProviderComponent} from "../desktop/components/MyCoRePageDesktopLayoutProviderComponent";
import {MyCoReSearchComponent} from "../desktop/components/MyCoReSearchComponent";
import {MyCoReLocalViewerIndexSearcherProvider} from "../desktop/components/MyCoReLocalViewerIndexSearcherProvider";
import {register as registerToolbarFactory} from "../desktop/widgets/toolbar/view/BootstrapToolbarViewFactory";
import {MyCoReDesktopToolbarProviderComponent} from "../desktop/components/MyCoReDesktopToolbarProviderComponent";

addViewerComponent(MyCoReFrameToolbarProviderComponent);
addViewerComponent(MyCoReImageInformationComponent);
addViewerComponent(MyCoRePageDesktopLayoutProviderComponent);
addViewerComponent(MyCoReSearchComponent);
addViewerComponent(MyCoReLocalViewerIndexSearcherProvider);
registerToolbarFactory();
