<?xml version="1.0" encoding="ISO-8859-1"?>
<!-- ============================================== -->
<!-- $Revision: 1.2 $ $Date: 2004-12-28 23:38:21 $ -->
<!-- ============================================== -->

<!-- Authors: Thomas Scheffler (yagee) -->

<xsl:stylesheet 
  version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xlink="http://www.w3.org/1999/xlink"
  exclude-result-prefixes="xlink">

<!--
Template: UrlSetParam
synopsis: It replaces parameter value or adds a parameter to an url
param:

  url: the url to hold the parameter and value
	par: name of the parameter
	value: new value
-->
<xsl:template name="UrlSetParam">
  <xsl:param name="url"/>
  <xsl:param name="par"/>
  <xsl:param name="value"/>
  <!-- There are two possibility for a parameter to appear in an url:
       1.) after a ? sign
       2.) after a & sign
       In both cases the value is either limited by a & sign or the string end
  //-->
	<xsl:variable name="asFirstParam">
	  <xsl:value-of select="concat('?',$par,'=')"/>
	</xsl:variable>
	<xsl:variable name="asOtherParam">
	  <xsl:value-of select="concat('&amp;',$par,'=')"/>
	</xsl:variable>
  <xsl:choose>
	  <xsl:when test="contains($url,$asFirstParam) or contains($url,$asOtherParam)">
		  <!-- Parameter is present -->
			<xsl:variable name="asParam">
			  <xsl:choose>
          <xsl:when test="contains($url,$asFirstParam)">
            <!-- Parameter is right after a question mark //-->
						<xsl:value-of select="$asFirstParam"/>
		      </xsl:when>
          <xsl:when test="contains($url,asOtherParam)">
            <!-- Parameter is right after a & sign //-->
						<xsl:value-of select="$asOtherParam"/>
					</xsl:when>
				</xsl:choose>
			</xsl:variable>
      <xsl:variable name="newurl">
        <xsl:value-of select="substring-before($url,$asParam)"/>
        <xsl:value-of select="$asParam"/>
        <xsl:value-of select="$value"/>
        <xsl:if test="contains(substring-after($url,$asParam),'&amp;')">
          <!--OK now we know that there are parameter left //-->
          <xsl:value-of select="concat('&amp;',substring-after(substring-after($url,$asParam),'&amp;'))"/>
        </xsl:if>
      </xsl:variable>
      <xsl:value-of select="$newurl"/>
    </xsl:when>
    <xsl:otherwise>
      <!-- The parameter is not yet specified //-->
			<xsl:choose>
			  <xsl:when test="contains($url,'?')">
				  <!-- Other parameters a present //-->
					<xsl:value-of select="concat($url,'&amp;',$par,'=',$value)"/>
				</xsl:when>
				<xsl:otherwise>
				  <!-- No other parameter presen //-->
					<xsl:value-of select="concat($url,'?',$par,'=',$value)"/>
				</xsl:otherwise>
			</xsl:choose>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!--
Template: UrlGetParam
synopsis: Gets the value of a given parameter from a specific url
param:

  url: the url to hold the parameter and value
	par: name of the parameter
-->
<xsl:template name="UrlGetParam">
  <xsl:param name="url"/>
  <xsl:param name="par"/>
  <!-- There are two possibility for a parameter to appear in an url:
       1.) after a ? sign
       2.) after a & sign
       In both cases the value is either limited by a & sign or the string end
  //-->
  <xsl:variable name="afterParam">
    <xsl:choose>
      <xsl:when test="contains($url,concat('?',$par,'='))">
        <!-- Parameter is right after a question mark //-->
	      <xsl:value-of select="substring-after($url,concat('?',$par,'='))"/>
      </xsl:when>
      <xsl:when test="contains($url,concat('&amp;',$par,'='))">
        <!-- Parameter is right after a & sign //-->
	      <xsl:value-of select="substring-after($url,concat('&amp;',$par,'='))"/>
      </xsl:when>
      <xsl:otherwise>
        <!-- The parameter is not specified //-->
      </xsl:otherwise>
    </xsl:choose>
	</xsl:variable>
  <xsl:choose>
    <xsl:when test="contains($afterParam,'&amp;')">
	    <!-- cut off other parameters -->
	    <xsl:value-of select="substring-before($afterParam,'&amp;')"/>
	  </xsl:when>
	  <xsl:otherwise>
	    <xsl:value-of select="$afterParam"/>
	  </xsl:otherwise>
  </xsl:choose>
</xsl:template>

</xsl:stylesheet>