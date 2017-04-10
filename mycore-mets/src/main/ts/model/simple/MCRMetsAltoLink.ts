///<reference path="MCRMetsFile.ts"/>
namespace org.mycore.mets.model.simple {
    export class MCRMetsAltoLink {
        constructor(public altoFile: MCRMetsFile, public begin: string, public end: string) {
        }

        public static copy(file: MCRMetsAltoLink) {
            return new MCRMetsAltoLink(file.altoFile, file.begin, file.end);
        }
    }
}
