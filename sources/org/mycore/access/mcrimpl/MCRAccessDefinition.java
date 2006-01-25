/*
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.access.mcrimpl;

import java.util.HashMap;

/**
 * Maps object ids to rules
 * 
 * @author Arne Seifert
 */
public class MCRAccessDefinition {
    
    private String objid;
    private HashMap pools = new HashMap();
    
    public MCRAccessDefinition(){
        pools.clear();
    }
    
    public String getObjID(){
        return objid;
    }
    
    public void setObjID(String value){
        objid = value;
    }
    
    public HashMap getPool(){
        return pools;
    }
    
    public void setPool(HashMap pool){
        pools = pool;
    }
    
    public void addPool(String poolname, String ruleid){
        pools.put(poolname, ruleid);
    }
    
    public void clearPools(){
        pools.clear();
    }
}
