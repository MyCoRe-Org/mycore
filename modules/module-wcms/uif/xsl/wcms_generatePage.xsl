<?xml version="1.0" encoding="ISO-8859-1"?>
<!-- ============================================== -->
<!-- $Revision: 1.11 $ $Date: 2005-02-11 09:01:21 $ -->
<!-- ============================================== -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
   <xsl:output method="html" indent="yes" encoding="UTF-8" media-type="text/html" 
     doctype-public="-//W3C//DTD HTML 4.01//EN"
     doctype-system="http://www.w3.org/TR/html4/strict.dtd" />
      
	<!-- ================== get some wcms required global variables ===================================== -->      
      <!-- location of navigation base -->
      <xsl:variable name="navigationBase" 
            select="concat($WebApplicationBaseURL,'modules/module-wcms/uif/web/common/navigation.xml',$JSessionID)" />
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
      <!-- set useTarget to 'yes' if you want the target attribute to appear in links
        the wcms controls. This would break HTML 4.01 strict compatiblity but allows
        the browser to open new windows when clicking on certain links.
        To keep standard compliance it's default turned of, as it may annoy some
        people, too.
      -->
      <xsl:variable name="wcms.useTargets" select="'yes'" />

      <xsl:include href="wcms_coreFunctions.xsl" />
      <xsl:include href="wcms_common-used.xsl" />
      <xsl:include href="wcms_chooseTemplate.xsl" />
            
      <!-- =================================================================================================== -->
      <xsl:template name="wcms.generatePage">
            <!-- call the appropriate template -->
            <xsl:call-template name="chooseTemplate" />
      </xsl:template>
      <!-- ================================================================================= -->
</xsl:stylesheet>