/*
 * $RCSfile$
 * $Revision$ $Date$
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

package org.mycore.datamodel.classifications;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.xpath.XPath;
import org.mycore.access.MCRAccessInterface;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRConfiguration;

/**
 * Instances of MCRClassificationBrowser contain the data of the currently
 * displayed navigation tree. MCRClassificationBrowser uses one
 * MCRClassificationBrowserData instance per browser session to store and update
 * the category lines to be displayed.
 * 
 * @author Anja Schaar
 * 
 */
public class MCRClassificationBrowserData {
  protected boolean showComments;

  protected String pageName;

  protected String xslStyle;

  protected String uri;

  private static MCRConfiguration config;

  private static Logger LOGGER = Logger.getLogger(MCRClassificationBrowserData.class);

  private static MCRAccessInterface AI = MCRAccessManager.getAccessImpl();

  //private Vector lines;
  private ArrayList lines;

  private MCRClassificationItem classif;

  private String startPath = "";

  private String actItemID = "";

  private String lastItemID = "";

  private String[] categFields;

  private String emptyLeafs = null;

  private String view = null;

  private String comments = null;

  private String searchField = "";

  private String sort = null;

  private String doctype = null;

  private String[] doctypeArray = null;

  private String restriction = null;

  int maxlevel = 0;

  int totalNumOfDocs = 0;
  
  

  public MCRClassificationBrowserData(String u, String mode, String actclid, String actEditorCategid ) throws Exception {
    uri = u;
    config = MCRConfiguration.instance();
    LOGGER.info(" incomming Path " + uri);

    String[] uriParts = uri.split("/"); // mySplit();
    LOGGER.info(" Start");
    String classifID = "";

    if (uriParts.length <= 1) {
      LOGGER.debug(this.getClass() + " PathParts - classification is default");
      pageName = config.getString("MCR.ClassificationBrowser.default.EmbeddingPage");
      xslStyle = config.getString("MCR.ClassificationBrowser.default.Style");
      emptyLeafs = config.getString("MCR.ClassificationBrowser.default.EmptyLeafs");
      view = config.getString("MCR.ClassificationBrowser.default.View");
      doctype = config.getString("MCR.ClassificationBrowser.default.Doctype");
      comments = config.getString("MCR.ClassificationBrowser.default.Comments");
      searchField = config.getString("MCR.ClassificationBrowser.default.searchField");
      classifID = actclid;
      startPath = "default";
    } else {
      LOGGER.debug(" PathParts - classification " + uriParts[1]);
      LOGGER.debug(" Number of PathParts =" + uriParts.length);
      try {
        classifID = config.getString("MCR.ClassificationBrowser." + uriParts[1] + ".Classification");
      } catch (org.mycore.common.MCRConfigurationException noClass) {
        classifID = actclid;
      }
      try {
        pageName = config.getString("MCR.ClassificationBrowser." + uriParts[1] + ".EmbeddingPage");
      } catch (org.mycore.common.MCRConfigurationException noPagename) {
        pageName = config.getString("MCR.ClassificationBrowser.default.EmbeddingPage");
      }
      try {
        xslStyle = config.getString("MCR.ClassificationBrowser." + uriParts[1] + ".Style");
      } catch (org.mycore.common.MCRConfigurationException noStyle) {
        xslStyle = config.getString("MCR.ClassificationBrowser.default.Style");
      }
      try {
        searchField = config.getString("MCR.ClassificationBrowser." + uriParts[1] + ".searchField");
      } catch (org.mycore.common.MCRConfigurationException noSearchfield) {
        searchField = config.getString("MCR.ClassificationBrowser.default.searchField");
      }

      try {
        emptyLeafs = config.getString("MCR.ClassificationBrowser." + uriParts[1] + ".EmptyLeafs");
      } catch (org.mycore.common.MCRConfigurationException noEmptyLeafs) {
        emptyLeafs = config.getString("MCR.ClassificationBrowser.default.EmptyLeafs");
      }
      try {
        view = config.getString("MCR.ClassificationBrowser." + uriParts[1] + ".View");
      } catch (org.mycore.common.MCRConfigurationException noView) {
        view = config.getString("MCR.ClassificationBrowser.default.View");
      }
      try {
        doctype = config.getString("MCR.ClassificationBrowser." + uriParts[1] + ".Doctype");
      } catch (org.mycore.common.MCRConfigurationException noDoctype) {
        doctype = config.getString("MCR.ClassificationBrowser.default.Doctype");
      }
      try {
        sort = config.getString("MCR.ClassificationBrowser." + uriParts[1] + ".Sort");
        comments = config.getString("MCR.ClassificationBrowser." + uriParts[1] + ".Comments");
        restriction = config.getString("MCR.ClassificationBrowser." + uriParts[1] + ".Restriction");
      } catch (org.mycore.common.MCRConfigurationException ig) {
        // ignore for this parameters, the are optionally
        ;
      }
      startPath = uriParts[1];
    }

    if ("edit".equals(mode)) {
      pageName = config.getString("MCR.classeditor.EmbeddingPage");
      xslStyle = config.getString("MCR.classeditor.Style");
      sort = "false";
      view = "tree";

      if (classifID.length() == 0) {
        return;
      }
    }

    if (emptyLeafs == null)
      emptyLeafs = "yes";
    if (view == null || !view.endsWith("flat"))
      view = "tree";
    if (comments == null)
      comments = "false";

    clearPath(uriParts);
    setClassification(classifID);
    setActualPath(actEditorCategid);    
    
    if (doctype != null) {
      try {
        String typelist = config.getString("MCR.type_" + doctype);
        if (typelist.equals("true")) {
            doctypeArray = new String[1];
            doctypeArray[0] = doctype;           
        } else {
        doctypeArray = typelist.split(","); }
      } catch (Exception allignore) {
        doctypeArray = new String[1];
        doctypeArray[0] = "document";
        LOGGER.warn("No search type was found - document was set");
      }
    }
    showComments = comments.endsWith("true") ? true : false;

    LOGGER.debug(" SetClassification " + classifID);
    LOGGER.debug(" Empty nodes: " + emptyLeafs);
    LOGGER.debug(" View: " + view);
    LOGGER.debug(" Comment: " + comments);
    LOGGER.debug(" Doctypes: " + doctype);
    for (int i = 0; i < doctypeArray.length; i++)
      LOGGER.debug(" Type: " + doctypeArray[i]);
    LOGGER.debug(" Restriction: " + restriction);
    LOGGER.debug(" Sort: " + sort);
  }


