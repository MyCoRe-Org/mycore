<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  version="1.0">

<xsl:output method="xml" encoding="UTF-8" indent="yes"/>

<xsl:param name="mycore_home"/>
<xsl:param name="mycore_appl"/>

<xsl:include href='/dlwww/cvs/mycore/stylesheets/MCRMetadataTemplates.xsl'/>
<xsl:include href='/dlwww/cvs/mycore-sample-application/stylesheets/MCRMetadataTemplates.xsl'/>

<xsl:variable name="newline">
 <xsl:text>
 </xsl:text>
</xsl:variable>

<xsl:template match="/">

<xsd:schema xmlns:xsd='http://www.w3.org/2001/XMLSchema' 
            xmlns:xml='http://www.w3.org/XML/1998/namespace'
            xmlns:xlink='http://www.w3.org/1999/xlink'
            elementFormDefault="unqualified">

 <xsd:import namespace='http://www.w3.org/XML/1998/namespace'
              schemaLocation='{$mycore_home}/schema/xml-2001.xsd'/> 
 <xsd:import namespace="http://www.w3.org/1999/xlink"
              schemaLocation="{$mycore_home}/schema/xlinks-2001.xsd" />
 <xsd:include schemaLocation='{$mycore_home}/schema/MCRObjectStructure.xsd'/>
 <xsd:include schemaLocation='{$mycore_home}/schema/MCRObjectService.xsd'/>
 

 <xsd:element name="mycoreobject" type="MCRObject"/>

 <xsd:complexType name="MCRObject">
  <xsd:sequence>
  <xsd:element name="structure" type="MCRObjectStructure"  minOccurs='1' 
    maxOccurs='1' />
  <xsd:element name="metadata" type="MCRObjectMetadata" minOccurs='1' 
    maxOccurs='1' />
  <xsd:element name="service"  type="MCRObjectService" minOccurs='1' 
    maxOccurs='1' />
  </xsd:sequence>
  <xsd:attribute name="ID" type="xsd:string" use="required" />
  <xsd:attribute name="label" type="xsd:string" use="required" />
 </xsd:complexType>

 <xsl:apply-templates select="/configuration/metadata"/>

 </xsd:schema>

</xsl:template>

<!-- Template for the metadata part -->

<xsl:template match="/configuration/metadata">

 <xsd:complexType name="MCRObjectMetadata">
  <xsd:sequence>

 <xsl:for-each select="element">
  <xsd:element name="{@name}" minOccurs="{@minOccurs}" maxOccurs="{@maxOccurs}">
   <xsd:complexType>
    <xsd:sequence>
      <xsl:apply-templates select="data"/>
    </xsd:sequence>
    <xsd:attribute name="class" type="xsd:string" use="required" />
    <xsd:attribute name="heritable" type="xsd:boolean" use="optional" />
   </xsd:complexType>
  </xsd:element>
 </xsl:for-each>
  <xsl:value-of select="$newline"/>
  </xsd:sequence>
  <xsd:attribute ref="xml:lang" />
 </xsd:complexType>

</xsl:template>

<!-- Call templates for each metadata -->

<xsl:template match="data">
 <xsl:apply-templates select="*"/>
</xsl:template>

</xsl:stylesheet>

