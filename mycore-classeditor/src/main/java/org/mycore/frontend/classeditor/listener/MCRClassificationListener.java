package org.mycore.frontend.classeditor.listener;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.ws.rs.ext.Provider;

import org.mycore.common.MCRJSONManager;
import org.mycore.frontend.classeditor.MCRCategoryIDTypeAdapter;
import org.mycore.frontend.classeditor.MCRCategoryListTypeAdapter;
import org.mycore.frontend.classeditor.MCRCategoryTypeAdapter;
import org.mycore.frontend.classeditor.MCRLabelSetTypeAdapter;

@Provider
public class MCRClassificationListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent arg0) {
        MCRJSONManager mg = MCRJSONManager.instance();
        mg.registerAdapter(new MCRCategoryTypeAdapter());
        mg.registerAdapter(new MCRCategoryIDTypeAdapter());
        mg.registerAdapter(new MCRLabelSetTypeAdapter());
        mg.registerAdapter(new MCRCategoryListTypeAdapter());
    }

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
    }

}