  public String getUri() {
	    return uri;
  }

  /**
   * Returns true if category comments for the classification currently
   * displayed should be shown.
   */
  public boolean showComments() {
    return showComments;
  }

  /**
   * Returns the pageName for the classification
   */
  public String getPageName() {
    return pageName;
  }

  /**
   * Returns the xslStyle for the classification
   */
  public String getXslStyle() {
    return xslStyle;
  }

  public MCRClassificationItem getClassification() {
    return classif;
  }

  public org.jdom.Document loadTreeIntoSite(org.jdom.Document cover, org.jdom.Document browser) {
	    Element placeholder = cover.getRootElement().getChild("classificationBrowser");
	    LOGGER.info(" Found Entry at " + placeholder);
	    if (placeholder != null) {
	      List children = browser.getRootElement().getChildren();
	      for (int j = 0; j < children.size(); j++) {
	        Element child = (Element) ((Element) (children.get(j))).clone();
	        placeholder.addContent(child);
	      }
	    }
	    LOGGER.debug(cover);
	    return cover;
  }

  
  
  private final void setClassification(String classifID) throws Exception {
	classif = MCRClassificationItem.getClassificationItem(classifID);
    lines = new ArrayList();      
    totalNumOfDocs = 0;
    putCategoriesintoLines(-1, classif.getChildrenFromJDomAsList(), 1);
    LOGGER.debug("Arraylist of CategItems initialized - Size " +  lines.size());
  }

