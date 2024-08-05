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

package org.mycore.mcr.acl.accesskey.value;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.NoSuchAlgorithmException;
import java.util.function.Supplier;

import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;

/**
 * This {@link MCRAccessKeyValueProcessor} hashes value and uses reference as salt.
 */
@MCRConfigurationProxy(proxyClass = MCRAccessKeyHashValueProcessor.Factory.class)
public class MCRAccessKeyHashValueProcessor implements MCRAccessKeyValueProcessor {

    private final int hashingIterations;

    public MCRAccessKeyHashValueProcessor(int hashingIterations) {
        this.hashingIterations = hashingIterations;
    }

    @Override
    public String getValue(String reference, String value) {
        try {
            return MCRUtils.asSHA256String(hashingIterations, reference.getBytes(UTF_8), value);
        } catch (NoSuchAlgorithmException e) {
            throw new MCRException("Cannot hash secret.", e);
        }

    }

    public static class Factory implements Supplier<MCRAccessKeyHashValueProcessor> {

        @MCRProperty(name = "MCR.ACL.AccessKey.ValueProcessor.Hash.HashIterations", absolute = true, required = true)
        public String hashingIterations;

        @Override
        public MCRAccessKeyHashValueProcessor get() {
            return new MCRAccessKeyHashValueProcessor(Integer.parseInt(this.hashingIterations));
        }
    }

}
