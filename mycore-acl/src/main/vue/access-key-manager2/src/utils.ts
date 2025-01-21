import { Config } from "./config";

declare global {
  interface Window {
    webApplicationBaseURL: string;
  }
}

const ALLOWED_SESSION_TYPES = "MCR.ACL.AccessKey.Strategy.AllowedSessionPermissionTypes";

export type JWT = {
  // eslint-disable-next-line camelcase
  login_success: boolean;
  // eslint-disable-next-line camelcase
  access_token: string;
  // eslint-disable-next-line camelcase
  token_type: string;
};

export const BASE_URL = window.webApplicationBaseURL as string;

export const fetchTranslations = async (baseUrl: string): Promise<Record<string, string>> => {
  const response = await fetch(`${baseUrl}rsc/locale/translate/component.acl.accesskey.*`);
  if (!response.ok) {
    throw new Error('Failed to load translations');
  }
  return await response.json();
}

export const fetchConfig = async (baseUrl: string): Promise<Config> => {
  const response = await fetch(`${baseUrl}config.json`);
  if (!response.ok) {
    throw new Error('Failed to load app configuration');
  }
  const config = await response.json();
  return {
    isSessionEnabled: config[ALLOWED_SESSION_TYPES] !== undefined && config[ALLOWED_SESSION_TYPES].length > 0,
    allowedSessionPermissionTypes: config[ALLOWED_SESSION_TYPES].split(","),
  } as Config;
};

const getQueryParameterByName = (
  parameterName: string,
  url = window.location.href.toLowerCase()
): string | null => {
  return new URL(url).searchParams.get(parameterName);
};

export const getReference = (): string | undefined => {
  const objectId = getQueryParameterByName("reference");
  if (objectId) {
    return objectId;
  }
  return undefined;
};

export const getAvailablePermissions = (): string[] | undefined => {
  const availablePermissionsString = getQueryParameterByName("availablepermissions");
  if (availablePermissionsString) {
    return availablePermissionsString.split(",");
  }
  return undefined;
};

export const fetchJWT = async (
  baseUrl: string,
  reference?: string,
  isSessionEnabled?: boolean
): Promise<JWT> => {
  if (reference) {
    const params = new URLSearchParams();
    params.append("ua", `acckey_${reference}`);
    if (isSessionEnabled) {
      params.append("sa", `acckey_${reference}`);
    }
    const result = await fetch(`${baseUrl}rsc/jwt?${params}`);
    return result.json();
  }
  const result = await fetch(`${baseUrl}rsc/jwt`);
  return result.json();
};

export const generateRandomString = (length: number): string => {
  const keylistalpha = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
  const keylistint = "123456789";
  const keylistspec = "!@#_%$";
  let temp = "";
  let len = length / 2;
  len -= 1;
  const lenspec = length - len - len;
  for (let i = 0; i < len; i += 1) {
    temp += keylistalpha.charAt(Math.floor(Math.random() * keylistalpha.length));
  }
  for (let i = 0; i < lenspec; i += 1) {
    temp += keylistspec.charAt(Math.floor(Math.random() * keylistspec.length));
  }
  for (let i = 0; i < len; i += 1) {
    temp += keylistint.charAt(Math.floor(Math.random() * keylistint.length));
  }
  temp = temp
    .split("")
    .sort(() => 0.5 - Math.random())
    .join("");
  return temp;
};

const shortString = (input: string, len: number): string =>
  input.length > len ? `${input.slice(0, len - 3)}...` : input;

export const shortReference = (reference: string): string => shortString(reference, 20);

export const getI18nKey = (value: string): string => `component.acl.accesskey.frontend.${value}`;

export const urlEncode = (value: string): string => encodeURIComponent(value);