  private void clearPath(String[] uriParts) throws Exception {
    String[] cati = new String[uriParts.length];
    String path = "/" + uriParts[1];
    int len = 0;
    // pfad bereinigen
    for (int k = 2; k < uriParts.length; k++) {
      LOGGER.debug(" uriParts[k]=" + uriParts[k] + " k=" + k);
      if (uriParts[k].length() > 0) {
        if (uriParts[k].equalsIgnoreCase("..") && len > 0) {
          len--;
        } else {
          cati[len] = uriParts[k];
          len++;
        }
      }
    }
    
    //remove double entries from path 
    //(if an entry appears the 2nd time it will not be displayed -> so we can remove it here)
    TreeSet result = new TreeSet();
	for(int i=0;i<len;i++){
		  Object x = cati[i];
		  if(result.contains(x)) 
			  	result.remove(x);
		  else  result.add(x);		  
	 } 
	  
	 //reinitialisieren
	 categFields=new String[result.size()];
	 int j=0;
	 Iterator it = result.iterator();
	 while(it.hasNext()){
		  String s = (String)it.next();
	  	  categFields[j]=s;
	  	  j++;
	  	  path +="/"+s;
	 }   	  
	 this.uri = path;
  }

  
  private void setActualPath(String actEditorCategid) throws Exception {
    actItemID = lastItemID = "";
    for (int k = 0; k < categFields.length; k++) {
      update(categFields[k]);
      lastItemID = actItemID;
      actItemID = categFields[k];
    }
    if (actEditorCategid != null) {
      actItemID = lastItemID = actEditorCategid;
    }
  }

  private Element setTreeline(Element cat, int level){
	  cat.setAttribute("level",String.valueOf(level));
	  if ( cat.getChildren("category").size() != 0  ) {
		  cat.setAttribute("hasChildren","T");		  
	  } else {
		  cat.setAttribute("hasChildren"," ");
	  }
	  return cat;
  }
  
  private Element getTreeline (int i) {
     if (i >= lines.size()) {
        return null;
      }
     return (Element) (lines.get(i));	  
  }
  
  private void putCategoriesintoLines(int startpos, List children, int level) {
        LOGGER.debug("Start Explore Arraylist of CategItems  ");	  
	    int i = startpos;
		for (int j = 0, k = children.size(); j < k; j++) {
			Element child = (Element) children.get(j);
			lines.add( ++i, setTreeline(child, level + 1));              
	        if ( startpos == -1 ) {
	     	   if ( child.getAttributeValue("counter")!= null )
	    		   totalNumOfDocs += Integer.parseInt( child.getAttributeValue("counter"));
	        }
		}
	    LOGGER.debug("End Explore - Arraylist of CategItems ");
  }
  

  public org.jdom.Document createXmlTreeforAllClassifications() throws Exception {
    MCRClassificationManager clm = new MCRClassificationManager();
    MCRClassificationItem[] clI = clm.getAllClassification();

    Element xDocument = new Element("classificationbrowse");
    Element EditClassbutton = new Element("userCanEdit");
    if (AI.checkPermission("create-classification")) {
      EditClassbutton.addContent("true");
    } else {
      EditClassbutton.addContent("false");
    }

    xDocument.addContent(EditClassbutton);

    Element xNavtree = new Element("classificationlist");
    xDocument.addContent(xNavtree);
    String browserClass = "";
    String Counter = "";

    for (int i = 0; i < clI.length && clI[i] != null; i++) {
      Element cli = clI[i].getClassificationItemAsJDom();
      try {
        browserClass = config.getString("MCR.classeditor." + clI[i].getClassificationID());
      } catch (Exception ignore) {
        browserClass = "default";
      }
      cli.setAttribute("browserClass", browserClass);
      try {
        doctype = config.getString("MCR.ClassificationBrowser." + browserClass + ".Doctype");
      } catch (org.mycore.common.MCRConfigurationException noDoctype) {
        doctype = config.getString("MCR.ClassificationBrowser.default.Doctype");
      }
      if (doctype != null) {
        try {
          String typelist = config.getString("MCR.type_" + doctype);
          doctypeArray = typelist.split(",");
        } catch (Exception allignore) {
          doctypeArray = new String[1];
          doctypeArray[0] = "document";
          LOGGER.warn("No search type was found - document was set");
        }
      }
      try {
        Counter = Integer.toString(clI[i].countDocLinks(doctypeArray, ""));
      } catch (Exception ignore) {
        Counter = "NaN";
      }
      cli.setAttribute("counter", Counter);
      xNavtree.addContent(cli);
    }

    return new Document(xDocument);
  }

  /**
   * Creates an XML representation of MCRClassificationBrowserData
   * 
   * @author Anja Schaar
   * 
   */

