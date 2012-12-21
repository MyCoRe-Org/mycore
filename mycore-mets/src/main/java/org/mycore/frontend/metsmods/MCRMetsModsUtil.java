/*
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

package org.mycore.frontend.metsmods;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.*;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConstants;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * @author Stefan Freitag
 * @version $Revision: 1.6 $ $Date: 2009/01/19 10:06:12 $
 */

public class MCRMetsModsUtil {

    private static Logger LOGGER = Logger.getLogger(MCRMetsModsUtil.class.getName());

    public Element createNewMetsFile(String derivate_id) {
        Element root = new Element("mets", MCRConstants.METS_NAMESPACE);
        root.addNamespaceDeclaration(MCRConstants.XSI_NAMESPACE);
        root.addNamespaceDeclaration(MCRConstants.XLINK_NAMESPACE);
        root.setAttribute("noNamespaceSchemaLocation", "http://www.loc.gov/METS/ http://www.loc.gov/mets/mets.xsd", MCRConstants.XSI_NAMESPACE);
        root.addContent(init_fileSec(derivate_id));
        root.addContent(init_structMap(derivate_id));
        return root;
    }

    public Element createMetsElement(ArrayList<String> list, Element mets, String default_url, String ContentIDS) {
        Iterator<String> iter = list.iterator();
        int i = 1;
        while (iter.hasNext()) {
            String pic = iter.next();
            String fn = pic.substring(pic.lastIndexOf("/") + 1, pic.lastIndexOf("."));
            pic = default_url + "/" + pic;

            add_file(mets, fn, pic, i, ContentIDS, "page");
            i++;
        }

        return mets;
    }

    /**
     * Gets a list containing picture names including directories.
     * 
     * @param list
     *            of pictures
     * @return JDOM element with METS
     */
    public Element createMetsElement(ArrayList<String> list, Element mets, String default_url) {
        Iterator<String> iter = list.iterator();
        int i = 1;
        while (iter.hasNext()) {
            String pic = iter.next();
            String fn = pic.substring(pic.lastIndexOf("/") + 1, pic.lastIndexOf("."));
            pic = default_url + "/" + pic;

            add_file(mets, fn, pic, i, "page");
            i++;
        }

        return mets;
    }

    public Element init_mods(String derivate_id, String title, String display, String place, String date) {
        Element dmdSec = new Element("dmdSec", MCRConstants.METS_NAMESPACE);
        dmdSec.setAttribute("ID", "dmd_" + derivate_id);

        Element mdWrap = new Element("mdWrap", MCRConstants.METS_NAMESPACE);
        mdWrap.setAttribute("MIMETYPE", "text/xml");
        mdWrap.setAttribute("MDTYPE", "MODS");

        Element xmlData = new Element("xmlData", MCRConstants.METS_NAMESPACE);
        Element mods = new Element("mods", MCRConstants.MODS_NAMESPACE);

        Element titleInfo = new Element("titleInfo", MCRConstants.MODS_NAMESPACE);
        titleInfo.addContent(new Element("title", MCRConstants.MODS_NAMESPACE).setText(title));

        Element name = new Element("name", MCRConstants.MODS_NAMESPACE);
        name.setAttribute("type", "personal");
        name.addContent(new Element("displayForm", MCRConstants.MODS_NAMESPACE).setText(display));

        Element originInfo = new Element("originInfo", MCRConstants.MODS_NAMESPACE);
        originInfo.addContent(new Element("place", MCRConstants.MODS_NAMESPACE).addContent(new Element("placeTerm", MCRConstants.MODS_NAMESPACE).setAttribute(
                "type", "text").setText(place)));
        originInfo.addContent(new Element("dateIssued", MCRConstants.MODS_NAMESPACE).setAttribute("keyDate", "yes").setAttribute("encoding", "w3cdtf").setText(
                date));

        mods.addContent(titleInfo);
        mods.addContent(name);
        mods.addContent(originInfo);

        xmlData.addContent(mods);
        mdWrap.addContent(xmlData);
        dmdSec.addContent(mdWrap);

        return dmdSec;
    }

