package org.mycore.services.acl;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.filter.Filter;
import org.mycore.access.MCRAccessInterface;
import org.mycore.access.mcrimpl.MCRAccessControlSystem;
import org.mycore.access.mcrimpl.MCRAccessRule;
import org.mycore.access.mcrimpl.MCRRuleMapping;
import org.mycore.access.mcrimpl.MCRRuleStore;
import org.mycore.backend.hibernate.tables.MCRACCESS;
import org.mycore.backend.hibernate.tables.MCRACCESSRULE;

public class MCRACLXMLProcessing {
    private static Logger LOGGER = Logger.getLogger(MCRACLXMLProcessing.class);

    public Element accessFilter2XML(String objid, String acpool) {
        Element filter = new Element("mcr_access_filter");
        Element objidFilter = new Element("objid");
        Element acpoolFilter = new Element("acpool");

        if (objid == null)
            objid = "";

        if (acpool == null)
            acpool = "";

        objidFilter.addContent(objid);
        filter.addContent(objidFilter);

        acpoolFilter.addContent(acpool);
        filter.addContent(acpoolFilter);

        return filter;
    }

    public Element access2XML(List accessList) {
        Element mcr_access_set = new Element("mcr_access_set");

        int i = 0;
        for (Iterator it = accessList.iterator(); it.hasNext();) {
            MCRACCESS accessView = (MCRACCESS) it.next();

            /*Element mcr_access = new Element("mcr_access");
            mcr_access.setAttribute("pos", Integer.toString(i));

            Element ACPOOL = new Element("ACPOOL");
            ACPOOL.addContent(accessView.getKey().getAcpool());

            Element OBJID = new Element("OBJID");
            OBJID.addContent(accessView.getKey().getObjid());

            Element RID = new Element("RID");
            RID.addContent(accessView.getRid());

            mcr_access.addContent(ACPOOL);
            mcr_access.addContent(OBJID);
            mcr_access.addContent(RID);

            mcr_access_set.addContent(mcr_access);*/
            
            //------------------------------------------------
            
            String acpool = accessView.getKey().getAcpool();
            String objid = accessView.getKey().getObjid();
            String rid = accessView.getRid();
            
            mcr_access_set.addContent(accessElem(i, acpool, objid, rid));
            
            i++;
        }
        return mcr_access_set;
    }
    
    public Element accessElem(int pos, String acpool, String objid, String rid){
        Element mcr_access = new Element("mcr_access");
        if (pos > 0)
            mcr_access.setAttribute("pos", Integer.toString(pos));

        Element ACPOOL = new Element("ACPOOL");
        ACPOOL.addContent(acpool);

        Element OBJID = new Element("OBJID");
        OBJID.addContent(objid);

        Element RID = new Element("RID");
        RID.addContent(rid);

        mcr_access.addContent(ACPOOL);
        mcr_access.addContent(OBJID);
        mcr_access.addContent(RID);
        return mcr_access;
    }

    public Element ruleSet2Items(List ruleList) {
        Element items = new Element("items");

        for (Iterator it = ruleList.iterator(); it.hasNext();) {
            MCRACCESSRULE rule = (MCRACCESSRULE) it.next();

            Element item = new Element("item");
            String rid = rule.getRid();
            item.setAttribute("value", rid);

            String descr = rule.getDescription();

            if (descr != null)
                item.setAttribute("label", descr + " ( " + rid + ")");
            else
                item.setAttribute("label", rid);

            items.addContent(item);
        }

        return items;
    }

    public Element ruleSet2XML(List ruleList) {
        Element mcrAccessRuleSet = new Element("mcr_access_rule_set");
        
        if (ruleList == null){
            MCRACCESSRULE emptyRule = new MCRACCESSRULE();
            emptyRule.setRid("");
            emptyRule.setRule("");
            ruleList = new LinkedList();
            ruleList.add(emptyRule);
        }

        for (Iterator it = ruleList.iterator(); it.hasNext();) {
            MCRACCESSRULE rule = (MCRACCESSRULE) it.next();

            Element mcrAccessRule = new Element("mcr_access_rule");
            mcrAccessRule.addContent(new Element("RID").addContent(rule.getRid()));
            mcrAccessRule.addContent(new Element("RULE").addContent(rule.getRule()));

            String descr = rule.getDescription();

            if (descr != null)
                mcrAccessRule.addContent(new Element("DESCRIPTION").addContent(descr));
            else
                mcrAccessRule.addContent(new Element("DESCRIPTION").addContent(""));

            mcrAccessRuleSet.addContent(mcrAccessRule);
        }

        return mcrAccessRuleSet;
    }