  public org.jdom.Document createXmlTree(String lang) throws Exception {

    MCRClassificationItem cl = getClassification();
    Element xDocument = new Element("classificationBrowse");
    LOGGER.info(cl.getClassificationID());

    Element xID = new Element("classifID");
    xID.addContent(cl.getClassificationID());
    xDocument.addContent(xID);

    Element xLabel = new Element("label");
    xLabel.addContent(cl.getText(lang));
    xDocument.addContent(xLabel);

    Element xDesc = new Element("description");
    xDesc.addContent(cl.getDescription(lang));
    xDocument.addContent(xDesc);

    Element xDocuments = new Element("cntDocuments");
    xDocuments.addContent(String.valueOf(totalNumOfDocs)); 
    xDocument.addContent(xDocuments);

    Element xShowComments = new Element("showComments");
    xShowComments.addContent(String.valueOf(showComments()));
    xDocument.addContent(xShowComments);

    Element xUri = new Element("uri");
    xUri.addContent(uri);
    xDocument.addContent(xUri);

    Element xStartPath = new Element("startPath");
    xStartPath.addContent(startPath);
    xDocument.addContent(xStartPath);

    Element xSearchField = new Element("searchField");
    xSearchField.addContent(searchField);
    xDocument.addContent(xSearchField);

    // Editierbutton Einf�gen - wenn die permission es erlaubt
    Element Editbutton = new Element("userCanEdit");

    // now we check this right for the current user
    String permString = String.valueOf(AI.checkPermission("create-classification"));
    Editbutton.addContent(permString);
    xDocument.addContent(Editbutton);

    // data as XML from outputNavigationTree
    Element xNavtree = new Element("navigationtree");
    xNavtree.setAttribute("classifID", cl.getClassificationID());
    xNavtree.setAttribute("categID", actItemID);
    xNavtree.setAttribute("predecessor", lastItemID);
    xNavtree.setAttribute("emptyLeafs", emptyLeafs);
    xNavtree.setAttribute("view", view);
    StringBuffer sb = new StringBuffer();
    if (doctypeArray.length > 1) sb.append("(");
    for (int i=0; i<doctypeArray.length;i++) {
      sb.append("(objectType+=+").append(doctypeArray[i]).append(")");
      if ((doctypeArray.length > 1) && (i<doctypeArray.length-1)) {
        sb.append("+or+");
      }
    }
    if (doctypeArray.length > 1) sb.append(")");
    xNavtree.setAttribute("doctype", sb.toString());
    xNavtree.setAttribute("restriction", restriction != null ? restriction : "");
    xNavtree.setAttribute("searchField", searchField);

    int i = 0;
    // MCRNavigTreeLine line;
    Element line;
    while ((line = getTreeline(i++)) != null) {    

      String catid = line.getAttributeValue("ID");
      int numDocs = Integer.parseInt( line.getAttributeValue("counter"));
      String status = line.getAttributeValue("hasChildren");
      
      Element label = (Element) XPath.selectSingleNode(line, "label[@xml:lang='" + lang +"']");
      String text =  label.getAttributeValue("text" );
      String description = label.getAttributeValue("description");    
      
      int level =  Integer.parseInt(line.getAttributeValue("level"));
      
      // f�r Sortierung schon mal die leveltiefe bestimmen
      LOGGER.debug(" NumDocs - " + numDocs);

      if (emptyLeafs.endsWith("no") && numDocs == 0) {
        LOGGER.debug(" empty Leaf continue - " + emptyLeafs);
        continue;
      }
      Element xRow = new Element("row");
      Element xCol1 = new Element("col");
      Element xCol2 = new Element("col");
      int numLength = String.valueOf(numDocs).length();

      xRow.addContent(xCol1);
      xRow.addContent(xCol2);
      xNavtree.addContent(xRow);

      xCol1.setAttribute("lineLevel", String.valueOf(level - 1));
      xCol1.setAttribute("childpos", "middle");

      if (level > maxlevel) {
        xCol1.setAttribute("childpos", "first");
        maxlevel = level;
        if (getTreeline(i) == null) {
          // Spezialfall nur genau ein Element
          xCol1.setAttribute("childpos", "firstlast");
        }
      } else if (getTreeline(i) == null) {
        xCol1.setAttribute("childpos", "last");
      }

      xCol1.setAttribute("folder1", "folder_plain");
      xCol1.setAttribute("folder2", numDocs > 0 ? "folder_closed_in_use" : "folder_closed_empty");

      if (status.equals("T")) {
        xCol1.setAttribute("plusminusbase", catid);
        xCol1.setAttribute("folder1", "folder_plus");
      } else if (status.equals("F")) {
        xCol1.setAttribute("plusminusbase", catid);
        xCol1.setAttribute("folder1", "folder_minus");
        xCol1.setAttribute("folder2", numDocs > 0 ? "folder_open_in_use" : "folder_open_empty");
      }

      xCol2.setAttribute("numDocs", String.valueOf(numDocs));
      String fmtnumDocs = fillToConstantLength(String.valueOf(numDocs), " ", 6);
      xCol2.setAttribute("fmtnumDocs", fmtnumDocs);

      if (numLength > 0) {
        String search = uri;
        if (catid.equalsIgnoreCase(actItemID))
          search += "/..";
        else
          search += "/" + catid;

        if (search.indexOf("//") > 0)
          search = search.substring(0, search.indexOf("//")) + search.substring(search.indexOf("//") + 1);

        xCol2.setAttribute("searchbase", search);
        xCol2.setAttribute("lineID", catid);
      }

      xCol2.addContent(text);

      if (showComments() && ( description != null)) {
        Element comment = new Element("comment");
        xCol2.addContent(comment);
        comment.setText( description );
      }
    }
    
    xNavtree.setAttribute("rowcount", "" + i);
    xDocument.addContent(xNavtree);

    if ("true".equals(sort))
      xDocument = sortMyTree(xDocument);
    return new org.jdom.Document(xDocument);
  }

