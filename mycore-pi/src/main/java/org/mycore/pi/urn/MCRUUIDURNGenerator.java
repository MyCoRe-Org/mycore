package org.mycore.pi.urn;

import java.util.UUID;

/**
 * Builds a new, unique NISS using Java implementation of the UUID
 * specification. java.util.UUID creates 'only' version 4 UUIDs.
 * Version 4 UUIDs are generated from a large random number and do
 * not include the MAC address.
 *
 * UUID = 8*HEX "-" 4*HEX "-" 4*HEX "-" 4*HEX "-" 12*HEX
 * Example One: 067e6162-3b6f-4ae2-a171-2470b63dff00
 * Example Two: 54947df8-0e9e-4471-a2f9-9af509fb5889
 *
 * @author Kathleen Neumann (kkrebs)
 * @author Sebastian Hofmann
 */
public class MCRUUIDURNGenerator extends MCRDNBURNGenerator {

    public MCRUUIDURNGenerator(String generatorID) {
        super(generatorID);
    }

    @Override
    protected String buildNISS() {
        return UUID.randomUUID().toString();
    }
}
