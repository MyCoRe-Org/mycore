package source;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class transform {
	
	public static void main(String[] args) throws JDOMException, IOException {
		System.out.println("Start Migration!");
		String inDir = new String();
		String outDir = new String();
		String[] lang = null;
		ToolBox tools = new ToolBox();
		
		if (args.length != 4){
			System.out.println("Usage: Migrate sourceDir outputDir -lang de,en,ru,pl");
			return;
		}
		else{
			inDir = args[0];
			outDir = args[1].endsWith(File.separator)? args[1] : args[1] + File.separator;
			lang = args[3].split(",");
		}
		
		File inputDir = new File(inDir);;
		if (!inputDir.exists()) { 
			System.out.println("Directory " + inDir + " does not exist!");
			return;
		}
		else if (!inputDir.isDirectory()){
			System.out.println(inDir + " is not a directory!");
			return;
		}
		
		FilenameFilter filter = new FilenameFilter(){
			public boolean accept(File arg0, String arg1) {
				return arg1.endsWith(".xsl");
			}
		};
		
		HashMap docMap = tools.createDocMap(inputDir.listFiles(filter)); // a hashmap with all the document in sourceDir
		Set fileSet = docMap.keySet();
		Object[] fileArray = fileSet.toArray();
		// replacing variable
		for (int i = 0; i < fileArray.length; i++){
			Object currentFile = fileArray[i];
			
			if (currentFile instanceof String){
				if (((String)currentFile).endsWith("-" + lang[0] + ".xsl")){
					String prefix = ((String)currentFile).replaceAll("-de.xsl", "."); // identify the variable with the lang xsl
					Document currentDoc = (Document)docMap.get(currentFile);
					List incList = tools.getIncList(currentDoc); // list of includes in the current lang xsl
					List varList = tools.getVarList(currentDoc); // list of variables in the current lang xsl
					
					HashMap varMap = new HashMap();
					varMap.put((String)currentFile, varList);
					varMap.putAll(tools.getIncVarMap(incList, docMap));
					
					for (int j = 0; j < incList.size(); j++){
						Set keySet = varMap.keySet();
						Object[] o = keySet.toArray();
						for (int l = 0; l < o.length; l++){
							String currentPrefix = ((String)o[l]).replaceAll("-de.xsl", ".");
							List currentVarList = (List)varMap.get(o[l]);
						
							String currentInc = ((Element)incList.get(j)).getAttributeValue("href");
							if (docMap.containsKey(currentInc)){
								tools.replace(currentInc, currentVarList, docMap, currentPrefix);
								System.out.println("Replacing variables in " + currentInc);
							}
						}
					}
				}
			}
		}
		
		HashMap langFiles = new HashMap();
		
		// writing the files after replacing the variables
		for (int i = 0; i < fileArray.length; i++){
			Object currentFile = fileArray[i];
			
			
			if (currentFile instanceof String) {
				String currentFileName = (String) currentFile;
				String[] folder = { "test", "final" };

				if (tools.isLangFile(currentFileName, lang)) {
					langFiles.put(currentFileName, docMap.remove(currentFile)); // move all language files into a separate hash map
					System.out.println("Remove language file " + (String)currentFileName);
				} else {
					for (int j = 0; j < folder.length; j++) {
						File file = new File(outDir + folder[j]);
						if (!file.exists())
							file.mkdirs();

						FileOutputStream fileOut = new FileOutputStream(outDir + folder[j] + File.separator + currentFileName);

						tools.writeXML((Document) docMap.get(currentFileName), fileOut);
						System.out.println("Write file " + outDir + folder[j] + File.separator + currentFileName);
					}
				}
			}
		}
		
		fileSet = langFiles.keySet();
		fileArray = fileSet.toArray();
		HashMap propMap = new HashMap();
		
		// creating message files
		for (int i = 0; i < fileArray.length; i++){
			Object currentFile = fileArray[i];
			
			if (currentFile instanceof String) {
				String currentFileName = (String) currentFile;
				String currentLang = tools.whichLang(currentFileName, lang);
				String message = "messages_" + currentLang + ".properties";
				Properties prop = propMap.containsKey(message) ? (Properties) propMap.remove(message) : new Properties();
				Document currentDoc = (Document) langFiles.get(currentFile);
				String prefix = currentFileName.replaceAll("-" + currentLang + ".xsl", "");
				
				tools.genProp(currentDoc, prefix, prop);
				propMap.put(message, prop);
				System.out.println("For language file " + currentFileName + " creating message file " + message);
			}
		}
		
		// writing message files
		fileSet = propMap.keySet();
		fileArray = fileSet.toArray();
		for (int i = 0; i < fileArray.length; i++){
			Object currentFile = fileArray[i];
			
			if (currentFile instanceof String) {
				String currentFileName = (String) currentFile;
				Properties prop = (Properties) propMap.get(currentFile);
				File file = new File(outDir + "messages");
				if (!file.exists())
					file.mkdirs();

				FileOutputStream propOut = new FileOutputStream(outDir + "messages" + File.separator + currentFileName);
				
				prop.store(propOut, currentFileName);
				
				System.out.println("Writing file " + outDir + "messages" + File.separator + currentFileName);
				
			}
		}
		
		// writing language files
		fileSet = langFiles.keySet();
		fileArray = fileSet.toArray();
		for (int i = 0; i < fileArray.length; i++){
			Object currentFile = fileArray[i];
			
			
			if (currentFile instanceof String) {
				String currentFileName = (String) currentFile;
				String[] folder = { "test", "final" };

				
				for (int j = 0; j < folder.length; j++) {
					File file = new File(outDir + folder[j]);
					if (!file.exists())
						file.mkdirs();

					FileOutputStream fileOut = new FileOutputStream(outDir + folder[j] + File.separator + currentFileName);
					
					Document currentDoc = (Document)((Document) langFiles.get(currentFileName)).clone(); // clone Dolly :-)
					if (j == 0){
						Namespace xsl = Namespace.getNamespace("xsl", "http://www.w3.org/1999/XSL/Transform");
						Element root = currentDoc.getRootElement();
						root.removeChildren("variable", xsl);
						currentDoc.setRootElement(root);
					}

					tools.writeXML(currentDoc, fileOut);
					System.out.println("Write file " + outDir + folder[j] + File.separator + currentFileName);
				}
			}
		}
		
		System.out.println("Finish!");
	}
}

