<?xml version="1.0" encoding="ISO_8859-1"?>

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

<xsl:include href="editor-config.xsl" />

<!-- ============ Parameter aus MyCoRe LayoutServlet ============ -->

<xsl:param name="WebApplicationBaseURL" />

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
            <xsl:variable name="help" select="descendant::helpPopup[@id=$help.id]" />
            <xsl:copy-of select="$help/*|$help/text()" />
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

