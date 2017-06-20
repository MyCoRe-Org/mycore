package org.mycore.datamodel.ifs2;

public class MCRIFS2MetadataTestCase extends MCRIFS2TestCase {
    private MCRMetadataStore metaDataStore;

    protected void createStore() throws Exception {
        setMetaDataStore(MCRStoreManager.createStore(STORE_ID, MCRMetadataStore.class));
    }

    public void setMetaDataStore(MCRMetadataStore metaDataStore) {
        this.metaDataStore = metaDataStore;
    }

    public MCRMetadataStore getMetaDataStore() {
        return metaDataStore;
    }

    @Override
    public MCRStore getGenericStore() {
        return getMetaDataStore();
    }
}