class ToolBox{
	public boolean isLangFile(String fileName, String[] lang){
		for (int i = 0; i < lang.length; i++){
			if (fileName.endsWith("-" + lang[i] + ".xsl"))
				return true;
		}
		
		return false;
	}
	
	public String whichLang(String fileName, String[] lang){
		for (int i = 0; i < lang.length; i++){
			if (fileName.endsWith("-" + lang[i] + ".xsl"))
				return lang[i];
		}
		
		return null;
	}
	
	public HashMap createDocMap(File[] fileList) throws JDOMException, IOException{
		HashMap docMap = new HashMap();
		
		for (int i = 0; i < fileList.length; i++){
			docMap.put(fileList[i].getName(), createDoc(fileList[i]));
		}
		
		return docMap;
	}
	
	//	 create a Document
	public Document createDoc(Object obj) throws JDOMException, IOException {
		SAXBuilder saxBuilder = new SAXBuilder();
		if (obj instanceof File)
			return saxBuilder.build((File) obj);
		else if (obj instanceof Reader)
			return saxBuilder.build((Reader) obj);
		else
			return null;
	}
	
	// write the Document to an output stream
	public void writeXML(Document doc, Object output) throws IOException {
		Format format = Format.getPrettyFormat();
		format.setEncoding("ISO-8859-1");
		XMLOutputter outputter = new XMLOutputter(format);
		if (output instanceof OutputStream)
			outputter.output(doc, (OutputStream)output);
		if (output instanceof Writer)
			outputter.output(doc, (Writer)output);
	}
	
