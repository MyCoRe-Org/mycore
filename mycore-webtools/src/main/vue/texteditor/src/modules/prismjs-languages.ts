// default supported languages
// Markup + HTML + XML + SVG + MathML + SSML + Atom + RSS

// imported prism languages
// see https://prismjs.com/download.html for a full list
import "prismjs/components/prism-markdown";
import "prismjs/components/prism-csv";
import "prismjs/components/prism-yaml";

import type { Grammar } from "prismjs";
import { languages } from 'prismjs';

/**
 * Selects a prismjs language based on a given content type. If no type can be found undefined is returned.
 */
export function getPrismLanguage(contentType: string): PrismLanguage | undefined {
  let contentTypeSplit = contentType.split("/");
  if (contentTypeSplit.length !== 2) {
    throw new Error("Invalid contentType " + contentType);
  }
  const language = contentTypeSplit[1];
  if (language === undefined || languages[language] === undefined) {
    return undefined;
  }
  return {
    language: language,
    grammar: languages[language]
  }
}

export interface PrismLanguage {

  language: string,

  grammar: Grammar

}
