package org.mycore.mcr.acl.accesskey;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.mcr.acl.accesskey.access.MCRAccessKeyAccessService;
import org.mycore.mcr.acl.accesskey.access.MCRAccessKeyAccessServiceImpl;
import org.mycore.mcr.acl.accesskey.persistence.MCRAccessKeyRepository;
import org.mycore.mcr.acl.accesskey.persistence.MCRAccessKeyRepositoryImpl;
import org.mycore.mcr.acl.accesskey.value.MCRAccessKeyValueProcessor;

public class MCRAccessKeyServiceFactory {

    private static volatile MCRAccessKeyService service;

    private static final Object lock = new Object();

    public static MCRAccessKeyService getService() {
        if (service == null) {
            synchronized (lock) {
                if (service == null) {
                    service
                        = createAndConfigureService(createRepository(), createAccessService(), createValueProcessor());
                }
            }
        }
        return service;
    }

    private static MCRAccessKeyService createAndConfigureService(MCRAccessKeyRepository accessKeyRepository,
        MCRAccessKeyAccessService accessService, MCRAccessKeyValueProcessor valueProcessor) {
        final MCRAccessKeyServiceImpl service = new MCRAccessKeyServiceImpl(accessKeyRepository, accessService);
        service.setValueProcessor(valueProcessor);
        return service;
    }

    private static MCRAccessKeyValueProcessor createValueProcessor() {
        return MCRConfiguration2.getInstanceOfOrThrow(MCRAccessKeyValueProcessor.class,
            "MCR.ACL.AccessKey.Service.ValueProcessor.Class");
    }

    private static MCRAccessKeyRepository createRepository() {
        return new MCRAccessKeyRepositoryImpl();
    }

    private static MCRAccessKeyAccessService createAccessService() {
        return new MCRAccessKeyAccessServiceImpl();
    }
}
