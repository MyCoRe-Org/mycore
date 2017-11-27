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

package org.mycore.frontend.classeditor.mocks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.mycore.datamodel.common.MCRLinkTableInterface;

public class LinkTableStoreMock implements MCRLinkTableInterface {

    @Override
    public void create(String from, String to, String type, String attr) {

    }

    @Override
    public void delete(String from, String to, String type) {

    }

    @Override
    public int countTo(String fromtype, String to, String type, String restriction) {
        return 0;
    }

    @Override
    public Map<String, Number> getCountedMapOfMCRTO(String mcrtoPrefix) {
        return new HashMap<>();
    }

    @Override
    public Collection<String> getSourcesOf(String to, String type) {
        return new ArrayList<>();
    }

    @Override
    public Collection<String> getDestinationsOf(String from, String type) {
        return new ArrayList<>();
    }
}
