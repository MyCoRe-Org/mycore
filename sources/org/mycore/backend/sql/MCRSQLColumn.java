package org.mycore.backend.sql;

public class MCRSQLColumn {
    
    private String name;
    private String value;
    private String type;
    
    public MCRSQLColumn(){
        name = "";
        value = "";
        type = "";
    }
    
    public MCRSQLColumn(String name, String value, String type){
        this.name = name;
        this.value = value;
        this.type = type;
    }
    
    public String getType(){
        return type;
    }
    public void setType(String value){
        this.type = value;
    }
    
    public String getName(){
        return name;
    }
    public void setName(String value){
        this.name = value;
    }
    
    public String getValue(){
        return value;
    }
    public void setValue(String value){
        this.value = value;
    }

}