  public void update(String categID) throws Exception {
    int lastLevel = 0;
    boolean hideLevel = false;

    LOGGER.debug(this.getClass() + " update CategoryTree for: " + categID);    
    Element line;    
    for (int i = 0; i < lines.size(); i++) {
      line = getTreeline(i);
      String catid = line.getAttributeValue("ID");
      String status = line.getAttributeValue("hasChildren");
      int level =  Integer.parseInt(line.getAttributeValue("level"));
      
      hideLevel = hideLevel && (level > lastLevel);
      LOGGER.debug(" compare CategoryTree on " + i + "_" + catid + " to " + categID);

      if (view.endsWith("tree")) {
        if (hideLevel) {
          lines.remove(i--);
        } else if (categID.equals( catid)) {
          if (status.equals("F")) // hide expanded category -   // children
          {
            line.setAttribute("hasChildren" ,"T");
            hideLevel = true;
            lastLevel = level;
          } else if (status.equals("T")) // expand category -   // children
          {
            line.setAttribute("hasChildren" ,"F");
            putCategoriesintoLines(i, new MCRCategoryItem(catid, classif.ID,"").getChildrenFromJDomAsList(), level +1);
          }
        }
      } else {
        if (categID.equalsIgnoreCase(catid)) {
          line.setAttribute("level" ,"0");
          LOGGER.info(" expand " + catid);
          line.setAttribute("hasChildren" ,"F");
          putCategoriesintoLines(i, new MCRCategoryItem(catid, classif.ID,"").getChildrenFromJDomAsList(), level +1);
        } else {
          LOGGER.debug(" remove lines " + i + "_" + catid);
          lines.remove(i--);
        }
      }

    }
  }  

  
  // don't use it works not really good 
  
  private Element sortMyTree(Element xDocument) {
    Element xDoc = (Element) xDocument.clone();
    for (int i = 0; i < maxlevel; i++) {
      xDoc = sortMyTreeperLevel(xDoc, i, 0);
    }
    return xDoc;
  }

