/*
 * $Revision$ $Date$
 * 
 * This file is part of *** M y C o R e *** See http://www.mycore.de/ for
 * details.
 * 
 * This program is free software; you can use it, redistribute it and / or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program, in a file called gpl.txt or license.txt. If not, write to the
 * Free Software Foundation Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307 USA
 */
package org.mycore.oai;

import static org.mycore.oai.pmh.OAIConstants.NS_OAI;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.hibernate.Transaction;
import org.jdom2.Element;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.oai.classmapping.MCRClassificationAndSetMapper;
import org.mycore.oai.pmh.OAIConstants;
import org.mycore.oai.pmh.OAIDataList;
import org.mycore.oai.pmh.Set;
import org.mycore.parsers.bool.MCRAndCondition;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.services.fieldquery.MCRQuery;
import org.mycore.services.fieldquery.MCRQueryManager;

/**
 * Manager class to handle OAI-PMH set specific behavior. For a data provider instance, set support is optional and must be configured as described below.
 * Typically, sets are mapped to categories of a classification in MyCoRe. The set specifications are read from one or more URIs using MCRURIResolver. This
 * allows for sets that are typically built by applying an xsl stylesheet to the output of the classification URI resolver, but also for other ways to
 * dynamically create set specifications, or for a static set specification that is read from an xml file. Example:
 * MCR.OAIDataProvider.OAI.Sets.OA=webapp:oai/open_access.xml MCR.OAIDataProvider.OAI.Sets.DDC=xslStyle:classification2sets:classification:DDC The first line
 * reads a set specification from a static xml file stored in the web application. The DINI certificate demands that there always is a set open_access that
 * contains all public Open Access documents. Since this set always exists, its set specification can be read from a static file. The second line uses the
 * classification resolver to read in a classification, then transforms the xml to build set specifications from the listed categories. It is recommended not to
 * list sets that are completely empty, to simplify harvesting. The fastest way to accomplish this is to somehow ensure that no set specifications from empty
 * sets are delivered from the URIs, which means that the classification resolver filters out empty categories, or the xsl stylesheet somehow decides to filter
 * empty sets. Another way to filter out empty sets can be activated by setting a property: MCR.OAIDataProvider.OAI.FilterEmptySets=true When set to true, the
 * MCRSetManager handler filters out empty sets itself after reading in the URIs. This is done by constructing a query for each set and looking for matching
 * hits. Set queries are built using the OAI Adapter's buildSetCondition method. Filtering empty sets this way may be useful for some implementations, but it is
 * slower and should be avoided for large set hierarchies.
 * 
 * @see MCRURIResolver
 * @author Frank L\u00fctzenkirchen
 * @author Matthias Eichner
 */
public class MCROAISetManager {

    protected final static Logger LOGGER = Logger.getLogger(MCROAISetManager.class);

    protected String configPrefix;

    protected List<String> setURIs;

    /**
     * Time in milliseconds when the classification changed.
     */
    protected long classLastModified;

    /**
     * Time in minutes.
     */
    protected int cacheMaxAge;

    protected boolean filterEmptySets;

    protected final OAIDataList<Set> cachedSetList;

    public MCROAISetManager() {
        this.setURIs = new ArrayList<String>();
        this.cachedSetList = new OAIDataList<Set>();
        this.classLastModified = Long.MIN_VALUE;
    }

    /**
     * Use {@link #init(String, int, boolean)} instead.
     * 
     * @param configPrefix
     */
    @Deprecated
    public void init(String configPrefix) {
        this.configPrefix = configPrefix;
        this.cacheMaxAge = MCRConfiguration.instance().getInt(this.configPrefix + "SetCache.MaxAge", 0);
        this.filterEmptySets = MCRConfiguration.instance().getBoolean(this.configPrefix + "FilterEmptySets", true);
        updateURIs();
        if (this.cacheMaxAge != 0) {
            startTimerTask();
        }
    }

    public void init(String configPrefix, int cacheMaxAge, boolean filterEmptySets) {
        this.configPrefix = configPrefix;
        this.cacheMaxAge = cacheMaxAge;
        this.filterEmptySets = filterEmptySets;
        updateURIs();
        if (this.cacheMaxAge != 0) {
            startTimerTask();
        }
    }

