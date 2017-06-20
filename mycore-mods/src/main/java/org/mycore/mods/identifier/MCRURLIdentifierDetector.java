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
