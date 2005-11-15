<?xml version="1.0" encoding="UTF-8"?>

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

       <xsd:simpleType name="mcrmonth">
         <xsd:annotation>
           <xsd:documentation>
             mcrmonth is like a normal month, but it my contain the value 0 to show
             that the month of the date is not known
           </xsd:documentation>
         </xsd:annotation>
         <xsd:restriction base="xsd:unsignedByte">
           <xsd:maxInclusive value="12"/>
         </xsd:restriction>
       </xsd:simpleType>

       <xsd:simpleType name="mcrday">
         <xsd:annotation>
           <xsd:documentation>
             mcrday is like a normal day, but it my contain the value 0 to show
             that the day of the date is not known. For application logic
             mcrdatefragment should solve the following constraint:
             day > 0 --> month > 0
           </xsd:documentation>
         </xsd:annotation>
         <xsd:restriction base="xsd:unsignedByte">
           <xsd:maxInclusive value="31"/>
         </xsd:restriction>
       </xsd:simpleType>
       
       <xsd:complexType name="mcrdatefragment">
         <xsd:sequence>
           <xsd:element name="year" type="xsd:gYear"/>
           <xsd:element name="month" type="mcrmonth"/>
           <xsd:element name="day" type="mcrday"/>
         </xsd:sequence>
       </xsd:complexType>

</xsl:template>

</xsl:stylesheet>