    protected void startTimerTask() {
        long maxAgeInMilli = this.cacheMaxAge * 60 * 1000;
        TimerTask tt = new TimerTask() {
            public void run() {
                MCRSession session = MCRSessionMgr.getCurrentSession();//create a new session for this thread
                MCRSessionMgr.setCurrentSession(session);//store session in this thread
                session.setUserInformation(MCRSystemUserInformation.getSystemUserInstance());
                Transaction transaction = MCRHIBConnection.instance().getSession().beginTransaction();
                try {
                    LOGGER.info("update oai set list");
                    synchronized (cachedSetList) {
                        OAIDataList<Set> setList = createSetList();
                        cachedSetList.clear();
                        cachedSetList.addAll(setList);
                    }
                } finally {
                    try {
                        transaction.commit();
                    } catch(Exception exc) {
                        LOGGER.error("Error occured while retrieving oai set list", exc);
                        transaction.rollback();
                    }
                    MCRSessionMgr.releaseCurrentSession();//release so this session is not returned by getCurrentSession
                    session.close();//no further need for this session
                }
            }
        };
        new Timer().schedule(tt, new Date(System.currentTimeMillis() + maxAgeInMilli), maxAgeInMilli);
    }

    protected void updateURIs() {
        this.setURIs = new ArrayList<String>();
        MCRConfiguration config = MCRConfiguration.instance();
        Properties setProperties = config.getProperties(this.configPrefix + "Sets.");
        for (Object o : setProperties.values()) {
            String value = (String) o;
            if (value.trim().length() > 0) {
                this.setURIs.add(value);
            }
        }
    }

    /**
     * Returns a list of OAI-PMH sets defined by MyCoRe.
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    public OAIDataList<Set> get() {
        // no cache
        if (this.cacheMaxAge == 0) {
            return createSetList();
        }
        // cache
        // check if classification changed
        long lastModified = MCRCategoryDAOFactory.getInstance().getLastModified();
        if (lastModified != this.classLastModified) {
            this.classLastModified = lastModified;
            synchronized (this.cachedSetList) {
                OAIDataList<Set> setList = createSetList();
                cachedSetList.clear();
                cachedSetList.addAll(setList);
            }
        }
        // create a shallow copy of the set list
        OAIDataList<Set> clonedList;
        synchronized (this.cachedSetList) {
            clonedList = (OAIDataList<Set>) this.cachedSetList.clone();
        }
        return clonedList;
    }

    @SuppressWarnings("unchecked")
    protected OAIDataList<Set> createSetList() {
        OAIDataList<Set> setList = new OAIDataList<Set>();
        for (String uri : this.setURIs) {
            Element resolved = MCRURIResolver.instance().resolve(uri);
            for (Element setElement : (List<Element>) (resolved.getChildren("set", OAIConstants.NS_OAI))) {
                String setSpec = setElement.getChildText("setSpec", NS_OAI);
                String setName = setElement.getChildText("setName", NS_OAI);
                if (!contains(setSpec, setList)) {
                    if (setSpec.contains(":")) {
                        String classID = setSpec.substring(0, setSpec.indexOf(':')).trim();
                        classID = MCRClassificationAndSetMapper.mapClassificationToSet(this.configPrefix, classID);
                        String newSetSpec = classID + setSpec.substring(setSpec.indexOf(':'));
                        setList.add(new Set(newSetSpec, setName));
                    } else {
                        setList.add(new Set(setSpec, setName));
                    }
                }
            }
        }

        // Filter out empty sets
        if (this.filterEmptySets) {
            for (Iterator<Set> it = setList.iterator(); it.hasNext();) {
                Set set = it.next();
                String setSpec = set.getSpec();
                // Check parent set, if existing
                if (setSpec.contains(":") && (setSpec.lastIndexOf(":") > setSpec.indexOf(":"))) {
                    String parentSetSpec = setSpec.substring(0, setSpec.lastIndexOf(":"));
                    // If parent set is empty, all child sets must be empty, too
                    if (!contains(parentSetSpec, setList)) {
                        it.remove();
                        continue;
                    }
                }
                // Build a query to count results
                MCRAndCondition query = new MCRAndCondition();
                query.addChild(buildSetCondition(setSpec));
                MCRCondition restriction = MCROAIUtils.getDefaultRestriction(this.configPrefix);
                if (restriction != null) {
                    query.addChild(restriction);
                }
                if (MCRQueryManager.search(new MCRQuery(query)).getNumHits() == 0) {
                    it.remove();
                }
            }
        }
        return setList;
    }

    /**
     * Returns the set with the specified setSpec from the set list or null, if no set with that setSpec is found.
     * 
     * @param setSpec
     *            identifier of the set
     * @param setList
     *            list of sets
     * @return the set with setSpec
     */
    public static Set get(String setSpec, OAIDataList<Set> setList) {
        for (Set s : setList) {
            if (s.getSpec().equals(setSpec)) {
                return s;
            }
        }
        return null;
    }

    /**
     * Returns true if setList contains a set with specified setSpec.
     * 
     * @param setSpec
     *            identifier of the set
     * @param setList
     *            list of sets
     * @return true if the list contains the set
     */
    public static boolean contains(String setSpec, OAIDataList<Set> setList) {
        return get(setSpec, setList) != null;
    }

    protected MCRCondition buildSetCondition(String setSpec) {
        return MCROAIUtils.getDefaultSetCondition(setSpec, this.configPrefix);
    }

}
