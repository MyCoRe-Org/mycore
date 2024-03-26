export abstract class BaseContentHandler implements ContentHandler {

    mcrApplicationBaseURL: string;

    protected constructor(webApplicationBaseURL: string) {
        this.mcrApplicationBaseURL = webApplicationBaseURL;
    }

    abstract hasWriteAccess(id: string): Promise<boolean>;

    abstract load(id: string): Promise<Content>;

    abstract save(id: string, content: Content): Promise<void>;

    abstract dirtyAfterSave(id: string): boolean

    buildError(preText: string, response: Response) {
        const message = response.headers.get("X-Error-Message");
        const detail = response.headers.get("X-Error-Detail");
        const errorUUID = response.headers.get("X-Error-Uuid");
        return new Error(`${preText} Request failed with status code ${response.status}. ${message} - ${detail} -
         Error Log Code: ${errorUUID}`);
    }

    /**
     * Checks if the response 'Allow' header. If PUT is available, the user has write access.
     * TODO: Currently not supported by the rest v2 API. Should be used in future instead of "/try".
     *
     * @param id the id
     * @param response http response
     */
    handleWriteAccessResponse(id: string, response: Response) {
        if (!response.ok) {
            throw this.buildError(`Unable get OPTIONS of ${id}.`, response);
        }
        //TODO const allow = response.headers.get("Allow");
        const allow = "HEAD,DELETE,GET,OPTIONS,PUT";
        if (!allow) {
            throw new Error(`Unable get OPTIONS of ${id}. Request failed because 'Allow' header is empty.`);
        }
        return allow.toUpperCase().split(",").filter(s => s === "PUT").length >= 1;
    }

}
