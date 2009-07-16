<?xml version="1.0" encoding="ISO-8859-1"?>

<!-- ============================================== -->
<!-- $Revision: 1.7 $ $Date: 2007-08-16 09:31:35 $ -->
<!-- ============================================== --> 

<xsl:stylesheet 
  version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
  exclude-result-prefixes="i18n">

<xsl:output 
  method="html" 
  encoding="UTF-8" 
  media-type="text/html" 
  doctype-public="-//W3C//DTD HTML 3.2 Final//EN"
/>

<xsl:include href="editor-common.xsl" />

<!-- ========================================================================= -->

<!-- ======== handles editor help ======== -->

<xsl:template match="/">
  <html>
    <head>
      <title>
        <xsl:choose>
          <xsl:when test="helpPopup/title">
            <xsl:for-each select="helpPopup">
              <xsl:call-template name="output.title" />
            </xsl:for-each>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="i18n:translate('buttons.helpPopup.help')" />
          </xsl:otherwise>
        </xsl:choose>
      </title>
      <xsl:choose>
        <xsl:when test="helpPopup/@css">
          <link type="text/css" rel="stylesheet">
            <xsl:attribute name="href">/<xsl:value-of select="helpPopup/@css" /></xsl:attribute>
          </link>
        </xsl:when>
      </xsl:choose>
    </head>
    <xsl:choose>
      <xsl:when test="helpPopup/@css">
      </xsl:when>
      <xsl:otherwise>
        <style type="text/css"><xsl:text>
           body, html {font-size:12px; font-family:Verdana,Arial,Helvetica,SansSerif; line-height:16px }
          </xsl:text>
        </style>
      </xsl:otherwise>
    </xsl:choose>
    <body>
      <table border="0" cellpadding="5" cellspacing="0" width="100%" height="100%">
        <xsl:choose>
          <xsl:when test="$CurrentLang = 'ar'">
            <tr>
              <td align="right" >
                <xsl:for-each select="helpPopup">
                  <xsl:call-template name="output.label" />
                </xsl:for-each>
              </td>
            </tr>
            <tr>
              <td align="left">
                <input type="button" class="editorButton" onClick="window.close();" >
	    		  <xsl:attribute name="value">
                    <xsl:value-of select="i18n:translate('buttons.helpPopup.close')" />
	    		  </xsl:attribute>
	    		</input>
              </td>
            </tr>
          </xsl:when>
          <xsl:otherwise>
            <tr>
              <td align="left" >
                <xsl:for-each select="helpPopup">
                  <xsl:call-template name="output.label" />
                </xsl:for-each>
              </td>
            </tr>
            <tr>
              <td align="right">
                <input type="button" class="editorButton" onClick="window.close();" >
	    		  <xsl:attribute name="value">
                    <xsl:value-of select="i18n:translate('buttons.helpPopup.close')" />
	    		  </xsl:attribute>
	    		</input>
              </td>
            </tr>			  
		  </xsl:otherwise>
        </xsl:choose>
      </table>
    </body>
  </html>
</xsl:template>

<!-- ========================================================================= -->

</xsl:stylesheet>