	// replaces all the variables listed in varList with a i18n method in the file denoted by fileName 
	// prefix is the name of the lang xsl without eg. "-de.xsl"
	public void replace(String fileName, List varList, HashMap docMap, String prefix) throws JDOMException, IOException{
		Document doc = (Document)docMap.remove(fileName);
		String stringDoc = docToString(doc);

		Object o = null;
		Element currentElem = null;
		for (Iterator i = varList.iterator(); i.hasNext();) {
			o = i.next();

			if (o instanceof Element) {
				currentElem = (Element) o;
				String varName = currentElem.getAttributeValue("name");
				if (stringDoc.indexOf("$" +varName) > 1){
					stringDoc = replaceWrapper(stringDoc, "\\$" + varName, "i18n:translate('" + prefix + varName + "')");
				
					if (stringDoc.indexOf("xmlns:i18n") <= -1){ // I make string replacement, because I've no better working version
						stringDoc = stringDoc.replaceAll("\\<xsl\\:stylesheet", "<xsl:stylesheet xmlns:i18n=\"xalan://org.mycore.services.i18n.MCRTranslation\" ");
						stringDoc = stringDoc.replaceAll("exclude\\-result\\-prefixes=\"", "exclude-result-prefixes=\"i18n ");
					}
				
					if (stringDoc.indexOf("xmlns:xalan") <= -1){
						stringDoc = stringDoc.replaceAll("\\<xsl\\:stylesheet", "<xsl:stylesheet xmlns:xalan=\"http://xml.apache.org/xalan\"");
						stringDoc = stringDoc.replaceAll("exclude\\-result\\-prefixes=\"", "exclude-result-prefixes=\"xalan ");
					}
				}
				
			}
		}
		
		doc = createDoc(new StringReader(stringDoc));
		
		
		docMap.put(fileName, doc);
		
		List incList = getIncList(doc);

		for (Iterator i = incList.iterator(); i.hasNext();) {
			o = i.next();

			if (o instanceof Element) {
				currentElem = (Element) o;
				String currentInc = currentElem.getAttributeValue("href");
				if (docMap.containsKey(currentInc))
					replace(currentInc, varList, docMap, prefix);
			}
		}
	}
	
	// smart replacing a substring in a string
	// it don't remoeve the ending character
	// eg. replace "$changeme" to "replacement" (without the quote)
	//	   so the function replace only words ending with: (){}",tab
	//	   without replacing the ending character
	public String replaceWrapper(String stringDoc, String regexp, String replacement) {
		char[] end = ")}'\",\t ".toCharArray();
		for (int i = 0; i < end.length; i++) {
			stringDoc = stringDoc.replaceAll(regexp + "\\" + end[i], replacement + end[i]);
		}
		return stringDoc;
	}

	// make a string representation from a Document
	public String docToString(Document doc) {
		XMLOutputter outputter = new XMLOutputter();
		return outputter.outputString(doc);
	}

	// get an include list from a Document
	public List getIncList(Document doc) {
		Namespace xsl = Namespace.getNamespace("xsl", "http://www.w3.org/1999/XSL/Transform");
		Element elem = doc.getRootElement();

		return elem.getChildren("include", xsl);
	}
	
	// get an include list from a Document
	public List getVarList(Document doc) {
		Namespace xsl = Namespace.getNamespace("xsl", "http://www.w3.org/1999/XSL/Transform");
		Element elem = doc.getRootElement();

		return elem.getChildren("variable", xsl);
	}
	
	// get an include list from a Document
	public HashMap getIncVarMap(List incList, HashMap docMap) {
		Namespace xsl = Namespace.getNamespace("xsl", "http://www.w3.org/1999/XSL/Transform");
		HashMap incVarMap = new HashMap();
		
		for (int i = 0; i < incList.size(); i++){
			String incFile = ((Element) incList.get(i)).getAttributeValue("href");
			if (incFile.endsWith("-de.xsl")){
				Document doc = (Document)docMap.get(incFile);
				Element elem = doc.getRootElement();
				incVarMap.put(incFile, elem.getChildren("variable", xsl));
			}
		}
		

		return incVarMap;
	}
	
	public void genProp(Document xmlDoc, String prefix, Properties prop) throws JDOMException, IOException{
		Namespace xsl = Namespace.getNamespace("xsl", "http://www.w3.org/1999/XSL/Transform");
		Element rootNode = xmlDoc.getRootElement();
		List varList = rootNode.getChildren("variable", xsl);
		HashMap attribMap = new HashMap();
		
		Object o = null;
		Element currentElem = null;
		for (Iterator i = varList.iterator(); i.hasNext();){
			o = i.next();
			
			if (o instanceof Element){
				currentElem = (Element) o;
				String attribVal = currentElem.getAttributeValue("select");
				if (attribVal == null){
					attribVal = currentElem.getChild("value-of", xsl).getAttributeValue("select");
				}
				
				attribMap.put(currentElem.getAttributeValue("name"), attribVal.replaceAll("'",""));
			}
		}
		
		Set keySet = attribMap.keySet();
		Object[] attrib = keySet.toArray();
		for (int i = 0; i < attrib.length; i++){
			o = attrib[i];
			
			if (o instanceof String){
				String currentAttrib = (String) o;
				String attribVal = (String) attribMap.get(currentAttrib);
				if (attribVal.startsWith("$")){
					attribVal = (String)attribMap.get(attribVal.replaceAll("\\$",""));
				}
				
				prop.setProperty(prefix + "." + currentAttrib, attribVal);
			}
		}
	}
}