    public Element init_amdSec(String derivate_id, String owner, String ownerLogo, String ownerSiteURL, String reference, String presentation) {
        Element amdSec = new Element("amdSec", MCRConstants.METS_NAMESPACE);
        amdSec.setAttribute("ID", "amd_" + derivate_id);
        amdSec.addContent(init_rights(derivate_id, owner, ownerLogo, ownerSiteURL));
        amdSec.addContent(init_digiprov(derivate_id, reference, presentation));
        return amdSec;
    }

    public Element init_rights(String derivate_id, String owner, String ownerLogo, String ownerSiteURL) {
        Element rightsMD = new Element("rightsMD", MCRConstants.METS_NAMESPACE);
        rightsMD.setAttribute("ID", "rights_" + derivate_id);

        Element mdWrap = new Element("mdWrap", MCRConstants.METS_NAMESPACE);
        mdWrap.setAttribute("MIMETYPE", "text/xml");
        mdWrap.setAttribute("MDTYPE", "OTHER");
        mdWrap.setAttribute("OTHERMDTYPE", "DVRIGHTS");

        Element xmlData = new Element("xmlData", MCRConstants.METS_NAMESPACE);
        Element rights = new Element("rights", MCRConstants.DV_NAMESPACE);
        rights.addContent(new Element("owner", MCRConstants.DV_NAMESPACE).setText(owner));
        rights.addContent(new Element("ownerLogo", MCRConstants.DV_NAMESPACE).setText(ownerLogo));
        rights.addContent(new Element("ownerSiteURL", MCRConstants.DV_NAMESPACE).setText(ownerSiteURL));

        xmlData.addContent(rights);
        mdWrap.addContent(xmlData);
        rightsMD.addContent(mdWrap);

        return rightsMD;
    }

    public Element init_digiprov(String derivate_id, String reference, String presentation) {
        Element digiprovMD = new Element("digiprovMD", MCRConstants.METS_NAMESPACE);
        digiprovMD.setAttribute("ID", "digiprov_" + derivate_id);

        Element mdWrap = new Element("mdWrap", MCRConstants.METS_NAMESPACE);
        mdWrap.setAttribute("MIMETYPE", "text/html");
        mdWrap.setAttribute("MDTYPE", "OTHER");
        mdWrap.setAttribute("OTHERMDTYPE", "DVLINKS");

        Element xmlData = new Element("xmlData", MCRConstants.METS_NAMESPACE);
        Element dvlinks = new Element("links", MCRConstants.DV_NAMESPACE);
        dvlinks.addContent(new Element("reference", MCRConstants.DV_NAMESPACE).setText(reference));
        dvlinks.addContent(new Element("presentation", MCRConstants.DV_NAMESPACE).setText(presentation));

        xmlData.addContent(dvlinks);
        mdWrap.addContent(xmlData);
        digiprovMD.addContent(mdWrap);

        return digiprovMD;
    }

    private Element init_fileSec(String derivate_id) {
        Element fileSec = new Element("fileSec", MCRConstants.METS_NAMESPACE);
        fileSec.setAttribute("ID", "fileSec_" + derivate_id);
        Element fileGrp = new Element("fileGrp", MCRConstants.METS_NAMESPACE);
        fileGrp.setAttribute("USE", "DEFAULT");
        fileSec.addContent(fileGrp);
        return fileSec;
    }

    private Element init_structMap(String derivate_id) {
        Element structMap = new Element("structMap", MCRConstants.METS_NAMESPACE);
        structMap.setAttribute("TYPE", "PHYSICAL");
        Element div = new Element("div", MCRConstants.METS_NAMESPACE);
        div.setAttribute("ID", "phys_" + derivate_id);
        div.setAttribute("DMDID", "dmd_" + derivate_id);
        div.setAttribute("ADMID", "amd_" + derivate_id);
        div.setAttribute("TYPE", "physSequence");
        structMap.addContent(div);
        return structMap;
    }

    public Element add_file(Element mets, String file_id, String url, int order, String contentids, String type) {
        return add_file_ext(mets, file_id, url, order, contentids, type);
    }

    public Element add_file(Element mets, String file_id, String url, int order, String type) {
        return add_file_ext(mets, file_id, url, order, null, type);
    }

