namespace org.mycore.mets.tests {
    export class TestUtils {

        static createJson() {
            return {
                "rootSection" : {
                    "metsSectionList" : [
                        {
                            "metsSectionList" : [],
                            "id" : "7f747d91-ff2f-404b-8c29-e523b6a5d7a0",
                            "type" : "subSection",
                            "label" : "subSection1Label"
                        },
                        {
                            "metsSectionList" : [],
                            "id" : "1a43b22f-8d09-4a27-be20-e27eb9c9ae0e",
                            "type" : "subSection",
                            "label" : "subSection2Label"
                        },
                        {
                            "metsSectionList" : [],
                            "id" : "123123123-8d09-4a27-be20-e27eb9c9ae0e",
                            "type" : "subSection",
                            "label" : "subSection3Label"
                        }
                    ],
                    "id" : "e6cd8715-da98-4f4b-83f8-15228d62e505",
                    "type" : "testRootType",
                    "label" : "testRootLabel"
                },
                "metsPageList" : [
                    {
                        "id" : "feb2acda-c13f-4fb8-bd61-e8e2b4878670",
                        "orderLabel" : "1",
                        "contentIds" : "URN:special-urn1",
                        "fileList" : [
                            {
                                "id" : "625cfa55-c35e-457a-a6c2-6412cee68fb7",
                                "href" : "1.jpg",
                                "mimeType" : "image/jpeg",
                                "use" : "MASTER"
                            },
                            {
                                "id" : "ec0df29e-bf6b-499c-a9ba-a6066c8857b4",
                                "href" : "1.xml",
                                "mimeType" : "text/xml",
                                "use" : "ALTO"
                            }
                        ]
                    },
                    {
                        "id" : "f9dc256c-bea6-45c0-998d-9e1904b9484c",
                        "orderLabel" : "2",
                        "contentIds" : "URN:special-urn2",
                        "fileList" : [
                            {
                                "id" : "15e8bbf3-6467-497e-9b6a-55af67e1c408",
                                "href" : "2.jpg",
                                "mimeType" : "image/jpeg",
                                "use" : "MASTER"
                            },
                            {
                                "id" : "ae32b7f0-2bdc-47ad-88f9-92b034bd699a",
                                "href" : "2.xml",
                                "mimeType" : "text/xml",
                                "use" : "ALTO"
                            }
                        ]
                    },
                    {
                        "id" : "a6927332-97ab-4b72-8c7c-81e3fb79a0f0",
                        "orderLabel" : "3",
                        "contentIds" : "URN:special-urn3",
                        "fileList" : [
                            {
                                "id" : "0528e793-fab2-412b-acb2-2982313a9fee",
                                "href" : "3.jpg",
                                "mimeType" : "image/jpeg",
                                "use" : "MASTER"
                            },
                            {
                                "id" : "a3543e6a-be6d-4001-b81b-f211b7ce4a5c",
                                "href" : "3.xml",
                                "mimeType" : "text/xml",
                                "use" : "ALTO"
                            }
                        ]
                    }
                ],
                "sectionPageLinkList" : [
                    {
                        "from" : "e6cd8715-da98-4f4b-83f8-15228d62e505",
                        "to" : "feb2acda-c13f-4fb8-bd61-e8e2b4878670"
                    },
                    {
                        "from" : "7f747d91-ff2f-404b-8c29-e523b6a5d7a0",
                        "to" : "f9dc256c-bea6-45c0-998d-9e1904b9484c"
                    },
                    {
                        "from" : "1a43b22f-8d09-4a27-be20-e27eb9c9ae0e",
                        "to" : "a6927332-97ab-4b72-8c7c-81e3fb79a0f0"
                    }
                ]
            };
        }


        static createDefaultModel() {
            let metsEditorModel = new org.mycore.mets.model.MetsEditorModel(<any> {metsId : "test-mets"});
            metsEditorModel.onModelLoad(org.mycore.mets.model.simple.MCRMetsSimpleModel.fromJson(TestUtils.createJson()));
            return metsEditorModel;
        }

        static getWords(w) {
            return w.split(" ").map(
                (w) => w.replace(".", "")
                    .replace("(", "")
                    .replace((")"), "")
            );
        }
    }
}
