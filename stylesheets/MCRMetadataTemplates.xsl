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

</xsl:stylesheet>

