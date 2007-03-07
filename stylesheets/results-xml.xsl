<?xml version="1.0" encoding="UTF-8"?>

<!-- ============================================== -->
<!-- $Revision: 1.1 $ $Date: 2007-03-07 10:06:29 $ -->
<!-- ============================================== -->

<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:mcr="http://www.mycore.org/"
  >
	
<xsl:output method="xml" encoding="UTF-8"/>

<xsl:template match="/mcr:results">
	<xsl:copy-of select="." />
</xsl:template>

</xsl:stylesheet>