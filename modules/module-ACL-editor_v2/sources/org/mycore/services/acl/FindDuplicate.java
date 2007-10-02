package org.mycore.services.acl;

import java.io.File;

import org.jdom.Document;
import org.jdom.Element;
import org.mycore.common.xml.MCRXMLHelper;

public class FindDuplicate {
	
	public static void main(String[] args) {
		String errorMSG = "Usage: " + FindDuplicate.class.getName() + "folder1 folder2 [-b]";
		boolean printBoth = false;
		
		if (args.length < 2 || args.length > 3){
			System.out.println(errorMSG);
			System.exit(1);
		}
		
		if (args.length == 3 && args[2].equals("-b"))
			printBoth = true;
		else{
			System.out.println(errorMSG);
			System.exit(1);
		}
		
		File folder1 = new File(args[0]);
		File folder2 = new File(args[0]);
		
		if (!(folder1.isDirectory() && folder2.isDirectory())){
			System.out.println(errorMSG);
			System.exit(1);
		}
		
		File[] fileList1 = folder1.listFiles();
		File[] fileList2 = folder1.listFiles();
		
		for (File file1 : fileList1){
			for (File file2 : fileList2){
				Document doc1 = new Document(new Element(""));
				Document doc2 = new Document(new Element(""));
				
				if (MCRXMLHelper.deepEqual(doc1, doc2)){
					String id = file1.getName().replaceAll(".xml", "");
					if (printBoth)
						id = id + "\t" + file2.getName().replaceAll(".xml", "");
					
					System.out.println(id);
				}
						
			}
		}
		
		
	}
}
