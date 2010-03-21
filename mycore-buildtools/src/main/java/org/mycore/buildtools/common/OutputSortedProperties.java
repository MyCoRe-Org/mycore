package org.mycore.buildtools.common;

import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TreeSet;

public class OutputSortedProperties {
	@SuppressWarnings("unchecked")
	public static void output(Properties props, Writer writer, String comment){
		try{
			if(comment!=null){
				writer.write("#"+comment+"\n");
			}
			SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
			writer.write("#"+sdf.format(new Date())+"\n");
			TreeSet sortedKeys = new TreeSet(props.keySet());
			for(Object o: sortedKeys){
				writer.write(o.toString()+"="+props.getProperty(o.toString())+"\n");
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
}
