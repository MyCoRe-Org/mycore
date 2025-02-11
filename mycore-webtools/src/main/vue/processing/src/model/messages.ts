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

export interface ProcessingMessage {

    type: string;

}

export interface ConnectMessage extends ProcessingMessage {
}

export interface RegistryMessage extends ProcessingMessage {
}

export interface AddCollectionMessage extends ProcessingMessage {

    id: number;

    name: string;

    properties: { [name: string]: any };

}

export interface UpdateProcessableMessage extends ProcessingMessage {

    id: number;

    name: string;

    collectionId: number;

    status: string;

    user: string;

    createTime: number;

    startTime: number;

    progressText: string | undefined;

    properties: { [name: string]: any };

}

export interface UpdateCollectionPropertyMessage extends ProcessingMessage {

    id: number;

    propertyName: string;

    propertyValue: any;

}

export interface ErrorMessage extends ProcessingMessage {

    error: string;

}
