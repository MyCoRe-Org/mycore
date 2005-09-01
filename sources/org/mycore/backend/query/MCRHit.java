package org.mycore.backend.query;

import java.util.HashMap;
import java.util.Iterator;

import org.jdom.Attribute;
import org.jdom.Element;

public class MCRHit
{
    
    private String id;
    private HashMap map = new HashMap();
    
    public MCRHit(){
    }
    
    public MCRHit(String id){
        this.id = id;
    }
    
  public String getID(){
      return id;
  }
  public void setID(String id){
      this.id = id;
  }
  
  public HashMap getMetadata(){
      return map;
  }
  
  public void addMetaValue(String key, String value){
      try{
          map.put(key, value);
      }catch(Exception e){
          e.printStackTrace();
      }
  }
  
  public Element getXMLElement(){
      Element el = new Element("mcrhit");
      try{
          el.setAttribute(new Attribute("mcrid", this.id));
          el.addContent(new Element("metadata"));
          Iterator it = map.keySet().iterator();   
          while(it.hasNext()){
              String key = (String) it.next();
              el.getChild("metadata").addContent(
                      new Element("meta")
                      .setAttribute(new Attribute("name", key))
                      .setAttribute(new Attribute("value", (String) map.get(key))) 
              );
          }
      }catch(Exception e){
          e.printStackTrace();
      }
      return el;
  }
  
  
}
