<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet 
  version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
>

<!-- ================================================================================= -->
<xsl:template name="Navigation_main">
  <xsl:param name="browserAddress" />  	  
  			  
     <table width="100%" style="height:100%" border="0" cellspacing="0" cellpadding="0" class="navi_column">
		
		<!-- main menu -->
  	    <tr valign="top">
		  <td>
			<xsl:call-template name="NavigationTree">
				<xsl:with-param name="rootNode" 
					select="document($navigationBase)/navigation/navi-main" 
					/>
				<xsl:with-param name="browserAddress" select="$browserAddress" />
				<xsl:with-param name="CSSLayoutClass" select="'navi_main'"/>
				<xsl:with-param name="menuPointHeigth" select="'17'" />
				<!-- use pixel values -->
				<xsl:with-param name="columnWidthIcon" select="'9'" />
				<!-- use percent values -->
				<xsl:with-param name="spaceBetweenMainLinks" select="'10'" />
				<!-- use pixel values -->
				<xsl:with-param name="borderWidthTopDown" select="'15'" />
				<!-- use pixel values -->
				<xsl:with-param name="borderWidthSides" select="'7'" />
				<!-- use percent values -->
			</xsl:call-template>
		  </td>
		</tr>
		<!-- END OF: main menu -->
		
	  </table>
  
</xsl:template>
<!-- ================================================================================= -->


</xsl:stylesheet>
