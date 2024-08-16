declare global {
  interface Window {
    webApplicationBaseURL: string;
  }
}

export type JWT = {
  // eslint-disable-next-line camelcase
  login_success: boolean;
  // eslint-disable-next-line camelcase
  access_token: string;
  // eslint-disable-next-line camelcase
  token_type: string;
};

export const getWebApplicationBaseURL = (): string | undefined => window.webApplicationBaseURL;

export const fetchI18n = async (webApplicationBaseURL: string) =>
  (await fetch(`${webApplicationBaseURL}rsc/locale/translate/component.acl.accesskey.*`)).json();

export const fetchConfig = async (webApplicationBaseURL: string) =>
  (await fetch(`${webApplicationBaseURL}config.json`)).json();

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
  webApplicationBaseURL: string,
  objectId?: string,
  isSessionEnabled?: boolean
): Promise<JWT> => {
  if (objectId) {
    const params = new URLSearchParams();
    params.append("ua", `acckey_${objectId}`);
    if (isSessionEnabled) {
      params.append("sa", `acckey_${objectId}`);
    }
    const result = await fetch(`${webApplicationBaseURL}rsc/jwt?${params}`);
    return result.json();
  }
  const result = await fetch(`${webApplicationBaseURL}rsc/jwt`);
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

const shortString = (input: string, len: number) =>
  input.length > len ? `${input.slice(0, len - 3)}...` : input;

export const shortReference = (reference: string) => shortString(reference, 20);

export const getI18nKey = (value: string) => `component.acl.accesskey.frontend.${value}`;

export const urlEncode = (value: string) => encodeURIComponent(value);
