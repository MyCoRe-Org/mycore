/**
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
package org.mycore.backend.query;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

/**
 * @author Arne Seifert
 *
 */
public class MCRResults {
    
    static Logger logger = Logger.getLogger(MCRResults.class.getName());
    private int hits = 0;
    private List mcrhits = new LinkedList();
    private boolean issorted = false;
    private boolean iscomplete = false;
    
    
    public int getNumHits(){
        return hits;
    }
    
    
    public void addHit(MCRHit hit){
        mcrhits.add(hit);
    }
    
    public MCRHit getHit( int i ){
        if (i>0 && i< mcrhits.size()){
            return (MCRHit) mcrhits.get(i);
        }else{
            return null;
        }
    }
    
    public void setSorted(boolean value){
        issorted = value;
    }
    public boolean isSorted(){
        return issorted;
    }
    
    public boolean isComplete(){
        return iscomplete;
    }
    
    
    public void sort(){
        if (! issorted){
            if (iscomplete){
                //sort result
            }
        }
    }
    
    public Document toXML(){
        Document doc = new Document();
        doc.setRootElement(new Element("mcrresults"));
        doc.getRootElement().addContent(new Element("mcrhits"));
        
        for (int i=0; i<mcrhits.size(); i++){
            doc.getRootElement().getChild("mcrhits").addContent(((MCRHit)mcrhits.get(i)).getXMLElement());
        }
        return doc;
    }
    
    public String toString(){
        try{
            XMLOutputter out = new XMLOutputter(org.jdom.output.Format.getPrettyFormat());
            return out.outputString(toXML());
        }catch(Exception e){
            logger.error(e);
            return "";
        }
    }

    public static MCRResults and( MCRResults a, MCRResults b ){
        return a;
    }
    
    public static MCRResults or( MCRResults a, MCRResults b ){
        return a;
    }
    
    public static MCRResults merge( MCRResults a, MCRResults b ){
        return a;
    }

}
