/*  											*/
/* Image Viewer - MCR-IView 1.0, 05-2006  		*/
/* +++++++++++++++++++++++++++++++++++++		*/
/*  											*/
/* Andreas Trappe 	- concept, devel. in misc.  */
/* Britta Kapitzki	- Design					*/
/* Thomas Scheffler - html prototype		    */
/* Stephan Schmidt 	- html prototype			*/
/*  											*/

package org.mycore.frontend.iview;

import java.awt.Point;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;

import org.mycore.common.MCRCache;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.services.imaging.MCRImgCacheCommands;
import org.mycore.services.imaging.MCRImgService;
import org.mycore.services.imaging.Stopwatch;

/**
 * @author Andreas Trappe, Chi Vu Huu
 * 
 */

public class MCRIViewServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;

    private static Logger LOGGER = Logger.getLogger(MCRIViewServlet.class);

    private Stopwatch timer = new Stopwatch();

    private String dateFormat = "dd.MM.yyyy HH:mm:ss";

    private DateFormat dateFormatter = new SimpleDateFormat(dateFormat);

    public void init() throws MCRConfigurationException, ServletException {
        super.init();
    }

    public void doGetPost(MCRServletJob job) throws IOException, ServletException, JDOMException {

        HttpServletRequest request = job.getRequest();
        HttpServletResponse response = job.getResponse();

        // get host config
        String hostAlias = getProperty(request, "hosts");
        if ((hostAlias == null) || (hostAlias.trim().length() == 0))
            hostAlias = "local";

        // get PATH of MCRNode
        String requestPath = request.getPathInfo();

        // printRequest(request);

        // generate error page if path is empty
        if (requestPath == null)
            prepareErrorPage(request, response, "Error: HTTP request path is null");
        // generate error page if path is incorrect
        StringTokenizer st = new StringTokenizer(requestPath, "/");
        if (!st.hasMoreTokens())
            prepareErrorPage(request, response, "Error: HTTP request path is null");

        /*
         * ############################################################################################################# //
         * get mode and process
         */
        timer.start();
        if ((request.getParameter("mode") != null) && (!request.getParameter("mode").equals(""))) {
            if (request.getParameter("mode").equals("generateLayout")) {
                generateLayout(request, response);
            } else if (request.getParameter("mode").equals("getImage")) {
                getImage(request, response);
            } else if (request.getParameter("mode").equals("getMetadata")) {
                getMetadata(request, response, st);
            } else if (request.getParameter("mode").equals("setMetadata"))
                setMetadata(request, response);
        }
        timer.stop();
        timer.reset();
    }

    public void setMetadata(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        LOGGER.debug("AJAX: received size of image area - " + "width=" + request.getParameter("XSL.browser.res.width.SESSION") + "px, height="
                + request.getParameter("XSL.browser.res.height.SESSION") + "...");
        Properties resolution = new Properties();
        resolution.put("XSL.browser.res.width.SESSION", request.getParameter("XSL.browser.res.width.SESSION"));
        resolution.put("XSL.browser.res.height.SESSION", request.getParameter("XSL.browser.res.height.SESSION"));
        updateIViewConfig(new Properties(), request, resolution, "AJAX resol. data added...");

        forwardJDOM(request, response, new Element("setMetadata").setText("successfully"));

    }

    public void generateLayout(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, JDOMException {

        MCRFilesystemNode nodeToBeDisplayed = getMCRNodeByRequest(request, response);
        Properties iViewConfig = setIViewConfig(request);
        response.setHeader("pragma", "no-cache");

        setZoom(request, iViewConfig, nodeToBeDisplayed);
        // move ? -> recalculate POI
        if (iViewConfig.getProperty("MCR.Module-iview.move") != null)
            movePOI(request, iViewConfig, nodeToBeDisplayed);

        int origWidth = getWidthOfImage(nodeToBeDisplayed);
        int origHeight = getHeightOfImage(nodeToBeDisplayed);

        // thumbnail highlighting
        float orig2ThumbSF = MCRIViewTools.computeScaleFactor(getWidthOfImage(nodeToBeDisplayed), getHeightOfImage(nodeToBeDisplayed),
                getWidthOfThumbnail(iViewConfig), getHeightOfThumbnail(iViewConfig));
        int thumbPOIX = Math.round(getPOI(iViewConfig).x * orig2ThumbSF);
        int thumbPOIY = Math.round(getPOI(iViewConfig).y * orig2ThumbSF);
        int thumbWidth = Math.round(origWidth * orig2ThumbSF);
        int thumbHeight = Math.round(origHeight * orig2ThumbSF);

        // generate jdom ############################
        // ############# header
        Element jdom = new Element("iview");
        jdom.addContent(new Element("header"));
        // header - current zoom value
        jdom.getChild("header").addContent(new Element("currentZoom").setText(getZoom(iViewConfig)));
        // header - thumbnail highlighting data
        jdom.getChild("header").addContent(new Element("thumbHighLighting-X").setText(Integer.toString(thumbPOIX)));
        jdom.getChild("header").addContent(new Element("thumbHighLighting-Y").setText(Integer.toString(thumbPOIY)));
        jdom.getChild("header").addContent(new Element("thumbHighLighting-SF").setText(Float.toString(orig2ThumbSF)));
        jdom.getChild("header").addContent(new Element("thumbWidth").setText(Integer.toString(thumbWidth)));
        jdom.getChild("header").addContent(new Element("thumbHeight").setText(Integer.toString(thumbHeight)));
        jdom.getChild("header").addContent(new Element("origWidth").setText(Integer.toString(origWidth)));
        jdom.getChild("header").addContent(new Element("origHeight").setText(Integer.toString(origHeight)));

        // content
        jdom.addContent(new Element("content").addContent(new Element("ownerID").setText(nodeToBeDisplayed.getOwnerID())));
        // header - path
        if (nodeToBeDisplayed instanceof MCRFile)
            jdom.getChild("content").addContent(new Element("path").setText(nodeToBeDisplayed.getParent().getPath().toString()));
        else
            jdom.getChild("content").addContent(new Element("path").setText(nodeToBeDisplayed.getPath().toString()));
        // header - fileToBeDisplayed
        if (nodeToBeDisplayed instanceof MCRFile)
            jdom.getChild("content").addContent(new Element("fileToBeDisplayed").setText(nodeToBeDisplayed.getAbsolutePath().toString()));
        // header - parent
        if (nodeToBeDisplayed.hasParent()) {
            if ((nodeToBeDisplayed instanceof MCRFile) && (nodeToBeDisplayed.getParent().hasParent()))
                jdom.getChild("content").addContent(new Element("parent").setText(nodeToBeDisplayed.getParent().getParent().getPath()));
            else if (nodeToBeDisplayed instanceof MCRDirectory)
                jdom.getChild("content").addContent(new Element("parent").setText(nodeToBeDisplayed.getParent().getPath().toString()));
        }

        // ############# node list
        if (nodeToBeDisplayed != null && nodeToBeDisplayed instanceof MCRFile)
            jdom.getChild("content").addContent(getNodeList(nodeToBeDisplayed.getParent(), iViewConfig, request, response).detach());
        else if (nodeToBeDisplayed != null && nodeToBeDisplayed instanceof MCRDirectory)
            jdom.getChild("content").addContent(getNodeList((MCRDirectory) nodeToBeDisplayed, iViewConfig, request, response).detach());

        // forward jdom ############################
        forwardJDOM(request, response, jdom);
    }

    /**
     * Returns the width of an image as int, if possible. If not -1 will be
     * returned.
     * 
     * @param MCRFilesystemNode
     * @return width of image as int or -1, if no width can be found.
     */
    private int getWidthOfImage(MCRFilesystemNode fsn) {
        try {
            if (fsn instanceof MCRFile) {
                Element addData = fsn.getAdditionalData("ImageMetaData");
                // in some unknown circumstances this value is empty, so create
                // again
                if (addData == null) {
                    MCRImgCacheCommands.cacheFile((MCRFile) fsn, false);
                    addData = fsn.getAdditionalData("ImageMetaData");
                }
                String widthString = addData.getChild("imageSize").getChildText("width");
                int width = Integer.parseInt(widthString);
                return width;
            } else
                return -1;
        } catch (NumberFormatException nfe) {
            LOGGER.warn("", nfe);
            return -1;
        } catch (IOException ioe) {
            LOGGER.warn("", ioe);
            return -1;
        } catch (JDOMException jde) {
            LOGGER.warn("", jde);
            return -1;
        }
    }

    /**
     * Returns the height of an image as int, if possible. If not -1 will be
     * returned.
     * 
     * @param MCRFilesystemNode
     * @return height of image as int or -1, if no height can be found.
     */
    private int getHeightOfImage(MCRFilesystemNode fsn) {
        try {
            if (fsn instanceof MCRFile) {
                Element addData = fsn.getAdditionalData("ImageMetaData");
                if (addData == null)
                    return -1;
                String widthString = addData.getChild("imageSize").getChildText("height");
                int width = Integer.parseInt(widthString);
                return width;
            } else
                return -1;
        } catch (NumberFormatException nfe) {
            return -1;
        } catch (IOException ioe) {
            return -1;
        } catch (JDOMException jde) {
            return -1;
        }
    }

    /**
     * Use this to put the value of a zoom to iViewConfig, if nothing is known.
     * E.g. zoom can be "thumbnail", "fitToScreen", "0.7", "+0.1" etc. Zoom in
     * (+) and zoom out (-) are calculated. "fitToScreen", "thumbnail" and
     * "fitToWidth" are not calculated!
     * 
     * @param request
     *            HttpRequest
     * @param Properties
     *            iViewConfig
     */
    private void setZoom(HttpServletRequest request, Properties iViewConfig, MCRFilesystemNode fsn) {

        String zoom = getZoom(iViewConfig);
        float newZoom;

        // zoom out
        if (zoom.substring(0, 1).equals("-")) {
            // calculate zoom
            float lastZoom = getZoomValue(iViewConfig);
            newZoom = getZoomOut(lastZoom);
            // save zoom
            Properties zoomProp = new Properties();
            zoomProp.put("XSL.MCR.Module-iview.navi.zoom.SESSION", java.lang.Float.toString(newZoom));
            updateIViewConfig(iViewConfig, request, zoomProp, "setZoomOut=" + java.lang.Float.toString(newZoom) + "........................");
            // save zoomValue
            setZoomValue(request, newZoom, iViewConfig);
            // move poi
            Point poi = calculatePOIAfterZooming(iViewConfig, getPOI(iViewConfig), lastZoom, newZoom, fsn, true);
            setPOI(request, iViewConfig, poi);
        }
        // zoom in
        else if (zoom.substring(0, 1).equals(" ")) {
            // calculate zoom
            float lastZoom = getZoomValue(iViewConfig);
            newZoom = getZoomIn(lastZoom);
            // save zoom
            Properties zoomProp = new Properties();
            zoomProp.put("XSL.MCR.Module-iview.navi.zoom.SESSION", java.lang.Float.toString(newZoom));
            updateIViewConfig(iViewConfig, request, zoomProp, "setZoomIn=" + java.lang.Float.toString(newZoom) + "........................");
            // save zoomValue
            setZoomValue(request, newZoom, iViewConfig);
            // move poi
            Point poi = calculatePOIAfterZooming(iViewConfig, getPOI(iViewConfig), lastZoom, newZoom, fsn, true);
            setPOI(request, iViewConfig, poi);
        }
        // real given zoom value (is not "thumbnail | fitToScreen | fitToWidth")
        else if (!(zoom.equals("thumbnail") || zoom.equals("fitToScreen") || zoom.equals("fitToWidth"))) {
            float oldZoomValue = getZoomValue(iViewConfig);
            newZoom = java.lang.Float.parseFloat(zoom);
            newZoom = validateZoom(newZoom);
            // zoom changed ?
            if (oldZoomValue != newZoom) {
                // move poi
                Point poi = calculatePOIAfterZooming(iViewConfig, getPOI(iViewConfig), getZoomValue(iViewConfig), newZoom, fsn, true);
                setPOI(request, iViewConfig, poi);
            }
            setZoomValue(request, newZoom, iViewConfig);
        }
    }

    private Point calculatePOIAfterZooming(Properties iViewConfig, Point oldPOI, float oldZoom, float newZoom, MCRFilesystemNode fsn, boolean log) {

        Point midpoint = new Point();
        midpoint.x = Math.round(oldPOI.x + (getWidthOfBrowser(iViewConfig) / oldZoom) / 2);
        midpoint.y = Math.round(oldPOI.y + (getHeightOfBrowser(iViewConfig) / oldZoom) / 2);

        Point newPOI = new Point();
        newPOI.x = midpoint.x - Math.round(getWidthOfBrowser(iViewConfig) / newZoom / 2);
        newPOI.y = midpoint.y - Math.round(getHeightOfBrowser(iViewConfig) / newZoom / 2);

        if (log)
            LOGGER.debug("calculatePOIAfterZooming: new POI=(" + newPOI.x + "," + newPOI.y + ")");

        newPOI = validatePOI(newPOI, iViewConfig, fsn, true);

        return newPOI;
    }

    private void setZoomValue(HttpServletRequest request, float zoomValue, Properties iViewConfig) {
        Properties zoomValueProp = new Properties();
        zoomValueProp.put("XSL.MCR.Module-iview.navi.zoomValue.SESSION", java.lang.Float.toString(zoomValue));
        updateIViewConfig(iViewConfig, request, zoomValueProp, "setZoomValue=" + java.lang.Float.toString(zoomValue) + "........................");
    }

    private float getZoomValue(Properties iViewConfig) {
        if (iViewConfig.containsKey("MCR.Module-iview.navi.zoomValue")) {
            return java.lang.Float.parseFloat(iViewConfig.getProperty("MCR.Module-iview.navi.zoomValue"));
        } else {
            return 1.0F;
        }
    }

    private float getZoomIn(float currentZoom) {
        // calculate
        float newZoom;
        float zoomDistance = getZoomDistance();
        newZoom = currentZoom + zoomDistance;
        // validate if out of border
        newZoom = validateZoom(newZoom);
        // round
        newZoom = roundZoom(newZoom, true);
        LOGGER.debug("zoom in value calculated = " + java.lang.Float.toString(newZoom) + " (old: " + java.lang.Float.toString(currentZoom) + ")");
        return newZoom;
    }

    private float roundZoom(float zoom, boolean log) {
        float zoomRounded = (float) (Math.round(zoom * 10)) / 10;
        if (log)
            LOGGER.debug("zoom value rounded=" + zoom + " (old:" + zoomRounded);
        return zoomRounded;
    }

    private float getZoomOut(float currentZoom) {
        // calculate
        float newZoom;
        float zoomDistance = getZoomDistance();
        newZoom = currentZoom - zoomDistance;
        // validate if out of border
        newZoom = validateZoom(newZoom);
        // round
        newZoom = roundZoom(newZoom, true);
        LOGGER.debug("zoom out value calculated = " + java.lang.Float.toString(newZoom) + " (old: " + java.lang.Float.toString(currentZoom) + ")");
        return newZoom;
    }

    private float getZoomDistance() {
        float zoomDist = java.lang.Float.parseFloat((CONFIG.getString("MCR.Module-iview.zoomDistance")));
        LOGGER.debug("getZoomDistance=" + java.lang.Float.toString(zoomDist));
        return zoomDist;
    }

    private float validateZoom(float zoom) {
        float verZoom = zoom;
        if (verZoom < 0.1F)
            verZoom = 0.1F;
        else if (verZoom > 1.0F)
            verZoom = 1.0F;
        return verZoom;
    }

    public void getImage(HttpServletRequest request, HttpServletResponse response) throws ServletException, FileNotFoundException, IOException {

        // get right file
        MCRFilesystemNode nodeToBeDisplayed = getMCRNodeByRequest(request, response);
        MCRFile image = null;
        if (nodeToBeDisplayed instanceof MCRFile)
            image = (MCRFile) nodeToBeDisplayed;
        else {
            prepareErrorPage(request, response, "mode=getImage only works with MCRFile, " + "requested MCRFilesystemNode is not of type MCRFile");
        }

        // get viewer properties
        Properties iViewConfig = getIViewConfig(request);
        String zoom = "";
        int availableWidth = 0;
        int availableHeight = 0;
        int xPOI = 0;
        int yPOI = 0;
        // // direct call of getImage to get thumbnail
        if (request.getParameter("XSL.MCR.Module-iview.navi.zoom") != null && request.getParameter("XSL.MCR.Module-iview.navi.zoom").equals("thumbnail")) {
            zoom = "thumbnail";
        } // // call from IView
        else {
            zoom = getZoom(iViewConfig);
            availableWidth = getAvailableWidth(zoom, nodeToBeDisplayed, iViewConfig);
            availableHeight = getAvailableHeight(zoom, nodeToBeDisplayed, iViewConfig);
            LOGGER.debug("requested zoomFactor=" + zoom);
            // //// get ROI (Region of interest)
            xPOI = getPOI(iViewConfig).x;
            yPOI = getPOI(iViewConfig).y;
        }

        // put image in requested size to output stream
        ServletOutputStream out = response.getOutputStream();
        response.setContentType("image/jpeg");
        MCRImgService imgService = new MCRImgService();
        Stopwatch timer = new Stopwatch();
        timer.start();

        if (zoom.equals("thumbnail")) {
            int thumbnailWidth = getWidthOfThumbnail(iViewConfig);
            int thumbnailHeight = getHeightOfThumbnail(iViewConfig);
            imgService.getImage(image, thumbnailWidth, thumbnailHeight, out);
            timer.stop();
            LOGGER.debug("finished getting image with zoom=thumbnail from ");
        } else if (zoom.equals("fitToWidth")) {
            imgService.getImage(image, xPOI, yPOI, availableWidth, availableHeight, out);
            float zoomValue = imgService.getScaleFactor();
            setZoomValue(request, zoomValue, iViewConfig);
            LOGGER.debug("finished getting image with zoom=fitToWidth(" + zoomValue + ") from ");
        } else if (zoom.equals("fitToScreen")) {
            imgService.getImage(image, availableWidth, availableHeight, out);
            float zoomValue = imgService.getScaleFactor();
            setZoomValue(request, zoomValue, iViewConfig);
            LOGGER.debug("finished getting image with zoom=fitToScreen(" + zoomValue + ") from ");
        } // normal number (10 or 20...)
        else {
            float zoomValue = java.lang.Float.parseFloat(zoom);
            LOGGER.debug("imgService.getImage(image, xPOI(" + xPOI + "), yPOI(" + yPOI + "), " + "availableWidth(" + availableWidth + "), availableHeight("
                    + availableHeight + "), zoomValue(" + zoomValue + "), out);");
            imgService.getImage(image, xPOI, yPOI, availableWidth, availableHeight, zoomValue, out);
            timer.stop();
            LOGGER.debug("finished getting image with real given zoom=" + zoomValue + " from ");
        }
        out.flush();
        out.close();

    }

    private int getAvailableWidth(String zoom, MCRFilesystemNode nodeToBeDisplayed, Properties iViewConfig) {
        if (scrollBar(iViewConfig)) {
            if (zoom.equals("fitToWidth") || zoom.equals("fitToScreen")) {
                return getWidthOfBrowser(iViewConfig);
            } // normal number (10 or 20...)
            else {
                return getWidthOfImage(nodeToBeDisplayed);
            }
        } else {
            return getWidthOfBrowser(iViewConfig);
        }
    }

    private int getAvailableHeight(String zoom, MCRFilesystemNode nodeToBeDisplayed, Properties iViewConfig) {
        if (scrollBar(iViewConfig)) {
            if (zoom.equals("fitToWidth")) {
                return getHeightOfImage(nodeToBeDisplayed);
            } else if (zoom.equals("fitToScreen")) {
                return getHeightOfBrowser(iViewConfig);
            }
            // normal number (10 or 20...)
            else
                return getHeightOfImage(nodeToBeDisplayed);
        } else {
            return getHeightOfBrowser(iViewConfig);
        }
    }

    private boolean scrollBar(Properties iViewConfig) {
        if (!iViewConfig.getProperty("MCR.Module-iview.scrollBars", "true").equals("true"))
            return false;
        else
            return true;
    }

    private int getHeightOfThumbnail(Properties iViewConfig) {
        int thumbnailHeight = Integer.parseInt(iViewConfig.getProperty("MCR.Module-iview.thumbnail.size.height"));
        return thumbnailHeight;
    }

    private int getWidthOfThumbnail(Properties iViewConfig) {
        int thumbnailWidth = Integer.parseInt(iViewConfig.getProperty("MCR.Module-iview.thumbnail.size.width"));
        return thumbnailWidth;
    }

    private String getZoom(Properties iViewConfig) {
        return iViewConfig.getProperty("MCR.Module-iview.navi.zoom");
    }

    private int getHeightOfBrowser(Properties iViewConfig) {
        return Integer.parseInt(iViewConfig.getProperty("browser.res.height").toString());
    }

    private int getWidthOfBrowser(Properties iViewConfig) {
        return Integer.parseInt(iViewConfig.getProperty("browser.res.width").toString());
    }

    public boolean stringNotEmpty(String string) {
        if (string != null && !string.equals(""))
            return true;
        else
            return false;
    }

    /**
     * generates an JDOM-Element containing a list of supported children of a
     * given node
     */
    private Element getNodeList(MCRDirectory dir, Properties iViewConfig, HttpServletRequest request, HttpServletResponse response) throws IOException,
            ServletException, JDOMException {
        // get cached file node list from MCRSession OR init new one
        MCRSession session = MCRSessionMgr.getCurrentSession();
        String cacheObjKey = "IView.FileNodesList";
        Object cacheObj = session.get(cacheObjKey);
        MCRCache cachedFileNodeList;
        // // init new one
        if (cacheObj == null) {
            cachedFileNodeList = new MCRCache(10, "IViewFileNodeList in MCRSession,session=" + session.getID());
            session.put(cacheObjKey, cachedFileNodeList);
        } // get cached one
        else
            cachedFileNodeList = (MCRCache) cacheObj;
        String cachedFileNodeListID = dir.getID();

        // get node list from cache
        if (cachedFileNodeList.get(cachedFileNodeListID) != null && !stringNotEmpty(request.getParameter("XSL.MCR.Module-iview.defaultSort.SESSION"))) {
            Element nodeList = (Element) cachedFileNodeList.get(cachedFileNodeListID);
            return nodeList;
        }
        // generate node list and put to cache
        else {
            String sort = iViewConfig.getProperty("MCR.Module-iview.defaultSort").toString();
            String sortOrder = iViewConfig.getProperty("MCR.Module-iview.defaultSort.order").toString();
            int sortValue = 0;
            int orderValue = 0;
            if (sort.equals("name"))
                sortValue = 1;
            else if (sort.equals("size"))
                sortValue = 2;
            else if (sort.equals("lastModified"))
                sortValue = 3;
            if (sortOrder.equals("ascending"))
                orderValue = 4;
            else
                orderValue = 5;
            LOGGER.debug("sort nodes by " + sort + ", order=" + sortOrder);
            MCRFileNodeComparator comp = new MCRFileNodeComparator(sortValue, orderValue);
            // get nodes
            Element root = new Element("nodes");
            LOGGER.debug("start to get children list");
            MCRFilesystemNode[] children = dir.getChildren(comp);
            LOGGER.debug("finsihshed getting children list");

            LOGGER.debug("start to go throug children list");
            for (int i = 0; i < children.length; i++) {
                if (getSupport(children[i])) {
                    Element node = new Element("node");
                    node.setAttribute("ID", children[i].getID());
                    root.addContent(node);
                    addChild(node, "name", children[i].getName());
                    addChild(node, "size", String.valueOf(children[i].getSize()));
                    addDate(node, "lastModified", children[i].getLastModified());
                    if (children[i] instanceof MCRFile) {
                        node.setAttribute("type", "file");
                        MCRFile file = (MCRFile) (children[i]);
                        addChild(node, "contentType", file.getContentTypeID());
                        addChild(node, "md5", file.getMD5());
                        addChild(node, "label", file.getLabel());
                    } else
                        node.setAttribute("type", "directory");
                }
            }
            LOGGER.debug("finished to go throug children list");
            // cache
            cachedFileNodeList.put(cachedFileNodeListID, root);
            LOGGER.debug("finished to get node list for" + dir.getName());
            return root;
        }
    }

    private void addChild(Element parent, String itemName, String content) {
        if ((content == null) || (content.trim().length() == 0))
            return;
        parent.addContent(new Element(itemName).addContent(content.trim()));
    }

    private void addDate(Element parent, String type, GregorianCalendar date) {
        Element xDate = new Element("date");
        parent.addContent(xDate);

        xDate.setAttribute("type", type);

        String time = dateFormatter.format(date.getTime());

        xDate.setAttribute("format", dateFormat);
        xDate.addContent(time);
    }

    public void prepareErrorPage(HttpServletRequest request, HttpServletResponse response, String errorMessage) throws IOException, ServletException {
        LOGGER.error(errorMessage);
        generateErrorPage(request, response, HttpServletResponse.SC_BAD_REQUEST, errorMessage, new MCRException(errorMessage), false);
        return;
    }

    private Point getPOI(Properties iViewConfig) {
        int x = 0;
        int y = 0;

        if (!scrollBar(iViewConfig)) {
            if (iViewConfig.getProperty("MCR.Module-iview.roi.xpos") != null || iViewConfig.getProperty("MCR.Module-iview.roi.ypos") != null) {
                x = Integer.parseInt(iViewConfig.getProperty("MCR.Module-iview.roi.xpos"));
                y = Integer.parseInt(iViewConfig.getProperty("MCR.Module-iview.roi.ypos"));
            }
        }

        return new Point(x, y);
    }

    public void movePOI(HttpServletRequest request, Properties iViewConfig, MCRFilesystemNode fsn) {

        Point point = getPOI(iViewConfig);

        // position = 0,0
        if (iViewConfig.getProperty("MCR.Module-iview.move").equals("reset")) {
            LOGGER.debug("MCR.Module-iview.move=reset received -> xpos and ypos = 0");
            point.x = 0;
            point.y = 0;
        } // new position to be calculated
        else {
            // get distance to be moved
            int deltaX = getXDistance(iViewConfig, true);
            int deltaY = getYDistance(iViewConfig, true);

            getZoomValue(iViewConfig);

            if (iViewConfig.getProperty("MCR.Module-iview.move").equals("up")) {
                point.y = point.y - deltaY;
                LOGGER.debug("move up, new ROI=" + Integer.toString(point.x) + "," + Integer.toString(point.y) + "...");
            } else if (iViewConfig.getProperty("MCR.Module-iview.move").equals("right")) {
                point.x = point.x + deltaX;
                LOGGER.debug("move right, new ROI=" + Integer.toString(point.x) + "," + Integer.toString(point.y) + "...");
            } else if (iViewConfig.getProperty("MCR.Module-iview.move").equals("down")) {
                point.y = point.y + deltaY;
                LOGGER.debug("move down, new ROI=" + Integer.toString(point.x) + "," + Integer.toString(point.y) + "...");
            } else if (iViewConfig.getProperty("MCR.Module-iview.move").equals("left")) {
                point.x = point.x - deltaX;
                LOGGER.debug("move left, new ROI=" + Integer.toString(point.x) + "," + Integer.toString(point.y) + "...");
            } else if (iViewConfig.getProperty("MCR.Module-iview.move").equals("draged")) {
                LOGGER.debug("move by draging, old ROI=" + Integer.toString(point.x) + "," + Integer.toString(point.y) + "...");
                point.x = point.x - deltaX;
                point.y = point.y - deltaY;
                LOGGER.debug("move by draging, new ROI=" + Integer.toString(point.x) + "," + Integer.toString(point.y) + "...");
            }
            // verify
            point = validatePOI(point, iViewConfig, fsn, true);
        }
        setPOI(request, iViewConfig, point);
    }

    private void setPOI(HttpServletRequest request, Properties iViewConfig, Point point) {
        // save
        Properties roi = new Properties();
        roi.put("XSL.MCR.Module-iview.roi.xpos.SESSION", Integer.toString(point.x));
        roi.put("XSL.MCR.Module-iview.roi.ypos.SESSION", Integer.toString(point.y));
        updateIViewConfig(iViewConfig, request, roi, "save POI(" + Integer.toString(point.x) + "," + Integer.toString(point.y) + ")");
    }

    private int getXDistance(Properties iViewConfig, boolean log) {
        int deltaX = 0;
        if (iViewConfig.getProperty("MCR.Module-iview.move") != null && iViewConfig.getProperty("MCR.Module-iview.move").equals("draged")) {
            // x draged distance
            if (iViewConfig.containsKey("MCR.Module-iview.move.distanceX"))
                deltaX = Integer.parseInt(iViewConfig.getProperty("MCR.Module-iview.move.distanceX").toString());
        } else {
            // browser area
            deltaX = getWidthOfBrowser(iViewConfig);
        }
        // adaptated to current zoom
        // float deltaXAdap = deltaX/getZoomValue(iViewConfig);
        // round
        // deltaX = Math.round(deltaXAdap);
        if (log)
            LOGGER.debug("x-Distance ==> orig " + deltaX);
        deltaX = (int) (deltaX / getZoomValue(iViewConfig));
        if (log)
            LOGGER.debug("x-Distance ==> browser resol.(" + getWidthOfBrowser(iViewConfig) + ") / " + "currentZoomValue(" + getZoomValue(iViewConfig) + ") = "
                    + getWidthOfBrowser(iViewConfig) / getZoomValue(iViewConfig) + " -> rounded = " + deltaX);

        return deltaX;
    }

    private int getYDistance(Properties iViewConfig, boolean log) {
        int deltaY = 0;
        if (iViewConfig.getProperty("MCR.Module-iview.move") != null && iViewConfig.getProperty("MCR.Module-iview.move").equals("draged")) {
            // x draged distance
            if (iViewConfig.containsKey("MCR.Module-iview.move.distanceY"))
                deltaY = Integer.parseInt(iViewConfig.getProperty("MCR.Module-iview.move.distanceY").toString());
        } else {
            // browser area
            deltaY = getHeightOfBrowser(iViewConfig);
        }
        // adaptated to current zoom
        // float deltaYAdap = deltaY/getZoomValue(iViewConfig);
        // round
        // deltaY = Math.round(deltaYAdap);
        if (log)
            LOGGER.debug("y-Distance ==> orig " + deltaY);
        deltaY = (int) (deltaY / getZoomValue(iViewConfig));
        if (log)
            LOGGER.debug("y-Distance ==> browser resol.(" + getHeightOfBrowser(iViewConfig) + ") / " + "currentZoomValue(" + getZoomValue(iViewConfig) + ") = "
                    + getHeightOfBrowser(iViewConfig) / getZoomValue(iViewConfig) + " -> rounded = " + deltaY);
        return deltaY;
    }

    /**
     * Verify if ROI is right and/or below out of image
     * 
     * @param point
     */
    public Point validatePOI(Point point, Properties iViewConfig, MCRFilesystemNode fsn, boolean log) {
        Point valPoint = new Point(point.x, point.y);
        int width = getWidthOfImage(fsn);
        int height = getHeightOfImage(fsn);

        // out on right side
        if (width > -1) {
            int xDistance = getXDistance(iViewConfig, false);
            int widthOfImage = getWidthOfImage(fsn);
            int outOfBorder = (valPoint.x + xDistance) - widthOfImage;

            if (outOfBorder > 0) {
                if (log)
                    LOGGER.debug("validatePOI: found x=" + valPoint.x + " which is right out --> reset to " + (valPoint.x - outOfBorder));
                valPoint.x = valPoint.x - outOfBorder;
            }
        }
        // out on below side
        if (height > -1) {
            int outOfBorder = (valPoint.y + getYDistance(iViewConfig, false)) - getHeightOfImage(fsn);
            if (outOfBorder > 0) {
                if (log)
                    LOGGER.debug("validatePOI: found y=" + valPoint.y + " which is below out --> reset to " + (valPoint.y - outOfBorder));
                valPoint.y = valPoint.y - outOfBorder;
            }
        }
        // out on left side
        if (valPoint.x < 0) {
            if (log)
                LOGGER.debug("validatePOI: found x=" + valPoint.x + " which is left out  --> reset to 0");
            valPoint.x = 0;
        }
        // out on above side
        if (valPoint.y < 0) {
            if (log)
                LOGGER.debug("validatePOI: found y=" + valPoint.y + " which is above out  --> reset to 0");
            valPoint.y = 0;
        }

        return valPoint;
    }

    public void forwardJDOM(HttpServletRequest request, HttpServletResponse response, Element elem) throws IOException {

        // name of target xsl
        if (getProperty(request, "XSL.Style") == null)
            request.setAttribute("XSL.Style", "iview");
        // SAXBuilder builder = new SAXBuilder();
        Element root = new Element("mcr-module");
        root.addContent(elem);
        Document jdom = new Document(root);
        getLayoutService().doLayout(request, response, jdom);
    }

    /**
     * @param node -
     *            The MCRFilesystemNode to be verified
     * @return true if module imaging supports this MCRFile or at least one
     *         MCRFile within MCRDirectory, false if no support
     */
    public boolean getSupport(MCRFilesystemNode node) {
        String suppContTypes = new String(CONFIG.getString("MCR.Module-iview.SupportedContentTypes"));
        if (node instanceof MCRDirectory) {
            List<MCRFile> list = new Vector<MCRFile>();
            getFirstSupportedFile(list, suppContTypes, (MCRDirectory) node);
            if (list.size() == 1)
                return true;
            else
                return false;
        } else {
            MCRFile file = (MCRFile) node;
            if (getFileSupport(file))
                return true;
            else
                return false;
        }
    }

    public boolean getFileSupport(MCRFile file) {
        boolean support = false;
        String suppContTypes = new String(CONFIG.getString("MCR.Module-iview.SupportedContentTypes"));
        if (file instanceof MCRFile && suppContTypes.indexOf(file.getContentTypeID()) > -1)
            support = true;
        return support;
    }

    public void getFirstSupportedFile(List<MCRFile> list, String contentType, MCRDirectory rootNode) {
        MCRFilesystemNode[] nodes = rootNode.getChildren();
        int i = 0;
        while ((i < nodes.length) && list.size() != 1) {
            if (nodes[i] instanceof MCRDirectory) {
                MCRDirectory dir = (MCRDirectory) (nodes[i]);
                getFirstSupportedFile(list, contentType, dir);
            } else {
                MCRFile file = (MCRFile) (nodes[i]);
                if (contentType.indexOf(file.getContentTypeID()) > -1) {
                    list.add(file);
                }
            }
            i++;
        }
    }

    public MCRFilesystemNode getMCRNodeByRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String reqPath = request.getPathInfo();
        StringTokenizer st = new StringTokenizer(reqPath, "/");
        String ownerID = st.nextToken(); // ownerID

        // get node
        MCRFilesystemNode root = MCRFilesystemNode.getRootNode(ownerID);
        MCRDirectory dirOwnerID = (MCRDirectory) root;
        MCRFilesystemNode nodeToBeDisplayed = null;
        if (reqPath.length() - 1 == ownerID.length()) { // root of derivate
            nodeToBeDisplayed = dirOwnerID;
        } // some subfolder or file
        else {
            int pos = ownerID.length() + 1;
            String path = reqPath.substring(pos); // path of node without
            // ownerID
            nodeToBeDisplayed = dirOwnerID.getChildByPath(path);
        }
        if (nodeToBeDisplayed != null && !getSupport(nodeToBeDisplayed))
            prepareErrorPage(request, response, "Error: MCRFilesystemNode=" + nodeToBeDisplayed.getID() + "is not supported by Module-IView");
        return nodeToBeDisplayed;
    }

    public void getMetadata(HttpServletRequest request, HttpServletResponse response, StringTokenizer st) throws ServletException, IOException {

        // type == "support"
        // verifies if a Derrivat's main file is supported
        // and responses <support mainfile="...">true|false</support>
        if (request.getParameter("type").equals("support")) {
            // get name main file
            String derivID = st.nextToken();
            MCRDerivate deriv = new MCRDerivate();
            deriv.receiveFromDatastore(derivID);
            String nameOfMainFile = deriv.getDerivate().getInternals().getMainDoc();
            // verify support
            if (nameOfMainFile != null && !nameOfMainFile.equals("")) {
                MCRDirectory root = (MCRDirectory) MCRFilesystemNode.getRootNode(derivID);
                // get main file
                MCRFile mainFile = (MCRFile) root.getChildByPath(nameOfMainFile);
                if (getFileSupport(mainFile)) {
                    forwardJDOM(request, response, new Element("support").setAttribute("mainFile", mainFile.getAbsolutePath()).setText("true"));
                } else
                    forwardJDOM(request, response, new Element("support").setText("false"));
            } else
                forwardJDOM(request, response, new Element("support").setText("false"));
        }
    }

    @SuppressWarnings("unchecked")
    public Properties getIViewConfig(HttpServletRequest request) {
        LOGGER.debug("getting IViewConfig....................................");
        Properties iViewConfig = new Properties();
        // PROPERTIES: Read all properties from mycore.properties
        iViewConfig = (Properties) (MCRConfiguration.instance().getProperties().clone());

        // SESSION: Read all *.xsl attributes that are stored in the browser's
        // session
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        Iterator<Object> sessionKeys = mcrSession.getObjectsKeyList();
        if (mcrSession != null) {
            while (sessionKeys.hasNext()) {
                String key = sessionKeys.next().toString();
                if (key.startsWith("XSL.")) {
                    iViewConfig.put(key.substring(4), mcrSession.get(key));
                }
            }
        }
        return iViewConfig;
    }

    public void updateIViewConfig(Properties iViewConfig, HttpServletRequest request, Properties dateUp, String logMessage) {
        LOGGER.debug("updating iViewConfig (" + logMessage + ").....................................");

        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        Enumeration<?> e = dateUp.keys();
        while (e.hasMoreElements()) {
            String key = e.nextElement().toString();
            String value = dateUp.get(key).toString();

            if (key.startsWith("XSL.")) {
                // store parameter in session 'cause it ends with *.SESSION
                if (key.endsWith(".SESSION")) {
                    String key4IViewConfig = key.substring(4, key.length() - 8);
                    String key4Session = key.substring(0, key.length() - 8);

                    iViewConfig.put(key4IViewConfig, value);
                    mcrSession.put(key4Session, value);
                    LOGGER.debug("update IViewConfig: found " + key + "=" + dateUp.getProperty(key) + " that should be saved in session, safed " + key4Session
                            + "=" + value);
                } else {
                    iViewConfig.put(key.substring(4), value);
                }
            }

        }
        // return iViewConfig;
    }

    @SuppressWarnings("unchecked")
    public Properties setIViewConfig(HttpServletRequest request) throws UnsupportedEncodingException {

        LOGGER.debug("setting IViewConfig....................................");

        // PROPERTIES: Read all properties from mycore.properties
        Properties iViewConfig = (Properties) (MCRConfiguration.instance().getProperties().clone());

        // SESSION: Read all *.xsl attributes that are stored in the browser
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        Iterator<Object> sessionKeys = mcrSession.getObjectsKeyList();
        if (mcrSession != null) {
            while (sessionKeys.hasNext()) {
                String name = (String) (sessionKeys.next());
                if (name.startsWith("XSL.")) {
                    iViewConfig.put(name.substring(4), mcrSession.get(name));
                }
            }
        }

        // HTTP-REQUEST-PARAMETER: Read all *.xsl attributes from the client
        for (Enumeration e = request.getParameterNames(); e.hasMoreElements();) {
            String name = (String) (e.nextElement());

            if (name.startsWith("XSL.")) {
                if (!name.endsWith(".SESSION")) {
                    iViewConfig.put(name.substring(4), request.getParameter(name));
                } // store parameter in session if ends with *.SESSION
                else {
                    iViewConfig.put(name.substring(4, name.length() - 8), request.getParameter(name));
                    if (mcrSession != null) {
                        mcrSession.put(name.substring(0, name.length() - 8), request.getParameter(name));
                        LOGGER.debug("found HTTP-Req.-Parameter " + name + "=" + request.getParameter(name) + " that should be saved in session, safed "
                                + name.substring(0, name.length() - 8) + "=" + request.getParameter(name));
                    }

                }
            }
        }

        // SERVLETS-REQUEST-ATTRIBUTES: Read all *.xsl attributes provided by
        // the invoking servlet
        for (Enumeration e = request.getAttributeNames(); e.hasMoreElements();) {
            String name = (String) (e.nextElement());
            if (name.startsWith("XSL.")) {
                if (!name.endsWith(".SESSION")) {
                    iViewConfig.put(name.substring(4), request.getAttribute(name));
                } // store parameter in session if ends with *.SESSION
                else {
                    iViewConfig.put(name.substring(4, name.length() - 8), request.getAttribute(name));
                    if (mcrSession != null) {
                        mcrSession.put(name.substring(0, name.length() - 8), request.getAttribute(name));
                        LOGGER.debug("found Req.-Attribut " + name + "=" + request.getAttribute(name) + " that should be saved in session, safed "
                                + name.substring(0, name.length() - 8) + "=" + request.getAttribute(name));
                    }

                }
            }

        }
        LOGGER.debug("+++++++++++++++++++++++++++++++++++++++++++++ ");

        // ensure iview session is not timed out
        verifyIViewConfig(iViewConfig, request);

        return iViewConfig;
    }

    /**
     * Ensure iview session is not timed out
     * 
     * @param iViewConfig
     * @param request
     *            TODO
     * @throws UnsupportedEncodingException
     */
    private void verifyIViewConfig(Properties iViewConfig, HttpServletRequest request) throws UnsupportedEncodingException {
        Properties dateUp = new Properties();
        if (!iViewConfig.containsKey("MCR.Module-iview.navi.zoom"))
            dateUp.put("XSL.MCR.Module-iview.navi.zoom.SESSION", "fitToScreen");
        if (!iViewConfig.containsKey("MCR.Module-iview.display"))
            dateUp.put("XSL.MCR.Module-iview.display.SESSION", "normal");
        if (!iViewConfig.containsKey("MCR.Module-iview.style"))
            dateUp.put("XSL.MCR.Module-iview.style.SESSION", "image");
        if (!iViewConfig.containsKey("MCR.Module-iview.embedded"))
            dateUp.put("XSL.MCR.Module-iview.embedded.SESSION", "false");
        if (!iViewConfig.containsKey("MCR.Module-iview.lastEmbeddedURL"))
            dateUp.put("XSL.MCR.Module-iview.lastEmbeddedURL.SESSION", MCRServlet.getBaseURL());

        updateIViewConfig(iViewConfig, request, dateUp, "session timed out, reset essential parameters");
    }

    @SuppressWarnings("unchecked")
    public void printRequest(HttpServletRequest request) {
        LOGGER.debug("############################################# ");
        for (Enumeration<String> e = request.getHeaderNames(); e.hasMoreElements();) {
            String name = e.nextElement();
            LOGGER.debug("HEADER: " + name + "=" + request.getHeader(name));
        }
        LOGGER.debug("start print Request-Parameters ############## ");
        for (Enumeration<String> e = request.getParameterNames(); e.hasMoreElements();) {
            String name = e.nextElement();
            LOGGER.debug("" + name + "=" + request.getParameter(name));
        }
        LOGGER.debug("finished printing Request-Parameters ######### ");
        LOGGER.debug("                                               ");

        LOGGER.debug("start print Request-Attributes ################ ");
        for (Enumeration<String> e = request.getAttributeNames(); e.hasMoreElements();) {
            String name = e.nextElement();
            LOGGER.debug("" + name + "=" + request.getAttribute(name));
        }
        LOGGER.debug("finished printing Request-Attributes ########## ");
        LOGGER.debug("############################################### ");
    }

    public void printIViewConfig(Properties iViewConfig) {
        LOGGER.debug("############################################# ");
        LOGGER.debug("start printing IViewCOnfig-Parameters ####### ");

        for (Object key : iViewConfig.keySet()) {
            String name = key.toString();
            String value = iViewConfig.getProperty(name);
            if (name.startsWith("MCR.Module-iview") || name.startsWith("browser"))
                LOGGER.debug(name + "=" + value);
        }
        LOGGER.debug("finished printing IViewCOnfig-Parameters ###### ");
        LOGGER.debug("############################################### ");
    }

}
