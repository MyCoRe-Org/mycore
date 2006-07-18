package org.mycore.services.oai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;

public class MCROAIConfigBean {
	
	private static MCRConfiguration config = MCRConfiguration.instance();
	private static Logger logger = Logger.getLogger(MCROAIConfigBean.class.getName());
	
	private String oaiInstanceName;
	private String repositoryName;
	private String repositoryIdentifier;
	//	 you can define for each instance of your application a special restriction
	private String queryRestriction;
	//	 searchfields from searchfields.xml relevant for oai list set
	private List searchFields;
	//	 classificationID relevant for the oai list set
	private String[] classificationIDs;

	public MCROAIConfigBean(String instance) {
    	this.oaiInstanceName = instance;
    	String restriction = config.getString("MCR.oai.queryRestriction." + instance,"");
    	if(!restriction.equals("")) {
    		this.queryRestriction = restriction;
    	}
    	String[] searchFieldsAr = config.getString("MCR.oai.setscheme.searchfields." + instance,"format,type").replaceAll(" ","").split(",");
    	this.searchFields = Arrays.asList(searchFieldsAr);
    	List lstClassificationIDs = new ArrayList();
    	for(int i=0;i<searchFieldsAr.length;i++){
    		String[] classIDs = config.getString("MCR.oai.setscheme.classids." + instance+"."+searchFieldsAr[i]).replaceAll(" ","").split(",");
    		for(int j=0;j<classIDs.length;j++){
    			lstClassificationIDs.add(classIDs[j]);
    		}
    	}   	
    	this.classificationIDs =(String[]) lstClassificationIDs.toArray(new String[]{});
    	
    	if(this.classificationIDs.length == 0){
    		logger.error("no classification entry found in MCR.oai.setscheme.classids." + instance);
    		this.classificationIDs = new String[2];
    		this.classificationIDs[0] = "DocPortal_class_00000006" ;
    		this.classificationIDs[0] = "DocPortal_class_00000005" ;
    	}
    	this.repositoryIdentifier = config.getString("MCR.oai.repositoryidentifier." + instance,"oai.mycore.de");
    	this.repositoryName = config.getString("MCR.oai.repositoryname." + instance,"MyCoRe Repository fuer Online Hochschulschriften");
	}


	public String[] getClassificationIDs() {
		return classificationIDs;
	}
	
	public String[] getClassificationIDsForSearchField(String searchField){
		return config.getString("MCR.oai.setscheme.classids." + oaiInstanceName+"."+searchField).replaceAll(" ","").split(",");
	}


	public void setClassificationIDs(String[] classificationIDs) {
		this.classificationIDs = classificationIDs;
	}


	public String getOaiInstanceName() {
		return oaiInstanceName;
	}


	public void setOaiInstanceName(String oaiInstanceName) {
		this.oaiInstanceName = oaiInstanceName;
	}


	public String getRepositoryIdentifier() {
		return repositoryIdentifier;
	}


	public void setRepositoryIdentifier(String repositoryIdentifier) {
		this.repositoryIdentifier = repositoryIdentifier;
	}


	public String getRepositoryName() {
		return repositoryName;
	}


	public void setRepositoryName(String repositoryName) {
		this.repositoryName = repositoryName;
	}


	public String getQueryRestriction() {
		return queryRestriction;
	}

	public void setQueryRestriction(String queryRestriction) {
		this.queryRestriction = queryRestriction;
	}

	public List getSearchFields() {
		return searchFields;
	}

	public void setSearchFields(List searchFields) {
		this.searchFields = searchFields;
	}

}
