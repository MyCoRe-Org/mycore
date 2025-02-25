/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.mcr.acl.accesskey.service;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.mcr.acl.accesskey.config.MCRAccessKeyConfig;
import org.mycore.mcr.acl.accesskey.persistence.MCRAccessKeyRepository;
import org.mycore.mcr.acl.accesskey.persistence.MCRAccessKeyRepositoryImpl;
import org.mycore.mcr.acl.accesskey.service.processor.MCRAccessKeySecretProcessor;
import org.mycore.mcr.acl.accesskey.validation.MCRAccessKeyValidator;
import org.mycore.mcr.acl.accesskey.validation.MCRAccessKeyValidatorImpl;

/**
 * Factory class responsible for providing singleton instances of various
 * services related to access keys. It ensures thread-safe instantiation
 * of these services.
 */
public final class MCRAccessKeyServiceFactory {

    private MCRAccessKeyServiceFactory() {

    }

    /**
     * Returns the singleton instance of {@link MCRAccessKeyService}.
     * If the instance does not already exist, it will be created in a thread-safe manner.
     *
     * @return the singleton instance of {@link MCRAccessKeyService}.
     */
    public static MCRAccessKeyService getAccessKeyService() {
        return ServiceHolder.INSTANCE;
    }

    /**
     * Returns single access key user service instance.
     *
     * @return the instance
     */
    public static MCRAccessKeyUserService getAccessKeyUserService() {
        return UserServiceHolder.INSTANCE;
    }

    /**
     * Returns single access key session service instance.
     *
     * @return the instance
     */
    public static MCRAccessKeySessionService getAccessKeySessionService() {
        return SessionServiceHolder.INSTANCE;
    }

    private static MCRAccessKeyUserService createUserService(MCRAccessKeyService service) {
        return new MCRAccessKeyUserService(service);
    }

    private static MCRAccessKeySessionService createSessionService(MCRAccessKeyService service) {
        return new MCRAccessKeySessionService(service);
    }

    private static MCRAccessKeyService createAndConfigureService(MCRAccessKeyRepository accessKeyRepository,
        MCRAccessKeyValidator validator, MCRAccessKeySecretProcessor secretProcessor) {
        return new MCRAccessKeyServiceImpl(accessKeyRepository, validator, secretProcessor);
    }

    private static MCRAccessKeyRepository createRepository() {
        return new MCRAccessKeyRepositoryImpl();
    }

    private static MCRAccessKeyValidator createValidator() {
        return new MCRAccessKeyValidatorImpl();
    }

    private static MCRAccessKeySecretProcessor getSecretProcessor() {
        return MCRConfiguration2.getSingleInstanceOfOrThrow(
            MCRAccessKeySecretProcessor.class, MCRAccessKeyConfig.getSecretProcessorClassProperty());
    }

    private static final class ServiceHolder {
        private static final MCRAccessKeyService INSTANCE = createAndConfigureService(
            createRepository(), createValidator(), getSecretProcessor());
    }

    private static final class UserServiceHolder {
        private static final MCRAccessKeyUserService INSTANCE = createUserService(getAccessKeyService());
    }

    private static final class SessionServiceHolder {
        private static final MCRAccessKeySessionService INSTANCE = createSessionService(getAccessKeyService());
    }

}
