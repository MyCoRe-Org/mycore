<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xsd='http://www.w3.org/2001/XMLSchema'
  version="1.0">

<xsl:output method="xml" encoding="UTF-8" indent="yes"/>

<xsl:param name="mycore_home"/>
<xsl:param name="mycore_appl"/>

<xsl:include href='MCRMetadataCoreTemplates.xsl'/>
<xsl:include href='MCRMetadataCoreTypes.xsl'/>
<xsl:include href='MCRMetadataTemplates.xsl'/>
<xsl:include href='MCRMetadataTypes.xsl'/>

<xsl:variable name="newline">
 <xsl:text>
 </xsl:text>
</xsl:variable>

<xsl:template match="/">

<xsd:schema xmlns:xsd='http://www.w3.org/2001/XMLSchema' 
            xmlns:xml='http://www.w3.org/XML/1998/namespace'
            xmlns:xlink='http://www.w3.org/1999/xlink'
            elementFormDefault="qualified">

 <xsd:import namespace='http://www.w3.org/XML/1998/namespace'
              schemaLocation='xml-2001.xsd'/> 
 <xsd:import namespace="http://www.w3.org/1999/xlink"
              schemaLocation="xlinks-2001.xsd" />

 <xsl:call-template name="mcrtypedefinitioncore"/>
 <xsl:call-template name="mcrtypedefinition"/>

 <xsl:variable name="var" select="/configuration/@type" />
 <xsl:choose>

  <xsl:when test="contains($var,'derivate')">
   <xsd:element name="mycorederivate" type="MCRDerivate"/>
   <xsd:complexType name="MCRDerivate">
    <xsd:sequence>
    <xsd:element name="derivate"  type="MCRObjectDerivate" minOccurs='1' 
      maxOccurs='1' />
    <xsd:element name="service"  type="MCRObjectService" minOccurs='1' 
      maxOccurs='1' />
    </xsd:sequence>
    <xsd:attribute name="ID" type="xsd:string" use="required" />
    <xsd:attribute name="label" type="xsd:string" use="required" />
    <xsd:attribute name="version" type="xsd:string" use="optional" />
   </xsd:complexType>
   <xsl:apply-templates select="/configuration/derivate"/>
   <xsl:apply-templates select="/configuration/service"/>
  </xsl:when>

  <xsl:otherwise>
   <xsd:element name="mycoreobject" type="MCRObject"/>
   <xsd:complexType name="MCRObject">
    <xsd:all>
     <xsd:element name="structure" type="MCRObjectStructure"  minOccurs='1' 
      maxOccurs='1' />
    <xsd:element name="metadata" type="MCRObjectMetadata" minOccurs='1' 
      maxOccurs='1' />
    <xsd:element name="service"  type="MCRObjectService" minOccurs='1' 
      maxOccurs='1' />
    </xsd:all>
    <xsd:attribute name="ID" type="xsd:string" use="required" />
    <xsd:attribute name="label" type="xsd:string" use="required" />
    <xsd:attribute name="version" type="xsd:string" use="optional" />
   </xsd:complexType>
   <xsl:apply-templates select="/configuration/structure"/>
   <xsl:apply-templates select="/configuration/metadata"/>
   <xsl:apply-templates select="/configuration/service"/>
  </xsl:otherwise>

 </xsl:choose>

 </xsd:schema>

</xsl:template>

<!-- Template for the structure part -->

<xsl:template match="/configuration/structure">
 <xsd:complexType name="MCRObjectStructure">
  <xsd:sequence>
   <xsl:for-each select="element">
    <xsd:element name="{@name}" minOccurs="{@minOccurs}" 
     maxOccurs="{@maxOccurs}">
    <xsd:complexType>
     <xsl:apply-templates select="*"/>
     <xsd:attribute name="heritable" type="xsd:boolean" use="optional" />
     <xsd:attribute name="notinherit" type="xsd:boolean" use="optional" />
     <xsd:attribute name="parasearch" type="xsd:boolean" use="optional" />
     <xsd:attribute name="textsearch" type="xsd:boolean" use="optional" />
    </xsd:complexType>
   </xsd:element>
  </xsl:for-each>
  <xsl:value-of select="$newline"/>
  </xsd:sequence>
  <xsd:attribute ref="xml:lang" />
 </xsd:complexType>
