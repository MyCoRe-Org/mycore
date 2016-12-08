<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" exclude-result-prefixes="">

  <xsl:param name="WebApplicationBaseURL"></xsl:param>

  <xsl:template match="processingGUI">

    <script src="{$WebApplicationBaseURL}modules/webtools/node_modules/shim.js"></script>

    <script src="{$WebApplicationBaseURL}modules/webtools/node_modules/zone.js"></script>
    <script src="{$WebApplicationBaseURL}modules/webtools/node_modules/Reflect.js"></script>
    <script src="{$WebApplicationBaseURL}modules/webtools/node_modules/system.js"></script>

    <script src="{$WebApplicationBaseURL}modules/webtools/processing/systemjs.config.js"></script>
    <script>
      System.import('app').catch(function(err) {
        console.error(err);
      });
    </script>

    <processing></processing>
  </xsl:template>

</xsl:stylesheet>