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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.oai.classmapping.MCRClassificationAndSetMapper;
import org.mycore.oai.pmh.Description;
import org.mycore.oai.pmh.OAIConstants;
import org.mycore.oai.pmh.OAIDataList;
import org.mycore.oai.pmh.Set;
import org.mycore.oai.set.MCROAISetConfiguration;
import org.mycore.oai.set.MCROAISetHandler;
import org.mycore.oai.set.MCROAISolrSetConfiguration;
import org.mycore.oai.set.MCRSet;
import org.mycore.util.concurrent.MCRFixedUserCallable;

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

    protected final static Logger LOGGER = LogManager.getLogger(MCROAISetManager.class);

    protected String configPrefix;

    protected Map<String, MCROAISetConfiguration<?, ?, ?>> setConfigurationMap;

    /**
     * Time in milliseconds when the classification changed.
     */
    protected long classLastModified;

    /**
     * Time in minutes.
     */
    protected int cacheMaxAge;

    protected final OAIDataList<MCRSet> cachedSetList;

    public MCROAISetManager() {
        this.setConfigurationMap = Collections.synchronizedMap(new HashMap<>());
        this.cachedSetList = new OAIDataList<>();
        this.classLastModified = Long.MIN_VALUE;
    }

    public void init(String configPrefix, int cacheMaxAge) {
        this.configPrefix = configPrefix;
        this.cacheMaxAge = cacheMaxAge;
        updateURIs();
        if (this.cacheMaxAge != 0) {
            ScheduledExecutorService updateSetExecutor = Executors.newScheduledThreadPool(1);
            try {
                updateCachedSetList();
                updateSetExecutor.scheduleWithFixedDelay(getUpdateRunnable(), cacheMaxAge, cacheMaxAge,
                    TimeUnit.MINUTES);
            } finally {
                MCRShutdownHandler.getInstance().addCloseable(updateSetExecutor::shutdown);
            }
        }
    }

    private Runnable getUpdateRunnable() {
        MCRFixedUserCallable<Object> callable = new MCRFixedUserCallable<>(
            Executors.callable(this::updateCachedSetList), MCRSystemUserInformation.getSystemUserInstance());
        return () -> {
            try {
                callable.call();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new MCRException(e);
            }
        };
    }

    private void updateCachedSetList() {
        LOGGER.info("update oai set list");
        synchronized (cachedSetList) {
            OAIDataList<MCRSet> setList = createSetList();
            cachedSetList.clear();
            cachedSetList.addAll(setList);
        }
    }

    protected void updateURIs() {
        this.setConfigurationMap = Collections.synchronizedMap(new HashMap<>());
        getDefinedSetIds().stream()
            .map(String::trim)
            .map(setId -> new MCROAISolrSetConfiguration(this.configPrefix, setId))
            .forEach(c -> setConfigurationMap.put(c.getId(), c));
    }

    public List<String> getDefinedSetIds() {
        return MCRConfiguration.instance().getStrings(this.configPrefix + "Sets", Collections.emptyList());
    }

    /**
     * Returns a list of OAI-PMH sets defined by MyCoRe.
     *
     * @return list of oai sets
     */
    @SuppressWarnings("unchecked")
    public OAIDataList<MCRSet> get() {
        // no cache
        if (this.cacheMaxAge == 0) {
            return createSetList();
        }
        OAIDataList<MCRSet> oaiDataList = getDirectList();
        // create a shallow copy of the set list
        synchronized (oaiDataList) {
            return (OAIDataList<MCRSet>) oaiDataList.clone();
        }
    }

    public OAIDataList<MCRSet> getDirectList() {
        if (this.cacheMaxAge == 0) {
            return createSetList();
        }
        // cache
        // check if classification changed
        long lastModified = MCRCategoryDAOFactory.getInstance().getLastModified();
        if (lastModified != this.classLastModified) {
            this.classLastModified = lastModified;
            synchronized (this.cachedSetList) {
                OAIDataList<MCRSet> setList = createSetList();
                cachedSetList.clear();
                cachedSetList.addAll(setList);
            }
        }
        return cachedSetList;
    }

    /**
     * Returns the {@link MCROAISetConfiguration} for the given set id.
     *
     * @param <Q> value of the configuration
     * @param <R> Result collection type
     * @param <K> Key value type for a single hit
     * @param setId the set identifier
     * @return the configuration for this set
     */
    @SuppressWarnings("unchecked")
    public <Q, R, K> MCROAISetConfiguration<Q, R, K> getConfig(String setId) {
        return (MCROAISetConfiguration<Q, R, K>) this.setConfigurationMap.get(setId);
    }

    protected OAIDataList<MCRSet> createSetList() {
        OAIDataList<MCRSet> setList = new OAIDataList<>();
        synchronized (this.setConfigurationMap) {
            for (MCROAISetConfiguration<?, ?, ?> conf : this.setConfigurationMap.values()) {
                MCROAISetHandler<?, ?, ?> handler = conf.getHandler();
                Map<String, MCRSet> setMap = handler.getSetMap();
                synchronized (setMap) {
                    setMap.clear();
                    Element resolved = MCRURIResolver.instance().resolve(conf.getURI());
                    if (resolved == null) {
                        throw new MCRException(
                            "Could not resolve set URI " + conf.getURI() + " for set " + conf.getId() + ".");
                    }
                    for (Element setElement : resolved.getChildren("set", OAIConstants.NS_OAI)) {
                        MCRSet set = createSet(conf.getId(), setElement);
                        setMap.put(set.getSpec(), set);
                        if (!contains(set.getSpec(), setList)) {
                            if (!handler.filter(set)) {
                                setList.add(set);
                            }
                        }
                    }
                }
            }
        }
        return setList;
    }

    private MCRSet createSet(String setId, Element setElement) {
        String setSpec = setElement.getChildText("setSpec", NS_OAI);
        String setName = setElement.getChildText("setName", NS_OAI);
        MCRSet set = new MCRSet(setId, getSetSpec(setSpec), setName);
        set.getDescription()
            .addAll(setElement.getChildren("setDescription", NS_OAI)
                .stream() //all setDescription
                .flatMap(e -> e.getChildren().stream().limit(1)) //first childElement of setDescription
                .peek(Element::detach)
                .map(d -> (Description) new Description() {
                    @Override
                    public Element toXML() {
                        return d;
                    }

                    @Override
                    public void fromXML(Element descriptionElement) {
                        throw new UnsupportedOperationException();
                    }
                })
                .collect(Collectors.toList()));
        return set;
    }

    private String getSetSpec(String elementText) {
        if (elementText.contains(":")) {
            StringBuffer setSpec = new StringBuffer();
            String classID = elementText.substring(0, elementText.indexOf(':')).trim();
            classID = MCRClassificationAndSetMapper.mapClassificationToSet(this.configPrefix, classID);
            setSpec.append(classID).append(elementText.substring(elementText.indexOf(':')));
            return setSpec.toString();
        } else {
            return elementText;
        }
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
    public static <T extends Set> T get(String setSpec, OAIDataList<T> setList) {
        return setList.stream().filter(s -> s.getSpec().equals(setSpec)).findFirst().orElse(null);
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
    public static boolean contains(String setSpec, OAIDataList<? extends Set> setList) {
        return get(setSpec, setList) != null;
    }

}
