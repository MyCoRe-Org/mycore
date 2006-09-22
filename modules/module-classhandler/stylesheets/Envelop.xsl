<?xml version="1.0" encoding="UTF-8"?>

<!-- ============================================== -->
<!-- A XML transformer to remove the SOAP-cover     -->
<!-- $Revision: 1.1 $ $Date: 2006-09-22 09:45:04 $ -->
<!-- ============================================== --> 

<xsl:stylesheet 
  version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:soapenv="http://www.mist.de"
>

<xsl:output 
  method="xml" 
  encoding="UTF-8" 
/>

<xsl:template match="/">
  <xsl:copy-of select="soapenv:Envelope/soapenv:Body/multiRef/." />
</xsl:template>

</xsl:stylesheet>
