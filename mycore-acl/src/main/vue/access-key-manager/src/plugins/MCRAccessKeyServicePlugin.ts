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
import axios, { AxiosInstance } from "axios";
import MCRException from "@/common/MCRException"
import MCRAccessKey from '@/common/MCRAccessKey';

export interface MCRAccessKeyInformation {
  items: Array<MCRAccessKey>;
  totalResults: number;
}

export interface MCRAccessKeyServiceOptions {
  baseURL: string;
  objectID: string;
  token: string;
}

interface MCRErrorResponse {
  detail: string;
  errorCode: string;
  message: string;
  timestamp: string;
  uuid: string;
}

export default new class MCRAccessKeyServicePlugin {

  private instance: AxiosInstance;

  public install(Vue: typeof _Vue, options: MCRAccessKeyServiceOptions) {
    Vue.prototype.$client = this;

    this.instance = axios.create({
      baseURL: `${options.baseURL}api/v2/objects/${options.objectID}/`,
      timeout: 2000
    });

    this.instance.interceptors.response.use(function (response) {
      return response;
    }, function (error) {
      const exception: MCRException = {};
      if (axios.isAxiosError(error)) {
        if (error.response) {
          if (error.response.status == 401) {
            exception.errorCode = "noPermission";
          } else if (error.response.status >= 500) {
             exception.errorCode = "server";
          } else {
            const errorResponse: MCRErrorResponse = error.response.data as MCRErrorResponse;
            if (errorResponse.errorCode != null) {
              exception.errorCode = errorResponse.errorCode;
              exception.message = errorResponse.message;
            }
          }
        } else if (error.request) {
           exception.errorCode = "request";
        }
      }
      return Promise.reject(exception);
    });

    this.instance.defaults.headers.common['Authorization'] = `Bearer ${options.token}`;
  }

  public async getAccessKeys(): Promise<MCRAccessKeyInformation> {
    const response = await this.instance.get("accesskeys");
    return <MCRAccessKeyInformation>response.data;
  }

  public async getAccessKey(id: string): Promise<MCRAccessKey> {
    const response = await this.instance.get(`accesskeys/${id}`);
    return response.data;
  }

  public async addAccessKey(accessKey: MCRAccessKey): Promise<string> {
    const response = await this.instance.post("accesskeys", accessKey);
    return response.headers["location"].split("/").pop();
  }

  public async updateAccessKey(accessKey: MCRAccessKey): Promise<void> {
    await this.instance.put(`/accesskeys/${accessKey.value}`, accessKey);
  }

  public async removeAccessKey(id: string): Promise<void> {
    await this.instance.delete(`accesskeys/${id}`);
  }
}
