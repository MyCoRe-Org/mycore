<?xml version="1.0" encoding="UTF-8"?>
<!-- ============================================== -->
<!-- $Revision: 1.1 $ $Date: 2007-12-14 14:31:25 $ -->
<!-- ============================================== -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:mcr="http://www.mycore.org/"
  xmlns:acl="xalan://org.mycore.access.MCRAccessManager" xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
  xmlns:xalan="http://xml.apache.org/xalan" exclude-result-prefixes="xlink mcr xalan i18n acl">
  <xsl:output method="html" />
  <xsl:param name="ServletsBaseURL" />
  <!-- HttpSession is empty if cookies are enabled, else ";jsessionid=<id>" -->
  <xsl:param name="HttpSession" />

  <xsl:template match="/module-webcli">
    <html>
      <head>
        <script type="text/javascript">
          <xsl:value-of select="concat('var webCLIServlet=&#x22;',$ServletsBaseURL,'MCRWebCLIServlet',$HttpSession,'&#x22;;')" />
        </script>
        <xsl:apply-templates select="head/*" />
      </head>
      <xsl:apply-templates select="body" />
    </html>
  </xsl:template>

  <xsl:template match='@*|node()'>
    <xsl:copy>
      <xsl:apply-templates select='@*|node()' />
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>