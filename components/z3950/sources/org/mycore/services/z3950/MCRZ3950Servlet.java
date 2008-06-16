package org.mycore.services.z3950;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * Dieses Servlet implementiert eine MyCoRe-Schnittstelle für einen
 * Z39.50-Server. Die Suche wird dabei an eine Serviceklasse
 * delegiert.
 * @author Andreas de Azevedo
 * @version 1.0
 *
 */
public class MCRZ3950Servlet extends MCRServlet {

    private static final long serialVersionUID = 10137452L;

    private static MCRConfiguration config = MCRConfiguration.instance();

    private static Logger logger = Logger.getLogger(MCRZ3950Servlet.class);

    // Z3950-Anfrage (find), soll im Format "Prefix" sein
    private String query;

    // Dokumententyp
    private String type;

    // Host, auf dem dies Suche angewandt wird
    private String host;
    
    // Index des Suchergebnisses, beginnend bei 1
    private int index;

    // Größe des Resultats (nicht implementiert)
    private int size;
    
    // Ist true, wenn das Ergebnis gekürzt werden soll
    private boolean cut;
    
    // Property, dass die Serviceklasse enthält
    private static final String MCR_QUERYSERVICE = "MCR.z3950.queryservice";
    
    /**
     * Method getServletInfo. Returns a short description of the servlet.
     * 
     * @return String
     */
    public String getServletInfo() {
        return "This class implements an Z3950-Interface for MyCoRe";
    }

    /**
     * This method handles HTTP GET/POST requests and resolves them to output.
     * 
     * @param job
     *            MCRServletJob containing request and response objects
     * @exception IOException
     *                for java I/O errors.
     * @exception ServletException
     *                for errors from the servlet engine.
     */
    public void doGetPost(MCRServletJob job) throws IOException, ServletException {
	   HttpServletRequest request = job.getRequest();
       HttpServletResponse response = job.getResponse();

	   // Ausgabe in UTF-8
	   response.setContentType("text/xml; charset=UTF-8");
	   
	   // Parameter überprüfen
	   if (!checkInputParameter(request)) {
		   logger.error("Es wurden nicht genügend Parameter angegeben!");
           // Keine Ergebnisse, es wird "0" zurückgegeben
		   ServletOutputStream out = response.getOutputStream();
		   out.write("0".getBytes("UTF-8"));
		   out.close();		  
		   return;
	   }
	   
	   MCRZ3950Query service = null;

       try {
           // check if property is set, else Exception is thrown
           service = (MCRZ3950Query) config.getInstanceOf(MCR_QUERYSERVICE);
       } catch (MCRConfigurationException mcrx) {
           logger.fatal("Missing configuration item: " + MCR_QUERYSERVICE + ".", mcrx);
           return;
       }
       
       service.setQuery(query);
	
	   if (service.search()) {
		   /*
		    * Wenn es Ergebnisse gab, dann sind zwei Fälle zu unterscheiden:
		    * 1. Der Z39.50-Server hat eine search-Anfrage getätigt, in diesem
		    *    Fall ist nur die Anzahl der Ergebnisse relevant und wird in
		    *    den Response-Output-Stream geschrieben.
		    * 2. Der Z39.50-Server hat eine present-Anfrage getätigt, d.h. die
		    *    Ergebnisse der Suchanfrage sollen nun angezeigt werden. Dann
		    *    wird entsprechend ein Ergebnis-Dokument als Antwort zurück-
		    *    gegeben. Zur Zeit ist wird noch jedes Ergebnis einzeln
		    *    abgefragt, daher wird das Ergebnis-Dokument immer auf ein
		    *    einzelnes Ergebnis gekürzt. Außerdem werden noch alle Tags,
		    *    die vom Typ "MCRMetaClassification" sind mit ihrem Label
		    *    gefüllt, da die IDs an sich wertlos sind.
		    */
		   if (cut) {
			   service.cutDownTo(index);
			   service.setIndex(index - 1);
		   }
		   try {
			   ServletOutputStream out = response.getOutputStream();
			   if (cut) {
				   byte[] mycoreResult = service.getDocumentAsByteArray();
				   out.write(mycoreResult);
			   } else {
				   out.write(String.valueOf(service.getSize()).getBytes("UTF-8"));
			   }
			   out.close();
		   } catch (IOException ioe) {
			   ioe.printStackTrace();
		   }
	   } else {
		   // Keine Ergebnisse
		   ServletOutputStream out = response.getOutputStream();
		   out.write("0".getBytes("UTF-8"));
		   out.close();
	   }
   }
	    
   private final boolean checkInputParameter(HttpServletRequest request) {
	   query = request.getParameter("query");
	   type = request.getParameter("type");
	   String cutStr = request.getParameter("cut");
	   cut = false;
	   if (cutStr != null) {
		   if (cutStr.equals("true")) cut = true;
	   } 
	   
	   String sizeStr = request.getParameter("size");
	   size = 0;
	   
	   if (sizeStr != null) {
		   size = Integer.parseInt(sizeStr);
	   }
	   
	   String indexStr = request.getParameter("index");
	   index = 0;
	   
	   if (indexStr != null) {
		   index = Integer.parseInt(indexStr);
	   }
	   
	   if (host == null) {
		   host = "local";
	   }
	   
	   if (host.equals("")) {
		   host = "local";
	   }
	   
	   if (query == null) {
		   query = "";
	   }
	   
	   if (type == null) {
		   logger.debug("Parameter type is NULL!");
		   return false;
	   }
	   
	   if (type.equals("")) {
		   logger.debug("Parameter type is EMPTY!");
		   return false;
	   }
	   
	   type = type.toLowerCase();
	   
	   logger.info("MCRZ3950Servlet: RequestEncoding = " + request.getCharacterEncoding());
	   logger.info("MCRZ3950Servlet: ContentType = " + request.getContentType());
	   logger.info("MCRZ3950Servlet: type = " + type);
	   logger.info("MCRZ3950Servlet: hosts = " + host);
	   logger.info("MCRZ3950Servlet: query = \"" + query + "\"");
	   logger.info("MCRZ3950Servlet: cut = \"" + cut + "\"");
	   logger.info("MCRZ3950Servlet: index = \"" + index + "\"");
	   logger.info("MCRZ3950Servlet: size = \"" + size + "\"");
	   
	   return true;
   }

}