</xsl:template>

<!-- Template for the metadata part -->

<xsl:template match="/configuration/metadata">
 <xsd:complexType name="MCRObjectMetadata">
  <xsd:sequence>
 <xsl:for-each select="element">
  <xsd:element name="{@name}" minOccurs="{@minOccurs}" maxOccurs="{@maxOccurs}">
   <xsd:complexType>
    <xsl:apply-templates select="*"/>
    <xsd:attribute name="heritable" type="xsd:boolean" use="optional" />
    <xsd:attribute name="notinherit" type="xsd:boolean" use="optional" />
    <xsl:choose>
     <xsl:when test="contains(@parasearch,'false')">
      <xsd:attribute name="parasearch" type="xsd:boolean" use="required"
        fixed="false" />
     </xsl:when>
     <xsl:otherwise>
      <xsd:attribute name="parasearch" type="xsd:boolean" use="optional" />
     </xsl:otherwise>
    </xsl:choose>
    <xsl:choose>
     <xsl:when test="contains(@textsearch,'true')">
      <xsd:attribute name="textsearch" type="xsd:boolean" use="required" 
        fixed="true"/>
     </xsl:when>
     <xsl:otherwise>
      <xsd:attribute name="textsearch" type="xsd:boolean" use="optional" />
     </xsl:otherwise>
    </xsl:choose>
   </xsd:complexType>
  </xsd:element>
 </xsl:for-each>

   <xsl:value-of select="$newline"/>
  </xsd:sequence>
  <xsd:attribute ref="xml:lang" />
 </xsd:complexType>

</xsl:template>

<!-- Template for the derivate part -->

<xsl:template match="/configuration/derivate">

 <xsd:complexType name="MCRObjectDerivate">
  <xsd:sequence>

 <xsl:for-each select="element">
  <xsd:element name="{@name}" minOccurs="{@minOccurs}" maxOccurs="{@maxOccurs}">
   <xsd:complexType>
    <xsl:apply-templates select="*"/>
    <xsd:attribute name="heritable" type="xsd:boolean" use="optional" />
    <xsd:attribute name="notinherit" type="xsd:boolean" use="optional" />
    <xsd:attribute name="parasearch" type="xsd:boolean" use="optional" />
    <xsd:attribute name="textsearch" type="xsd:boolean" use="optional" />
   </xsd:complexType>
  </xsd:element>
 </xsl:for-each>

   <xsl:value-of select="$newline"/>
  </xsd:sequence>
  <xsd:attribute ref="xml:lang" />
 </xsd:complexType>

</xsl:template>

<!-- Template for the service part -->

<xsl:template match="/configuration/service">

 <xsd:complexType name="MCRObjectService">
  <xsd:all>

 <xsl:for-each select="element">
  <xsd:element name="{@name}" minOccurs="{@minOccurs}" maxOccurs="{@maxOccurs}">
   <xsd:complexType>
    <xsl:apply-templates select="*"/>
    <xsd:attribute name="heritable" type="xsd:boolean" use="optional" />
    <xsd:attribute name="notinherit" type="xsd:boolean" use="optional" />
    <xsd:attribute name="parasearch" type="xsd:boolean" use="optional" />
    <xsd:attribute name="textsearch" type="xsd:boolean" use="optional" />
   </xsd:complexType>
  </xsd:element>
 </xsl:for-each>

   <xsl:value-of select="$newline"/>
  </xsd:all>
  <xsd:attribute ref="xml:lang" />
 </xsd:complexType>

</xsl:template>

</xsl:stylesheet>

