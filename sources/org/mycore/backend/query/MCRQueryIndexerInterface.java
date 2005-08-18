package org.mycore.backend.query;


import java.util.List;

import org.mycore.datamodel.metadata.MCRObjectID;

public interface MCRQueryIndexerInterface {

    public void initialLoad();
    
    public void updateObject(MCRObjectID objectid);
    
    public void deleteObject(MCRObjectID objectid);
    
    public void insertInQuery(String mcrid, List values);
   
    public void updateConfiguration();
}