    public Map findRulesDiff(Document editedRules, Document origRules) throws Exception {
        Element editedRulesRoot = editedRules.getRootElement();
        Map diffMap = new HashMap();
        
        List updateList = new LinkedList();
        List saveList = new LinkedList();
        List deleteList = new LinkedList();
        
        List matches = new LinkedList();
        
        String rule = "";
        String description = "";
        
        Filter newRulesFilter = new Filter() {
            public boolean matches(Object arg0) {
                if (((Element) arg0).getAttributeValue("pos") == null) {
                    return true;
                } else
                    return false;
            }
        };
        
        matches = editedRulesRoot.removeContent(newRulesFilter);
        
        // saving new rules
        MCRRuleStore ruleStore = MCRRuleStore.getInstance();
        int rid = ruleStore.getNextFreeRuleID(MCRAccessControlSystem.systemRulePrefix);
        int i = 0;
        Iterator iter = matches.iterator();
        while (iter.hasNext()){
            Element currentRule = (Element) iter.next();
            
            rule = currentRule.getChildText("RULE");
            description = currentRule.getChildText("DESCRIPTION");
            
            saveList.add(new MCRAccessRule(Integer.toString(rid + i), "", new Date(), rule, description));
            i++;
        }
        
        diffMap.put("update", updateList);
        diffMap.put("save", saveList);
        diffMap.put("delete", deleteList);
        
        return diffMap;
    }
    
    public Map findAccessDiff(Document editedAcces, Document origAccess) throws Exception {
        Element editedAccessRoot = editedAcces.getRootElement();
        Map diffMap = new HashMap();

        LOGGER.info("******* Filter not changed!!");

        List editedElemList = editedAccessRoot.getChildren();
        Element origAccessRoot = origAccess.getRootElement();
        
        List updateList = new LinkedList();
        List saveList = new LinkedList();
        List deleteList = new LinkedList();

        for (Iterator iter = editedElemList.iterator(); iter.hasNext();) {
            Element currentElem = (Element) iter.next();
            final String position = currentElem.getAttributeValue("pos");

            if (position == null) {
                String currentACPool = currentElem.getChildText("ACPOOL");
                String currentObjID = currentElem.getChildText("OBJID");
                String currentRID = currentElem.getChildText("RID");

                saveList.add(createRuleMapping(currentRID, currentACPool, currentObjID));
            } else {
                Filter posFilter = new Filter() {
                    public boolean matches(Object arg0) {
                        if (((Element) arg0).getAttributeValue("pos").equals(position)) {
                            return true;
                        } else
                            return false;
                    }
                };

                List matchingList = origAccessRoot.removeContent(posFilter);
                if (matchingList.size() > 1)
                    throw new Exception("Position number in access XML not unique!!");

                Element origElem = (Element) matchingList.get(0);
                String currentACPool = currentElem.getChildText("ACPOOL");
                String currentObjID = currentElem.getChildText("OBJID");
                String currentRID = currentElem.getChildText("RID");

                String origACPool = origElem.getChildText("ACPOOL");
                String origObjID = origElem.getChildText("OBJID");
                String origRID = origElem.getChildText("RID");

                if (!currentACPool.equals(origACPool) || !currentObjID.equals(origObjID)) {
                    saveList.add(createRuleMapping(currentRID, currentACPool, currentObjID));
                    deleteList.add(createRuleMapping(origRID, origACPool, origObjID));
                } else if (!currentRID.equals(origRID)) {
                    updateList.add(createRuleMapping(currentRID, currentACPool, currentObjID));
                }
            }
        }

        for (Iterator iter = origAccessRoot.getChildren().iterator(); iter.hasNext();) {
            Element origElem = (Element) iter.next();

            String origACPool = origElem.getChildText("ACPOOL");
            String origObjID = origElem.getChildText("OBJID");
            String origRID = origElem.getChildText("RID");

            deleteList.add(createRuleMapping(origRID, origACPool, origObjID));

        }

        diffMap.put("update", updateList);
        diffMap.put("save", saveList);
        diffMap.put("delete", deleteList);

        return diffMap;
    }

    public Element findDiffAsXML(Document editedAcces, Document origAccess) throws Exception {
        Map diffMap = findAccessDiff(editedAcces, origAccess);

        Element diff = new Element("Diff_list");

        Element update = makeElem((List) diffMap.get("update"), "update");
        Element save = makeElem((List) diffMap.get("save"), "save");
        Element delete = makeElem((List) diffMap.get("delete"), "delete");

        diff.addContent(update);
        diff.addContent(save);
        diff.addContent(delete);

        return diff;
    }

    private Element makeElem(List list, String rootName) {
        Element root = new Element(rootName);
        Element mcr_access_set = new Element("mcr_access_set");
        int i = 0;

        for (Iterator it = list.iterator(); it.hasNext();) {
            MCRRuleMapping ruleMapping = (MCRRuleMapping) it.next();

            Element mcr_access = new Element("mcr_access");
            mcr_access.setAttribute("pos", Integer.toString(i));

            Element ACPOOL = new Element("ACPOOL");
            ACPOOL.addContent(ruleMapping.getPool());

            Element OBJID = new Element("OBJID");
            OBJID.addContent(ruleMapping.getObjId());

            Element RID = new Element("RID");
            RID.addContent(ruleMapping.getRuleId());

            mcr_access.addContent(ACPOOL);
            mcr_access.addContent(OBJID);
            mcr_access.addContent(RID);

            mcr_access_set.addContent(mcr_access);
            i++;
        }

        return root.addContent(mcr_access_set);

    }

    private MCRRuleMapping createRuleMapping(String rid, String acpool, String objid) {
        MCRRuleMapping ruleMapping = new MCRRuleMapping();

        ruleMapping.setCreator("ACL-Editor");
        ruleMapping.setCreationdate(new Date());
        ruleMapping.setPool(acpool);
        ruleMapping.setRuleId(rid);
        ruleMapping.setObjId(objid);
        return ruleMapping;
    }
}
