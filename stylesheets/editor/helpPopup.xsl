<?xml version="1.0" encoding="ISO-8859-1"?>

<!-- ============================================== -->
<!-- $Revision: 1.3 $ $Date: 2005-06-16 10:03:45 $ -->
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
           body, html {</xsl:text><xsl:value-of select="$editor.font"/><xsl:text>}
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
            <xsl:choose>
              <xsl:when test="helpPopup/close">
                <input type="button" class="actionButton" value="Fenster schliessen" onClick="window.close();" />
              </xsl:when>
              <xsl:otherwise>
                <input type="button" style="{$editor.font} {$editor.button.style}" value="Fenster schliessen" onClick="window.close();" />
              </xsl:otherwise>
            </xsl:choose>
          </td>
        </tr>

      </table>
    </body>
  </html>
</xsl:template>

<!-- ========================================================================= -->

</xsl:stylesheet>

