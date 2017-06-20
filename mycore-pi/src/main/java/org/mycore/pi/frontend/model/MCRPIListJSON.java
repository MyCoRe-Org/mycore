package org.mycore.pi.frontend.model;

import java.util.List;

import org.mycore.pi.MCRPIRegistrationInfo;

public class MCRPIListJSON {

    public MCRPIListJSON(String type, int from, int size, int count, List<MCRPIRegistrationInfo> list) {
        this.type = type;
        this.from = from;
        this.size = size;
        this.count = count;
        this.list = list;
    }

    public String type;

    public int from;

    public int size;

    public int count;

    public List<MCRPIRegistrationInfo> list;
}
