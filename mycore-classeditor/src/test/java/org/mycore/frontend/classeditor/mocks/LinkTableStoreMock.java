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
        return new HashMap<String, Number>();
    }

    @Override
    public Collection<String> getSourcesOf(String to, String type) {
        return new ArrayList<String>();
    }

    @Override
    public Collection<String> getDestinationsOf(String from, String type) {
        return new ArrayList<String>();
    }
}
