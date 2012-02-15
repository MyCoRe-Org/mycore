package org.mycore.services.acl;

import java.util.LinkedList;
import java.util.List;

import org.jdom.Element;
import org.mycore.backend.hibernate.tables.MCRACCESS;
import org.mycore.backend.hibernate.tables.MCRACCESSPK;
import org.mycore.backend.hibernate.tables.MCRACCESSRULE;

public abstract class MCRACLXMLProcessing {

    public static Element accessFilter2XML(String objid, String acpool) {
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

    public static Element access2XML(List<MCRACCESS> accessList, boolean withPos) {
        Element mcrAccessSet = new Element("mcr_access_set");

        if (accessList == null) {
            MCRACCESS emptyAccess = new MCRACCESS();
            MCRACCESSRULE emptyRule = new MCRACCESSRULE();
            emptyRule.setRid("");
            emptyAccess.setRule(emptyRule);
            emptyAccess.setKey(new MCRACCESSPK("", ""));
            accessList = new LinkedList<MCRACCESS>();
            accessList.add(emptyAccess);
        }

        for (int pos = 0; pos < accessList.size(); pos++) {
            MCRACCESS accessView = accessList.get(pos);

            Element mcrAccess = new Element("mcr_access");
            if (withPos)
                mcrAccess.setAttribute("pos", Integer.toString(pos));

            Element ACPOOL = new Element("ACPOOL");
            ACPOOL.addContent(accessView.getKey().getAcpool());

            Element OBJID = new Element("OBJID");
            OBJID.addContent(accessView.getKey().getObjid());

            Element RID = new Element("RID");
            RID.addContent(accessView.getRule().getRid());
            
            Element CREATOR = new Element("CREATOR");
            CREATOR.addContent(accessView.getCreator());

            mcrAccess.addContent(ACPOOL);
            mcrAccess.addContent(OBJID);
            mcrAccess.addContent(RID);
            mcrAccess.addContent(CREATOR);

            mcrAccessSet.addContent(mcrAccess);
        }
        return mcrAccessSet;
    }

    public static Element ruleSet2Items(List<MCRACCESSRULE> ruleList) {
        Element items = new Element("items");
        for (MCRACCESSRULE rule : ruleList) {
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

    public static Element ruleSet2XML(List<MCRACCESSRULE> ruleList, List<String> notEditableCreatorList) {
        Element mcrAccessRuleSet = new Element("mcr_access_rule_set");

        if (ruleList == null) {
            MCRACCESSRULE emptyRule = new MCRACCESSRULE();
            emptyRule.setRid("");
            emptyRule.setRule("");
            ruleList = new LinkedList<MCRACCESSRULE>();
            ruleList.add(emptyRule);
        }

        for (int pos = 0; pos < ruleList.size(); pos++) {
            MCRACCESSRULE rule = ruleList.get(pos);
            Element mcrAccessRule = new Element("mcr_access_rule");
            if(notEditableCreatorList.contains(rule.getCreator()))
                mcrAccessRule.setAttribute("editable", "false");
            mcrAccessRule.setAttribute("pos", Integer.toString(pos));

            mcrAccessRule.addContent(new Element("RuleStyle").addContent("plain"));

            String rid = rule.getRid();
            mcrAccessRule.addContent(new Element("RID").addContent(rid));
            mcrAccessRule.addContent(new Element("RULE").addContent(rule.getRule()));
            mcrAccessRule.addContent(new Element("CREATOR").addContent(rule.getCreator()));

            String descr = rule.getDescription();

            if (descr != null)
                mcrAccessRule.addContent(new Element("DESCRIPTION").addContent(descr));
            else
                mcrAccessRule.addContent(new Element("DESCRIPTION").addContent(""));

            if (MCRACLHIBAccess.isRuleInUse(rid))
                mcrAccessRule.addContent(new Element("inUse").addContent("true"));
            else
                mcrAccessRule.addContent(new Element("inUse").addContent("false"));

            mcrAccessRuleSet.addContent(mcrAccessRule);
        }

        return mcrAccessRuleSet;
    }

}
