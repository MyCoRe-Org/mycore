<?xml version="1.0" encoding="ISO-8859-1"?>	
<!-- ============================================== -->
<!-- $Revision: 1.2 $ $Date: 2004-11-30 15:37:50 $ -->
<!-- ============================================== -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink" 
	exclude-result-prefixes="xlink">
	<xsl:output method="html" encoding="ISO-8859-1" media-type="text/html" doctype-public="-//W3C//DTD HTML 3.2 Final//EN" />

	<xsl:include href="coreFunctions.xsl"/>
	<xsl:include href="wcms_generatePage.xsl" />
					
	<xsl:param name="DocumentBaseURL" />
	<xsl:param name="ServletsBaseURL" />
	<xsl:param name="RequestURL" />
	<xsl:param name="CurrentUser" />
	<xsl:param name="CurrentGroups" />
	<xsl:param name="MCRSessionID" />
	<xsl:param name="WebApplicationBaseURL" />
	<xsl:param name="DefaultLang" />
	<xsl:param name="CurrentLang" />
	<xsl:param name="Lang" />		

	<!-- wcms -->	
	<xsl:variable name="Empty.Derivate"  select="'Leeres Derivate'" />	
	<xsl:param name="navigationBase" select="concat($WebApplicationBaseURL,'modules/module-wcms/uif/common/navigation.xml')" />	
	<xsl:param name="ImageBaseURL" select="concat($WebApplicationBaseURL,'modules/module-wcms/uif/common/images/') " />	
	<xsl:variable name="privcall" 
		select="concat($WebApplicationBaseURL,'servlets/MCRUserPrivServlet?MCRSessionID=',$MCRSessionID)"/>
	<xsl:variable name="PrivOfUser">
		<xsl:copy-of select="document($privcall)//mycoreuserpriv/user" />
	</xsl:variable>
	<xsl:variable name="MainTitle">
		<xsl:value-of 
			select="document($navigationBase)/navigation/@mainTitle" />
	</xsl:variable>
	<!-- end wcms -->
	
	<!-- ============================================================================================== -->	
	<xsl:template match="/">

		<!-- wcms -->
		<xsl:call-template name="wcms.generatePage" />
		<!-- end: wcms -->		
		
	</xsl:template>
	
</xsl:stylesheet>
