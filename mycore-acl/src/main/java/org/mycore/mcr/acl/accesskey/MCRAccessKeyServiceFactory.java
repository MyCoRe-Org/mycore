/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.mcr.acl.accesskey;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.mcr.acl.accesskey.persistence.MCRAccessKeyRepository;
import org.mycore.mcr.acl.accesskey.persistence.MCRAccessKeyRepositoryImpl;
import org.mycore.mcr.acl.accesskey.validation.MCRAccessKeyValidator;
import org.mycore.mcr.acl.accesskey.validation.MCRAccessKeyValidatorImpl;
import org.mycore.mcr.acl.accesskey.value.MCRAccessKeyValueProcessor;

/**
 * Provides default access key services.
 */
public class MCRAccessKeyServiceFactory {

    private static volatile MCRAccessKeyService service;

    private static final Object LOCK = new Object();

    private MCRAccessKeyServiceFactory() {

    }

    /**
     * Returns single access key service instance.
     *
     * @return the instance
     */
    public static MCRAccessKeyService getService() {
        if (service == null) {
            synchronized (LOCK) {
                if (service == null) {
                    service = createAndConfigureService(createRepository(), createValidator(), createValueProcessor());
                }
            }
        }
        return service;
    }

    private static MCRAccessKeyService createAndConfigureService(MCRAccessKeyRepository accessKeyRepository,
        MCRAccessKeyValidator validator, MCRAccessKeyValueProcessor valueProcessor) {
        return new MCRAccessKeyServiceImpl(accessKeyRepository, validator, valueProcessor);
    }

    private static MCRAccessKeyValueProcessor createValueProcessor() {
        return MCRConfiguration2.getInstanceOfOrThrow(MCRAccessKeyValueProcessor.class,
            "MCR.ACL.AccessKey.Service.ValueProcessor.Class");
    }

    private static MCRAccessKeyRepository createRepository() {
        return new MCRAccessKeyRepositoryImpl();
    }

    private static MCRAccessKeyValidator createValidator() {
        return new MCRAccessKeyValidatorImpl();
    }

}
