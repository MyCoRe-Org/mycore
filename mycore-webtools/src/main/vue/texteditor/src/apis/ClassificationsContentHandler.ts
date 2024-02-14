export class ClassificationsContentHandler implements ContentHandler {

    mcrApplicationBaseURL: string;

    constructor(webApplicationBaseURL: string) {
        this.mcrApplicationBaseURL = webApplicationBaseURL;
    }

    async load(classificationId: string): Promise<Content> {
        try {
            const response = await fetch(`${this.mcrApplicationBaseURL}api/v2/classifications/${classificationId}`);
            const xml = await response.text();
            if (response.ok) {
                return Promise.resolve({
                    data: xml,
                    type: "application/xml"
                });
            }
            return Promise.reject(`Unable to load ${classificationId}. Request failed with status code ${response.status}.`);
        } catch (err) {
            return Promise.reject(err);
        }
    }

    async save(classificationId: string, content: Content): Promise<void> {
        try {
            const response = await fetch(`${this.mcrApplicationBaseURL}api/v2/classifications/${classificationId}`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/xml; charset=utf-8"
                },
                body: content.data
            });
            if (response.ok) {
                return Promise.resolve();
            }
            return Promise.reject(`Unable to save ${classificationId}. Request failed with status code ${response.status}.`);
        } catch (err) {
            return Promise.reject(err);
        }
    }

    async hasWriteAccess(id: string): Promise<boolean> {
        // TODO
        return Promise.resolve(true);
    }

}
