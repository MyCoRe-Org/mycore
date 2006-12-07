<?xml version="1.0" encoding="UTF-8"?>

<!-- ============================================== -->
<!-- $Revision: 1.6 $ $Date: 2006-12-07 10:54:00 $ -->
<!-- ============================================== -->

<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xsd='http://www.w3.org/2001/XMLSchema'
  version="1.0">

<xsl:output method="xml" encoding="UTF-8" indent="yes"/>

<xsl:template name="mcrtypedefinitioncore">

<!-- Data type for default MyCoRe attributes -->

<xsd:simpleType name="mcrdefaulttype">
 <xsd:restriction base="xsd:string">
  <xsd:maxLength value="256"/>
 </xsd:restriction>
</xsd:simpleType>

<xsd:simpleType name="mcrdefaultform">
 <xsd:restriction base="xsd:string">
  <xsd:maxLength value="256"/>
 </xsd:restriction>
</xsd:simpleType>

<xsd:simpleType name="mcrdateformat">
 <xsd:restriction base="xsd:string">
  <xsd:maxLength value="256"/>
 </xsd:restriction>
</xsd:simpleType>

<xsd:simpleType name="mcrobjectid">
 <xsd:restriction base="xsd:string">
  <xsd:maxLength value="64"/>
 </xsd:restriction>
</xsd:simpleType>

<!-- Data type for MyCoRe category attributes -->

<xsd:simpleType name="mcrcategory">
 <xsd:restriction base="xsd:string">
  <xsd:maxLength value="128"/>
 </xsd:restriction>
</xsd:simpleType>

<!-- Data type for MyCoRe number attributes -->

<xsd:simpleType name="mcrdimension">
 <xsd:restriction base="xsd:string">
  <xsd:maxLength value="128"/>
 </xsd:restriction>
</xsd:simpleType>

<xsd:simpleType name="mcrmeasurement">
 <xsd:restriction base="xsd:string">
  <xsd:maxLength value="64"/>
 </xsd:restriction>
</xsd:simpleType>

</xsl:template>

</xsl:stylesheet>
