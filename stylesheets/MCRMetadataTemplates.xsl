<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  version="1.0">

<xsl:output method="xml" encoding="UTF-8" indent="yes"/>

<xsl:param name="mycore_home"/>
<xsl:param name="mycore_appl"/>

<xsl:variable name="newline">
 <xsl:text>
 </xsl:text>
</xsl:variable>

<!-- Dummy Template fo label -->

<xsl:template match="label">
</xsl:template>

<!-- Template for the metadata MCRMetaLangText -->

<xsl:template match="mcrmetalangtext">
 <xsd:element name="{@name}" minOccurs="{@minOccurs}" maxOccurs="{@maxOccurs}">
   <xsd:complexType>
     <xsd:simpleContent>
       <xsd:extension base="xsd:string">
         <xsd:attribute name="type" use="optional" />
         <xsd:attribute ref="xml:lang" />
       </xsd:extension>
     </xsd:simpleContent>
   </xsd:complexType>
 </xsd:element>
</xsl:template>

<!-- Template for the metadata MCRMetaLink -->

<xsl:template match="mcrmetalink">
 <xsd:element name="{@name}" minOccurs="{@minOccurs}" maxOccurs="{@maxOccurs}">
   <xsd:complexType>
     <xsd:attribute ref="xlink:type" />
     <xsd:attribute ref="xlink:href" use="optional"/>
     <xsd:attribute ref="xlink:title" use="optional"/>
     <xsd:attribute ref="xlink:label" use="optional"/>
     <xsd:attribute ref="xlink:from" use="optional"/>
     <xsd:attribute ref="xlink:to" use="optional"/>
   </xsd:complexType>
  </xsd:element>
</xsl:template>

<!-- Template for the metadata MCRMetaLinkID -->

<xsl:template match="mcrmetalinkid">
 <xsd:element name="{@name}" minOccurs="{@minOccurs}" maxOccurs="{@maxOccurs}">
   <xsd:complexType>
     <xsd:attribute ref="xlink:type" />
     <xsd:attribute ref="xlink:href" use="optional"/>
     <xsd:attribute ref="xlink:title" use="optional"/>
     <xsd:attribute ref="xlink:label" use="optional"/>
     <xsd:attribute ref="xlink:from" use="optional"/>
     <xsd:attribute ref="xlink:to" use="optional"/>
   </xsd:complexType>
  </xsd:element>
</xsl:template>

<!-- Template for the metadata MCRMetaClassification -->

<xsl:template match="mcrmetaclassification">
 <xsd:element name="{@name}" minOccurs="{@minOccurs}" maxOccurs="{@maxOccurs}">
   <xsd:complexType>
     <xsd:attribute name="classid" />
     <xsd:attribute name="categid" />
     <xsd:attribute name="type" use="optional" />
     <xsd:attribute ref="xml:lang" use="optional" />
   </xsd:complexType>
  </xsd:element>
</xsl:template>

<!-- Template for the metadata MCRMetaDate -->

<xsl:template match="mcrmetadate">
 <xsd:element name="{@name}" minOccurs="{@minOccurs}" maxOccurs="{@maxOccurs}">
   <xsd:complexType>
     <xsd:simpleContent>
       <xsd:extension base="xsd:string">
         <xsd:attribute name="type" use="optional" />
         <xsd:attribute ref="xml:lang" />
       </xsd:extension>
     </xsd:simpleContent>
   </xsd:complexType>
  </xsd:element>
</xsl:template>

<!-- Template for the metadata MCRMetaPerson -->

<xsl:template match="mcrmetaperson">
 <xsd:element name="{@name}" minOccurs="{@minOccurs}" maxOccurs="{@maxOccurs}">
  <xsd:complexType>
   <xsd:sequence>
    <xsd:element name="firstname" type="xsd:string" minOccurs='0' 
     maxOccurs='1'/>
    <xsd:element name="callname" type="xsd:string" minOccurs='0' 
     maxOccurs='1'/>
    <xsd:element name="surename" type="xsd:string" minOccurs='0' 
     maxOccurs='1'/>
    <xsd:element name="academic" type="xsd:string" minOccurs='0' 
     maxOccurs='1'/>
    <xsd:element name="peerage" type="xsd:string" minOccurs='0' 
     maxOccurs='1'/>
   </xsd:sequence>
   <xsd:attribute name="type" use="optional" />
   <xsd:attribute ref="xml:lang" />
  </xsd:complexType>
 </xsd:element>
</xsl:template>

<!-- Template for the metadata MCRMetaAddress -->

<xsl:template match="mcrmetaaddress">
 <xsd:element name="{@name}" minOccurs="{@minOccurs}" maxOccurs="{@maxOccurs}">
  <xsd:complexType>
   <xsd:sequence>
    <xsd:element name="country" type="xsd:string" minOccurs='0' 
     maxOccurs='1'/>
    <xsd:element name="state" type="xsd:string" minOccurs='0' 
     maxOccurs='1'/>
    <xsd:element name="zipcode" type="xsd:string" minOccurs='0' 
     maxOccurs='1'/>
    <xsd:element name="city" type="xsd:string" minOccurs='0' 
     maxOccurs='1'/>
    <xsd:element name="street" type="xsd:string" minOccurs='0' 
     maxOccurs='1'/>
    <xsd:element name="number" type="xsd:string" minOccurs='0' 
     maxOccurs='1'/>
   </xsd:sequence>
   <xsd:attribute name="type" use="optional" />
   <xsd:attribute ref="xml:lang" />
  </xsd:complexType>
 </xsd:element>
</xsl:template>

<!-- Template for the metadata MCRMetaCorporation -->

<xsl:template match="mcrmetacorporation">
 <xsd:element name="{@name}" minOccurs="{@minOccurs}" maxOccurs="{@maxOccurs}">
  <xsd:complexType>
   <xsd:sequence>
    <xsd:element name="name" type="xsd:string" minOccurs='0' 
     maxOccurs='1'/>
    <xsd:element name="nickname" type="xsd:string" minOccurs='0' 
     maxOccurs='1'/>
    <xsd:element name="parent" type="xsd:string" minOccurs='0' 
     maxOccurs='1'/>
    <xsd:element name="property" type="xsd:string" minOccurs='0' 
     maxOccurs='1'/>
   </xsd:sequence>
   <xsd:attribute name="type" use="optional" />
   <xsd:attribute ref="xml:lang" />
  </xsd:complexType>
 </xsd:element>
</xsl:template>

</xsl:stylesheet>

