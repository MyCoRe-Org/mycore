<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<!-- ================================================================================= -->
 <xsl:template name="footer">

  <xsl:choose>
	    <!-- last modified data of appropriate content file -->
		<xsl:when test=" MyCoReWebPage/meta/log[@lastEditor != ''] " > Autor: <xsl:value-of select=" MyCoReWebPage/meta/log/@lastEditor " 
			/>, <xsl:value-of select="substring(MyCoReWebPage/meta/log/@date,9,2)" />.<xsl:value-of 
			select="substring(MyCoReWebPage/meta/log/@date,6,2)" />.<xsl:value-of select="substring(MyCoReWebPage/meta/log/@date,1,4)" 
			/> - <xsl:value-of select="MyCoReWebPage/meta/log/@time" /> </xsl:when>
	    <!-- END OF: last modified data of appropriate content file -->	
		<!-- if not given print out general last modified data -->	
		<xsl:otherwise>
				<xsl:for-each 
					select="document(concat($WebApplicationBaseURL, '/modules/module-wcms/uif/common/lastModified.xml')) /log" > 
					Letzte Änderung <xsl:value-of select="substring(@date,9,2)" />.<xsl:value-of select="substring(@date,6,2)" 
					/>.<xsl:value-of select="substring(@date,1,4)" /> - <xsl:value-of select="@time" /> </xsl:for-each>
		</xsl:otherwise>
		<!-- END OF: if not given print out general last modified data -->		
  </xsl:choose>

 </xsl:template>
</xsl:stylesheet>
