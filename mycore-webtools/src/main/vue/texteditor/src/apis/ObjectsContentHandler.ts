export class ObjectsContentHandler implements ContentHandler {

    mcrApplicationBaseURL: string;

    constructor(webApplicationBaseURL: string) {
        this.mcrApplicationBaseURL = webApplicationBaseURL;
    }

    async load(id: string): Promise<Content> {
        try {
            const response = await fetch(`${this.mcrApplicationBaseURL}api/v2/objects/${id}`);
            const xml = await response.text();
            if (response.ok) {
                const contentType = response.headers.get("Content-Type");
                if (contentType === null) {
                    return Promise.reject(`Unable to load ${id}. Request was successful but the 'Content-Type' is empty.`);
                }
                return Promise.resolve({
                    data: xml,
                    type: contentType
                });
            }
            return Promise.reject(`Unable to load ${id}. Request failed with status code ${response.status}.`);
        } catch (err) {
            return Promise.reject(err);
        }
    }

    async save(id: string, content: Content): Promise<void> {
        try {
            const response = await fetch(`${this.mcrApplicationBaseURL}api/v2/objects/${id}`, {
                method: "PUT",
                headers: {
                    "Content-Type": `${content.type}; charset=utf-8`
                },
                body: content.data
            });
            if (response.ok) {
                return Promise.resolve();
            }
            return Promise.reject(`Unable to save ${id}. Request failed with status code ${response.status}.`);
        } catch (err) {
            return Promise.reject(err);
        }
    }

    async hasWriteAccess(id: string): Promise<boolean> {
        try {
            const response = await fetch(`${this.mcrApplicationBaseURL}api/v2/objects/${id}`, {
                method: "OPTIONS"/*,
                headers: {
                    "Access-Control-Request-Headers": "Allow"
                }*/
            });
            if (response.ok) {
                let allow = response.headers.get("Allow");
                console.log(allow);
                return Promise.resolve(true);
            }
            return Promise.reject(`Unable get OPTIONS of ${id}. Request failed with status code ${response.status}.`);
        } catch (err) {
            return Promise.reject(err);
        }
    }

}
