<?xml version="1.0"?>
<xsl:stylesheet version="3.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fn="http://www.w3.org/2005/xpath-functions"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:mcrsolr="http://www.mycore.de/xslt/solr">
                
  <xsl:function name="mcrsolr:historydate-julian-day-to-date-string" as="xs:string">
    <xsl:param name="jdn" as="xs:integer"/>
    <!-- Julianisches Datum: Es gibt die Zeit in Tagen und Tagesbruchteilen an, 
    die seit dem 1. Januar −4712 (4713 v. Chr.), 12:00 Uhr UT vergangen sind -->
    <!--  Mo., 24. Nov. 4714 B.C. -->
    
    <xsl:variable name="day0" select="xs:dateTime('-4713-11-24T12:00:00Z')" />
    <xsl:variable name="calc" select="$day0 + xs:dayTimeDuration(concat('P',$jdn, 'D'))" />
       
    <xsl:value-of select="format-dateTime($calc,'[Y0001]-[M01]-[D01]')"/> 
  </xsl:function>
</xsl:stylesheet>