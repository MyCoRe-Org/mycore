package org.mycore.frontend.jersey.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MCRResourceSercurityConf {
    private Map<String, List<String>> resourceRegister;
    
    private MCRResourceSercurityConf() {
        setResourceRegister(new HashMap<String, List<String>>());
    }
    
    private static MCRResourceSercurityConf instance;
    
    public static MCRResourceSercurityConf instance(){
        if(instance == null){
            instance = new MCRResourceSercurityConf();
        }
        
        return instance;
    }
    
    public void registerResource(String resourceClass, String methodPath){
        List<String> methodPaths = getResourceRegister().get(resourceClass);
        if(methodPaths == null){
            methodPaths = new ArrayList<String>();
            getResourceRegister().put(resourceClass, methodPaths);
        }
        
        methodPaths.add(methodPath);
    }

    private void setResourceRegister(Map<String, List<String>> resourceRegister) {
        this.resourceRegister = resourceRegister;
    }

    public Map<String, List<String>> getResourceRegister() {
        return resourceRegister;
    }
}
