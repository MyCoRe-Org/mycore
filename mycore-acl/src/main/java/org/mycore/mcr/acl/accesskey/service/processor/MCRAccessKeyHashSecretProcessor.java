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

package org.mycore.mcr.acl.accesskey.service.processor;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.NoSuchAlgorithmException;
import java.util.function.Supplier;

import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;

/**
 * This {@link MCRAccessKeySecretProcessor} hashes secret and uses reference as salt.
 */
@MCRConfigurationProxy(proxyClass = MCRAccessKeyHashSecretProcessor.Factory.class)
public class MCRAccessKeyHashSecretProcessor implements MCRAccessKeySecretProcessor {

    private final int hashingIterations;

    /**
     * Constructs new {@link MCRAccessKeyHashSecretProcessor} with given hashing iterations
     *
     * @param hashingIterations the number of hashing iterations
     */
    public MCRAccessKeyHashSecretProcessor(int hashingIterations) {
        this.hashingIterations = hashingIterations;
    }

    @Override
    public String processSecret(String reference, String secret) {
        try {
            return MCRUtils.asSHA256String(hashingIterations, reference.getBytes(UTF_8), secret);
        } catch (NoSuchAlgorithmException e) {
            throw new MCRException("Cannot hash secret.", e);
        }

    }

    /**
     * This factory creates new {@link MCRAccessKeyHashSecretProcessor} and should be called by config.
     */
    public static class Factory implements Supplier<MCRAccessKeyHashSecretProcessor> {

        /**
         * Hashing iterations as string.
         */
        @MCRProperty(name = "MCR.ACL.AccessKey.SecretProcessor.Hash.HashIterations", absolute = true, required = true)
        public String hashingIterations;

        @Override
        public MCRAccessKeyHashSecretProcessor get() {
            return new MCRAccessKeyHashSecretProcessor(Integer.parseInt(hashingIterations));
        }
    }

}
