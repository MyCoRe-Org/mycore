<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<!--
      <xsl:output method="html" encoding="UTF-8" media-type="text/html" doctype-public="-//W3C//DTD HTML 3.2 Final//EN" />
-->      

      <xsl:output method="xml" indent="yes" encoding="UTF-8" media-type="text/html" 
      doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN" 
      doctype-system="http://www.w3.org/TR/2000/REC-xhtml1-20000126/DTD/xhtml1-strict.dtd" />
      
	<!-- ================== get some wcms required global variables ===================================== -->      
      <!-- location of navigation base -->
      <xsl:variable name="navigationBase" 
            select="concat($WebApplicationBaseURL,'modules/module-wcms/uif/web/common/navigation.xml')" />
      <!-- base image path -->
      <xsl:variable name="ImageBaseURL" select="concat($WebApplicationBaseURL,'modules/module-wcms/uif/web/common/images/') " />
	<!-- main title configured in mycore.properties.wcms -->
      <xsl:param name="MCR.WCMS.nameOfProject"/>
      <xsl:variable name="MainTitle">
            <xsl:value-of select="$MCR.WCMS.nameOfProject"/>
      </xsl:variable>
      <!-- assign right browser address -->
      <xsl:param name="browserAddress" >
            <xsl:call-template name="wcms.getBrowserAddress" />
      </xsl:param>
      <!-- look for appropriate template entry and assign -> $template -->
      <xsl:param name="template" >
            <xsl:call-template name="wcms.getTemplate" >
                  <xsl:with-param name="browserAddress" select="$browserAddress"/>
                  <xsl:with-param name="navigationBase" select="$navigationBase"/>                  
            </xsl:call-template>
      </xsl:param>      
  
      
	<!-- ================== get some wcms required global variables ===================================== -->      

      <xsl:include href="wcms_coreFunctions.xsl" />
      <xsl:include href="wcms_common-used.xsl" />
      <xsl:include href="wcms_chooseTemplate.xsl" />
            
      <!-- =================================================================================================== -->
      <xsl:template name="wcms.generatePage">
<!--
template:<xsl:value-of select="$template" /><br/>
ba:<xsl:value-of select="$browserAddress" /><br/>
     -->       
            <!-- call the appropriate template -->
            <xsl:call-template name="chooseTemplate" />
      </xsl:template>
      <!-- ================================================================================= -->
</xsl:stylesheet>