    public Element add_file_ext(Element mets, String file_id, String url, int order, String contentids, String type) {
        Element fileGrp = mets.getChild("fileSec", MCRConstants.METS_NAMESPACE).getChild("fileGrp", MCRConstants.METS_NAMESPACE);
        Element div = mets.getChild("structMap", MCRConstants.METS_NAMESPACE).getChild("div", MCRConstants.METS_NAMESPACE);

        // <fileSec>
        Element file = new Element("file", MCRConstants.METS_NAMESPACE);
        file.setAttribute("MIMETYPE", "image/jpeg");
        file.setAttribute("ID", file_id + "_default");
        Element flocat = new Element("FLocat", MCRConstants.METS_NAMESPACE);
        flocat.setAttribute("LOCTYPE", "URL");
        flocat.setAttribute("href", url, MCRConstants.XLINK_NAMESPACE);
        file.addContent(flocat);
        fileGrp.addContent(file);
        // </fileSec>

        // <structMap>
        Element div_ = new Element("div", MCRConstants.METS_NAMESPACE);
        div_.setAttribute("ID", file_id);
        div_.setAttribute("ORDER", String.valueOf(order));
        // The following line has changed in order to set the filename as
        // orderlabel...
        div_.setAttribute("ORDERLABEL", file_id);// String.valueOf(order));
        div_.setAttribute("type", type);
        // The following line contains the CONTENTIDS, if is null then do
        // nothing
        if (contentids != null)
            div_.setAttribute("CONTENTIDS", contentids);
        Element fptr = new Element("fptr", MCRConstants.METS_NAMESPACE);
        fptr.setAttribute("FILEID", file_id + "_default");
        div_.addContent(fptr);
        div.addContent(div_);
        // </structMap>

        return mets;
    }

    public static ArrayList<MCRMetsModsPicture> getFileList(Document metsfile) {
        ArrayList<MCRMetsModsPicture> piclist = new ArrayList<MCRMetsModsPicture>();
        Element root = metsfile.getRootElement();
        Element structMap = root.getChild("structMap", MCRConstants.METS_NAMESPACE);
        Element div = structMap.getChild("div", MCRConstants.METS_NAMESPACE);
        List elements = div.getChildren("div", MCRConstants.METS_NAMESPACE);

        for (Object element1 : elements) piclist.add(new MCRMetsModsPicture("dummy", 0, "0"));

        for (Object element : elements) {
            Element e = (Element) element;
            String fileid = e.getAttributeValue("ID");
            String order = e.getAttributeValue("ORDER");
            String orderlabel = e.getAttributeValue("ORDERLABEL");

            piclist.set(Integer.parseInt(order) - 1, new MCRMetsModsPicture(fileid, Integer.parseInt(order), orderlabel));

        }

        return piclist;
    }

    public static String getMetsFile(ArrayList<MCRMetsModsPicture> piclist, Document metsfile) {
        Element root = metsfile.getRootElement();
        Element structMap = root.getChild("structMap", MCRConstants.METS_NAMESPACE);
        Element div = structMap.getChild("div", MCRConstants.METS_NAMESPACE);
        List elements = div.getChildren("div", MCRConstants.METS_NAMESPACE);
        for (Object element : elements) {
            Element e = (Element) element;
            String fileid = e.getAttributeValue("ID");

            for (int j = 0; j < piclist.size(); j++) {
                MCRMetsModsPicture mmp = piclist.get(j);
                if (mmp.getPicture().compareTo(fileid) == 0) {

                    // System.out.println("-fileid: "+fileid+" -j: "+j+" -mmp.getPicture: "+mmp.getPicture()+" -mmp.getOrder: "+mmp.getOrder()+" -mmp.getOrderlabel: "+mmp.getOrderlabel());

                    e.setAttribute("ORDER", String.valueOf(mmp.getOrder()));
                    e.removeAttribute("ORDERLABEL");
                    e.setAttribute("ORDERLABEL", mmp.getOrderlabel());
                    piclist.remove(j); // to get a little bit more speed
                }
            }
        }
        String st = "";
        try {
            Format form = Format.getCompactFormat();
            form.setEncoding("UTF-8");
            form.setOmitEncoding(false);
            XMLOutputter xmlout = new XMLOutputter(form);
            st = xmlout.outputString(metsfile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return st;
    }

}
