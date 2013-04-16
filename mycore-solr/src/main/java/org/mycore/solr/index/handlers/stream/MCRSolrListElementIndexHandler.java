package org.mycore.solr.index.handlers.stream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.jdom2.Element;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.solr.MCRSolrServerFactory;
import org.mycore.solr.index.MCRSolrIndexHandler;
import org.mycore.solr.index.cs.MCRSolrContentStream;
import org.mycore.solr.index.cs.MCRSolrListElementStream;

/**
 * This class index a {@link MCRSolrListElementStream}. The stream contains a list of xml elements (mycore objects)
 * which are indexed together. If one element couldn't be created (mycore server side), a fallback mechanism is implemented
 * to index in single threads.
 * 
 * @author Matthias Eichner
 */
public class MCRSolrListElementIndexHandler extends MCRSolrDefaultIndexHandler {

    protected List<MCRSolrIndexHandler> fallBackList;
    private int docs;

    public MCRSolrListElementIndexHandler(MCRSolrListElementStream stream, int docs) {
        this(stream, docs, MCRSolrServerFactory.getSolrServer());
    }

    public MCRSolrListElementIndexHandler(MCRSolrListElementStream stream, int docs, SolrServer solrServer) {
        super(stream, solrServer);
        this.docs=docs;
        this.fallBackList = new ArrayList<>();
    }

    @Override
    public void index() throws IOException, SolrServerException {
        try {
            super.index();
        } catch (RuntimeException exc) {
            // some index stuff failed on mycore side, try to index items in single threads
            List<Element> elementList = ((MCRSolrListElementStream) getStream()).getList();
            for (Element e : elementList) {
                e = e.detach();
                MCRSolrContentStream stream = new MCRSolrContentStream("element", new MCRJDOMContent(e));
                MCRSolrDefaultIndexHandler indexHandler = new MCRSolrDefaultIndexHandler(stream);
                indexHandler.setCommitWithin(getCommitWithin());
                this.fallBackList.add(indexHandler);
            }
        }
    }

    @Override
    public List<MCRSolrIndexHandler> getSubHandlers() {
        return this.fallBackList;
    }

    @Override
    public int getDocuments() {
        return docs;
    }

}
