<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

      <xsl:variable name="MainTitle">
            <xsl:value-of select="document($navigationBase)/navigation/@mainTitle" />
      </xsl:variable>
      <xsl:param name="navigationBase" 
            select="concat($WebApplicationBaseURL,'modules/module-wcms/uif/web/common/navigation.xml')" />
      <xsl:param name="ImageBaseURL" select="concat($WebApplicationBaseURL,'modules/module-wcms/uif/web/common/images/') " />

      <xsl:include href="wcms_coreFunctions.xsl" />      
      <xsl:include href="wcms_common-used.xsl" />
      <xsl:include href="wcms_chooseTemplate.xsl" />
      
      <!-- =================================================================================================== -->
      <xsl:template name="wcms.generatePage">
            
		<!-- assign right browser address -->
		<xsl:variable name="browserAddress" >
	            <xsl:call-template name="wcms.getBrowserAddress" />
		</xsl:variable>
                  
            <!-- look for appropriate template entry and assign -> $template -->
            <xsl:variable name="template" >
	            <xsl:call-template name="wcms.getTemplate" >
                        <xsl:with-param name="browserAddress" select="$browserAddress"/>
                  </xsl:call-template>
            </xsl:variable>

            <!-- call the appropriate template -->
            <xsl:call-template name="chooseTemplate">
                  <xsl:with-param name="template" select="$template" />
                  <xsl:with-param name="browserAddress" select="$browserAddress" />
            </xsl:call-template>

      </xsl:template>
      <!-- ================================================================================= -->
      
</xsl:stylesheet>