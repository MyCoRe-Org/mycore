import { AppConfig, AccessKeyConfig } from './types';

interface RawConfig {
  webApplicationBaseURL: string;
  currentLang: string;
  'MCR.ACL.AccessKey.Strategy.AllowedSessionPermissionTypes': string;
}

const rawConfig = (
  import.meta.env.DEV === true
    ? Object.fromEntries(
        Object.entries(import.meta.env)
          .filter(([key]) => key.startsWith('VITE_APP_CONFIG_'))
          .map(([key, value]) => [key.replace('VITE_APP_CONFIG_', ''), value])
      )
    : window.mycore
) as RawConfig;

export const appConfig: AppConfig = {
  baseUrl: rawConfig.webApplicationBaseURL,
  currentLang: rawConfig.currentLang,
};

export const accessKeyConfig: AccessKeyConfig = {
  allowedAccessKeySessionPermissions:
    rawConfig['MCR.ACL.AccessKey.Strategy.AllowedSessionPermissionTypes'].split(
      ','
    ),
};

export const I18N_PREFIX = 'component.acl.accesskey.*';
