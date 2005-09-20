<?xml version="1.0" encoding="ISO-8859-1"?>

<!-- ============================================== -->
<!-- $Revision: 1.4 $ $Date: 2005-09-20 08:38:28 $ -->
<!-- ============================================== --> 

<xsl:stylesheet 
  version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
>

<xsl:output 
  method="html" 
  encoding="ISO-8859-1" 
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
Hilfe zum Formular
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
        
        <tr>
          <td align="left" >
            <xsl:for-each select="helpPopup">
              <xsl:call-template name="output.label" />
            </xsl:for-each>
          </td>
        </tr>

        <tr>
          <td align="right">
            <input type="button" class="editorButton" value="Fenster schliessen" onClick="window.close();" />
          </td>
        </tr>

      </table>
    </body>
  </html>
</xsl:template>

<!-- ========================================================================= -->

</xsl:stylesheet>

