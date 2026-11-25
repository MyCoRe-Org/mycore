export const WEB_APPLICATION_BASE_URL = 'http://localhost:3000/mir/';

const globals = window as unknown as Record<string, unknown>;
globals.webApplicationBaseURL = WEB_APPLICATION_BASE_URL;
globals.currentLang = 'de';