  private Element sortMyTreeperLevel(Element xDocument, int activelevel, int position) {
    Element xDoc = xDocument;
    Element aktRow = ((Element) xDoc.getChild("navigationtree").getChildren().get(position));
    String aktText = ((Element) aktRow.getChildren().get(1)).getText();

    List children = xDoc.getChild("navigationtree").getChildren();
    int level = activelevel;
    int Cnt = 0;
    for (int j = position + 1; j < children.size(); j++) {
      Element child = (Element) (children.get(j));
      Element col1 = (Element) (child.getChildren().get(0));
      Element col2 = (Element) (child.getChildren().get(1));

      try {
        level = col1.getAttribute("lineLevel").getIntValue();
      } catch (Exception ignored) {
        ;
      }

      String sText = col2.getText();

      if (activelevel == level) {
        if (aktText.compareTo(sText) > 0) {
          changeRows(aktRow, child);
          boolean bjumpOverChilds = true;
          while (bjumpOverChilds && j < children.size() - 1) {
            Element next = (Element) (children.get(j + 1));
            if (next != null) {
              Element colx = (Element) (child.getChildren().get(0));
              int nextlevel = level;
              try {
                nextlevel = colx.getAttribute("lineLevel").getIntValue();
              } catch (Exception ignored) {
                ;
              }

              if (nextlevel > level)
                j++;
              else
                bjumpOverChilds = false;
            } else
              bjumpOverChilds = false;
          }
          Cnt++;
          xDoc = sortMyTreeperLevel(xDoc, activelevel, position);
        }
        if (position < children.size() - 1 && j == children.size() - 1 && Cnt == 0)
          xDoc = sortMyTreeperLevel(xDoc, activelevel, position + 1);
      }
    }
    return xDoc;
  }

  private void changeRows(Element aktRow, Element child) {

    Element col1 = (Element) (child.getChildren().get(0));
    Element col2 = (Element) (child.getChildren().get(1));
    Element placer = (Element) (aktRow.clone());
    Element place1 = (Element) (placer.getChildren().get(0));
    Element place2 = (Element) (placer.getChildren().get(1));

    Element xc1 = new Element("col");
    Element xc2 = new Element("col");

    aktRow.setContent(0, xc1);
    xc1.setAttribute("lineLevel", col1.getAttributeValue("lineLevel"));

    xc1.setAttribute("childpos", col1.getAttributeValue("childpos"));
    xc1.setAttribute("folder1", col1.getAttributeValue("folder1"));
    xc1.setAttribute("folder2", col1.getAttributeValue("folder2"));
    if (col1.getAttributeValue("plusminusbase") != null)
      xc1.setAttribute("plusminusbase", col1.getAttributeValue("plusminusbase"));

    aktRow.setContent(1, xc2);
    xc2.setAttribute("numDocs", col2.getAttributeValue("numDocs"));
    xc2.setAttribute("fmtnumDocs", col2.getAttributeValue("fmtnumDocs"));
    xc2.setAttribute("searchbase", col2.getAttributeValue("searchbase"));
    if (col2.getAttributeValue("lineID") != null)
      xc2.setAttribute("lineID", col2.getAttributeValue("lineID"));
    xc2.addContent(col2.getText());

    Element xc3 = new Element("col");
    Element xc4 = new Element("col");

    child.setContent(0, xc3);
    xc3.setAttribute("lineLevel", place1.getAttributeValue("lineLevel"));
    xc3.setAttribute("childpos", place1.getAttributeValue("childpos"));
    xc3.setAttribute("folder1", place1.getAttributeValue("folder1"));
    xc3.setAttribute("folder2", place1.getAttributeValue("folder2"));
    if (place1.getAttributeValue("plusminusbase") != null)
      xc3.setAttribute("plusminusbase", place1.getAttributeValue("plusminusbase"));

    child.setContent(1, xc4);
    xc4.setAttribute("numDocs", place2.getAttributeValue("numDocs"));
    xc4.setAttribute("fmtnumDocs", place2.getAttributeValue("fmtnumDocs"));
    xc4.setAttribute("searchbase", place2.getAttributeValue("searchbase"));
    if (place2.getAttributeValue("lineID") != null)
      xc4.setAttribute("lineID", place2.getAttributeValue("lineID"));
    xc4.addContent(place2.getText());
  }

  private static String fillToConstantLength(String value, String fillsign, int length) {
    int valueLength = value.length();
    if (valueLength >= length)
      return value;
    StringBuffer ret = new StringBuffer("");
    for (int i = 0; i < length - valueLength; i++) {
      ret.append(fillsign);
    }
    ret.append(value);
    return ret.toString();
  }

}
