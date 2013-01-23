package org.mycore.datamodel.classifications;

import java.util.List;

import org.jdom2.Element;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;

interface RowCreator {

    public abstract void createRows(String lang, Element xNavtree);

    public void setClassification(MCRCategoryID classifID);

    public void update(String categID) throws Exception;

    public abstract MCRCategory getClassification();

    public void setCommented(boolean commented);

    public void setBrowserClass(String browserClass);

    public void setUri(String uri);

    public void setView(String view);

}