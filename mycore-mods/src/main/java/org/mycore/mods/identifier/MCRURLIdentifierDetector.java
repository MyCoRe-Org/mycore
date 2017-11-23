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

package org.mycore.mods.identifier;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class MCRURLIdentifierDetector implements MCRIdentifierDetector<URI> {

    private Set<MCRIdentifierDetector<URI>> normalizers = new HashSet<>();

    public MCRURLIdentifierDetector(Collection<MCRIdentifierDetector<URI>> normalizers) {
        this.normalizers.addAll(normalizers);
    }

    public MCRURLIdentifierDetector() {

    }

    public void addDetector(MCRIdentifierDetector<URI> identifierDetector) {
        if (!normalizers.contains(identifierDetector)) {
            normalizers.add(identifierDetector);
        }
    }

    public void removeDetector(MCRIdentifierDetector<URI> identifierDetector) {
        if (normalizers.contains(identifierDetector)) {
            normalizers.remove(identifierDetector);
        }
    }

    @Override
    public Optional<Map.Entry<String, String>> detect(URI resolvable) {
        return this.normalizers.stream()
            .map(detector -> detector.detect(resolvable))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();
    }

}
