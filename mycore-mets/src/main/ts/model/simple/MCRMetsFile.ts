namespace org.mycore.mets.model.simple {
    /**
     * Represents a File in a mets.xml
     */
    export class MCRMetsFile {

        constructor(public id: string,
                    public href: string,
                    public mimeType: string,
                    public use: string) {
        }

        public static copy(file: MCRMetsFile) {
            return new MCRMetsFile(file.id, file.href, file.mimeType, file.use);
        }

    }
}
