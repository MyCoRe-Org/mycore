<?xml version="1.0" encoding="ISO-8859-1"?>

<!-- ============================================== -->
<!-- $Revision: 1.3 $ $Date: 2004-09-29 13:46:56 $ -->
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

<!-- ======== http request parameter ======== -->

<xsl:param name="help.id" /> <!-- ID of help text to show -->

<!-- ========================================================================= -->

<!-- ======== handles editor help ======== -->

<xsl:template match="/editor/components">
  <html>
    <head>
      <title>Hilfe zum Formular</title>
    </head>
    <style type="text/css"><xsl:text>
      . {</xsl:text><xsl:value-of select="$editor.font"/><xsl:text>}
    </xsl:text>
    </style>
    <body>
      <table border="0" cellpadding="0" cellspacing="0" width="100%" height="100%">
        
        <tr>
          <td align="left">
            <xsl:for-each select="descendant::helpPopup[@id=$help.id]">
              <xsl:call-template name="output.label" />
            </xsl:for-each>
          </td>
        </tr>

        <tr>
          <td align="right">
            <input type="button" style="{$editor.font} {$editor.button.style}" value="Fenster schliessen" onClick="window.close();" />
          </td>
        </tr>

      </table>
    </body>
  </html>
</xsl:template>

<!-- ========================================================================= -->

</xsl:stylesheet>

