import {getBaseURL} from "@/api/BaseURL";

const mode = import.meta.env.MODE;

export const getLanguage = () => {
    const isDev = mode === 'development';
    return isDev ? "de" : (window as any).currentLang as string || "de";
}

export async function i18n(key: string): Promise<string> {
    const response = await fetch(`${getBaseURL()}rsc/locale/translate/${getLanguage()}/${key}`);
    return await response.text();
}