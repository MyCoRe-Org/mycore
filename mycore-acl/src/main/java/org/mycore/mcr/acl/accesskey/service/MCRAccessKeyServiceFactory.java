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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

    private static volatile MCRAccessKeyService service;

    private static volatile MCRAccessKeyUserService userService;

    private static volatile MCRAccessKeySessionService sessionService;

    private static final Lock SERVICE_LOCK = new ReentrantLock();

    private static final Lock USER_SERVICE_LOCK = new ReentrantLock();

    private static final Lock SESSION_SERVICE_LOCK = new ReentrantLock();

    private MCRAccessKeyServiceFactory() {

    }

    /**
     * Returns the singleton instance of {@link MCRAccessKeyService}.
     * If the instance does not already exist, it will be created in a thread-safe manner.
     *
     * @return the singleton instance of {@link MCRAccessKeyService}.
     */
    public static MCRAccessKeyService getAccessKeyService() {
        if (service == null) {
            try {
                SERVICE_LOCK.lock();
                if (service == null) {
                    service = createAndConfigureService(createRepository(), createValidator(), getSecretProcessor());
                }
            } finally {
                SERVICE_LOCK.unlock();
            }
        }
        return service;
    }

    /**
     * Returns single access key user service instance.
     *
     * @return the instance
     */
    public static MCRAccessKeyUserService getAccessKeyUserService() {
        if (userService == null) {
            try {
                USER_SERVICE_LOCK.lock();
                if (userService == null) {
                    userService = createUserService(getAccessKeyService());
                }
            } finally {
                USER_SERVICE_LOCK.unlock();
            }
        }
        return userService;
    }

    /**
     * Returns single access key session service instance.
     *
     * @return the instance
     */
    public static MCRAccessKeySessionService getAccessKeySessionService() {
        if (sessionService == null) {
            try {
                SESSION_SERVICE_LOCK.lock();
                if (sessionService == null) {
                    sessionService = createSessionService(getAccessKeyService());
                }
            } finally {
                SESSION_SERVICE_LOCK.unlock();
            }
        }
        return sessionService;
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

}
