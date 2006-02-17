/*
 * Created on 14.07.2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.mycore.services.oai;

import java.util.List;

/**
 * @author mycore
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface MCROAIResumptionTokenStore {
    
    public List getResumptionTokenHits(String resumptionTokenID, int requestedSize, int maxResults);
    
    public void createResumptionToken(String id, String prefix, String instance, List resultList);
    
    public String getPrefix(String token);
    
    public void deleteOutdatedTokens();

}
