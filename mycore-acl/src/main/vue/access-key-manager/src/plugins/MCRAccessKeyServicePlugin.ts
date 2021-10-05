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

import _Vue from 'vue';
import axios, { AxiosInstance } from 'axios';
import MCRException from '@/common/MCRException';
import MCRAccessKey from '@/common/MCRAccessKey';
import { urlEncode } from '@/common/MCRUtils';

export interface MCRAccessKeyInformation {
  items: Array<MCRAccessKey>;
  totalResults: number;
}

export interface MCRAccessKeyServiceOptions {
  baseURL: string;
  objectID: string;
  token: string;
  derivateID?: string;
}

interface MCRErrorResponse {
  detail: string;
  errorCode: string;
  message: string;
  timestamp: string;
  uuid: string;
}

const SECRET_ENCODING_TYPE_NAME = 'base64url';

export default new class MCRAccessKeyServicePlugin {
  private instance: AxiosInstance;

  private objectID: string;

  private derivateID: string;

  public install(Vue: typeof _Vue, options: MCRAccessKeyServiceOptions) {
    /* eslint-disable */
    Vue.prototype.$client = this;
    /* eslint-enable */

    this.instance = axios.create({
      baseURL: `${options.baseURL}api/v2/objects/${options.objectID}/`,
      timeout: 2000,
    });

    this.objectID = options.objectID;
    this.derivateID = options.derivateID;

    this.instance.interceptors.response.use((response) => response, (error) => {
      const exception: MCRException = {};
      if (axios.isAxiosError(error)) {
        if (error.response) {
          if (error.response.status === 401 || error.response.status === 403) {
            exception.errorCode = 'noPermission';
          } else if (error.response.status >= 500) {
            exception.errorCode = 'server';
          } else {
            const errorResponse: MCRErrorResponse = error.response.data as MCRErrorResponse;
            const { errorCode } = errorResponse;
            if (errorCode != null) {
              if (errorCode !== 'UNKNOWN') {
                exception.errorCode = errorResponse.errorCode;
                exception.message = errorResponse.message;
              }
            }
          }
        } else if (error.request) {
          exception.errorCode = 'request';
        }
      }
      return Promise.reject(exception);
    });
    this.instance.defaults.headers.common.Authorization = `Bearer ${options.token}`;
  }

  public async getAccessKeys(): Promise<MCRAccessKeyInformation> {
    const url = this.derivateID != null ? `derivates/${this.derivateID}/accesskeys` : 'accesskeys';
    const response = await this.instance.get(url);
    return <MCRAccessKeyInformation>response.data;
  }

  public async getAccessKey(secret: string): Promise<MCRAccessKey> {
    const encodedSecret = urlEncode(secret);
    const url = this.derivateID != null ? `derivates/${this.derivateID}/accesskeys/${encodedSecret}` : `accesskeys/${encodedSecret}`;
    const response = await this.instance.get(url, {
      params: { secret_encoding: SECRET_ENCODING_TYPE_NAME },
    });
    return <MCRAccessKey>response.data;
  }

  public async addAccessKey(accessKey: MCRAccessKey): Promise<string> {
    const url = this.derivateID != null ? `derivates/${this.derivateID}/accesskeys` : 'accesskeys';
    const response = await this.instance.post(url, accessKey);
    const locationHeader = response.headers.location;
    return locationHeader.substr(locationHeader.indexOf('/accesskeys/') + '/accesskeys/'.length);
  }

  public async updateAccessKey(secret: string, accessKey: MCRAccessKey): Promise<void> {
    const encodedSecret = urlEncode(secret);
    const url = this.derivateID != null ? `derivates/${this.derivateID}/accesskeys/${encodedSecret}` : `/accesskeys/${encodedSecret}`;
    await this.instance.put(url, accessKey, {
      params: { secret_encoding: SECRET_ENCODING_TYPE_NAME },
    });
  }

  public async removeAccessKey(secret: string): Promise<void> {
    const encodedSecret = urlEncode(secret);
    const url = this.derivateID != null ? `derivates/${this.derivateID}/accesskeys/${encodedSecret}` : `accesskeys/${encodedSecret}`;
    await this.instance.delete(url, {
      params: { secret_encoding: SECRET_ENCODING_TYPE_NAME },
    });
  }
}();
