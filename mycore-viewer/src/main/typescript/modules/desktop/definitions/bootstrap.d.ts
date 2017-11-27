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

// Type definitions for Bootstrap 2.2
// Project: http://twitter.github.com/bootstrap/
// Definitions by: Boris Yankov <https://github.com/borisyankov/>
// Definitions: https://github.com/borisyankov/DefinitelyTyped


interface ModalOptions {
    backdrop?: boolean;
    keyboard?: boolean;
    show?: boolean;
    remote?: string;
}

interface ModalOptionsBackdropString {
    backdrop?: string; // for "static"
    keyboard?: boolean;
    show?: boolean;
    remote?: string;
}

interface ScrollSpyOptions {
    offset?: number;
}

interface TooltipOptions {
    animation?: boolean;
    html?: boolean;
    placement?: any;
    selector?: string;
    title?: any;
    trigger?: string;
    delay?: any;
    container?: any;
}

interface PopoverOptions {
    animation?: boolean;
    html?: boolean;
    placement?: any;
    selector?: string;
    trigger?: string;
    title?: any;
    content?: any;
    delay?: any;
    container?: any;
}

interface CollapseOptions {
    parent?: any;    
    toggle?: boolean;
}

interface CarouselOptions {
    interval?: number;
    pause?: string;
}

interface TypeaheadOptions {
    source?: any;
    items?: number;
    minLength?: number;
    matcher?: (item: any) => boolean;
    sorter?: (items: any[]) => any[];
    updater?: (item: any) => any;
    highlighter?: (item: any) => string;
}

interface AffixOptions {
    offset?: any;
}

interface JQuery {
    modal(options?: ModalOptions): JQuery;
    modal(options?: ModalOptionsBackdropString): JQuery;
    modal(command: string): JQuery;

    dropdown(): JQuery;
    dropdown(command: string): JQuery;

    scrollspy(command: string): JQuery;
    scrollspy(options?: ScrollSpyOptions): JQuery;

    tab(): JQuery;
    tab(command: string): JQuery;

    tooltip(options?: TooltipOptions): JQuery;
    tooltip(command: string): JQuery;

    popover(options?: PopoverOptions): JQuery;
    popover(command: string): JQuery;

    alert(): JQuery;
    alert(command: string): JQuery;

    //button(): JQuery;
    //button(command: string): JQuery;

    collapse(options?: CollapseOptions): JQuery;
    collapse(command: string): JQuery;

    carousel(options?: CarouselOptions): JQuery;
    carousel(command: string): JQuery;

    typeahead(options?: TypeaheadOptions): JQuery;

    affix(options?: AffixOptions): JQuery;
}
