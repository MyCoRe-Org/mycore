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
	private List<String> searchFields = new ArrayList<String>();
	//	 classificationID relevant for the oai list set
	private String[] classificationIDs=new String[]{};

	public MCROAIConfigBean(String instance) {
    	this.oaiInstanceName = instance;
    	String restriction = config.getString("MCR.OAI.QueryRestriction." + instance,"");
    	if(!restriction.equals("")) {
    		this.queryRestriction = restriction;
    	}
    	String s = config.getString("MCR.OAI.Setscheme.Searchfields." + instance,"").replaceAll(" ","");
    	if(s.length()>0){
    		String[] searchFieldsAr = s.split(",");
    		this.searchFields = Arrays.asList(searchFieldsAr);
    		List<String> lstClassificationIDs = new ArrayList<String>();
    		for(int i=0;i<searchFieldsAr.length;i++){
    			String[] classIDs = config.getString("MCR.OAI.Setscheme.Classids." + instance+"."+searchFieldsAr[i]).replaceAll(" ","").split(",");
    			for(int j=0;j<classIDs.length;j++){
    				lstClassificationIDs.add(classIDs[j]);
    			}
    		}   	
    		this.classificationIDs =lstClassificationIDs.toArray(new String[]{});
    	
    		if(this.classificationIDs.length == 0){
    			logger.error("no classification entry found in MCR.OAI.Setscheme.Classids." + instance);
    			this.classificationIDs = new String[2];
    			this.classificationIDs[0] = "DocPortal_class_00000006" ;
    			this.classificationIDs[1] = "DocPortal_class_00000005" ;
    		}
    	}
    	this.repositoryIdentifier = config.getString("MCR.OAI.Repository.Identifier." + instance,"oai.mycore.de");
    	this.repositoryName = config.getString("MCR.OAI.Repository.Name." + instance,"MyCoRe Repository fuer Online Hochschulschriften");
	}


	public String[] getClassificationIDs() {
		return classificationIDs;
	}
	
	public String[] getClassificationIDsForSearchField(String searchField){
		return config.getString("MCR.OAI.Setscheme.Classids." + oaiInstanceName+"."+searchField).replaceAll(" ","").split(",");
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

	public List<String> getSearchFields() {
		return searchFields;
	}

	public void setSearchFields(List<String> searchFields) {
		this.searchFields = searchFields;
	}

}
