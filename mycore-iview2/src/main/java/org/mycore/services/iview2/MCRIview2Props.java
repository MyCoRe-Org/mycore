package org.mycore.services.iview2;

import java.util.Properties;

import org.mycore.common.MCRConfiguration;

public class MCRIview2Props {
	private static String prefix = "MCR.Module-iview2.";
	private static Properties Iviewprops = MCRConfiguration.instance().getProperties(prefix);
	
	private MCRIview2Props() {}
	
	public static String getProperty(String propName) {
		return Iviewprops.getProperty(prefix + propName);
	}
}
