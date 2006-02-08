<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xsd='http://www.w3.org/2001/XMLSchema'
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
<xsd:sequence>
 <xsd:element name="{@name}" minOccurs="{@minOccurs}" maxOccurs="{@maxOccurs}">
   <xsd:complexType>
     <xsd:simpleContent>
       <xsd:extension base="xsd:string">
         <xsd:attribute name="type" use="optional" type="mcrdefaulttype" />
         <xsd:attribute name="form" use="optional" type="mcrdefaultform" />
         <xsd:attribute name="inherited" use="optional" type="xsd:integer" />
         <xsd:attribute ref="xml:lang" />
       </xsd:extension>
     </xsd:simpleContent>
   </xsd:complexType>
 </xsd:element>
</xsd:sequence>
<xsd:attribute name="class" type="xsd:string" use="required"
  fixed="MCRMetaLangText" />
</xsl:template>

<!-- Template for the metadata MCRMetaXML -->

<xsl:template match="mcrmetaxml">
<xsd:sequence>
 <xsd:element name="{@name}" minOccurs="{@minOccurs}" maxOccurs="{@maxOccurs}">
   <xsd:complexType>
    <xsd:sequence>
     <xsd:any processContents="skip"/>
    </xsd:sequence>
   </xsd:complexType>
 </xsd:element>
</xsd:sequence>
<xsd:attribute name="class" type="xsd:string" use="required"
  fixed="MCRMetaXML" />
</xsl:template>

<!-- Template for the metadata MCRMetaLink -->

<xsl:template match="mcrmetalink">
<xsd:sequence>
 <xsd:element name="{@name}" minOccurs="{@minOccurs}" maxOccurs="{@maxOccurs}">
   <xsd:complexType>
     <xsd:attribute name="inherited" use="optional" type="xsd:integer" />
     <xsd:attribute ref="xlink:type" />
     <xsd:attribute ref="xlink:href" use="optional"/>
     <xsd:attribute ref="xlink:title" use="optional"/>
     <xsd:attribute ref="xlink:label" use="optional"/>
     <xsd:attribute ref="xlink:from" use="optional"/>
     <xsd:attribute ref="xlink:to" use="optional"/>
   </xsd:complexType>
  </xsd:element>
</xsd:sequence>
<xsd:attribute name="class" type="xsd:string" use="required" 
  fixed="MCRMetaLink"/>
</xsl:template>

<!-- Template for the metadata MCRMetaLinkID -->

<xsl:template match="mcrmetalinkid">
<xsd:sequence>
 <xsd:element name="{@name}" minOccurs="{@minOccurs}" maxOccurs="{@maxOccurs}">
   <xsd:complexType>
     <xsd:attribute name="inherited" use="optional" type="xsd:integer" />
     <xsd:attribute ref="xlink:type" />
     <xsd:attribute ref="xlink:href" use="optional"/>
     <xsd:attribute ref="xlink:title" use="optional"/>
     <xsd:attribute ref="xlink:label" use="optional"/>
     <xsd:attribute ref="xlink:from" use="optional"/>
     <xsd:attribute ref="xlink:to" use="optional"/>
   </xsd:complexType>
  </xsd:element>
</xsd:sequence>
<xsd:attribute name="class" type="xsd:string" use="required" 
  fixed="MCRMetaLinkID"/>
</xsl:template>

<!-- Template for the metadata MCRMetaClassification -->

<xsl:template match="mcrmetaclassification">
<xsd:sequence>
 <xsd:element name="{@name}" minOccurs="{@minOccurs}" maxOccurs="{@maxOccurs}">
   <xsd:complexType>
     <xsd:attribute name="classid" use="required" type="mcrobjectid" />
     <xsd:attribute name="categid" use="required" type="mcrcategory" />
     <xsd:attribute name="type" use="optional" type="mcrdefaulttype" />
     <xsd:attribute name="inherited" use="optional" type="xsd:integer" />
     <xsd:attribute ref="xml:lang" use="optional" />
   </xsd:complexType>
  </xsd:element>
</xsd:sequence>
<xsd:attribute name="class" type="xsd:string" use="required"
  fixed="MCRMetaClassification" />
</xsl:template>

<!-- Template for the metadata MCRMetaDate -->

<xsl:template match="mcrmetadate">
<xsd:sequence>
 <xsd:element name="{@name}" minOccurs="{@minOccurs}" maxOccurs="{@maxOccurs}">
   <xsd:complexType>
     <xsd:simpleContent>
       <xsd:extension base="xsd:string">
         <xsd:attribute name="type" use="optional" type="mcrdefaulttype" />
         <xsd:attribute name="inherited" use="optional" type="xsd:integer" />
         <xsd:attribute ref="xml:lang" />
       </xsd:extension>
     </xsd:simpleContent>
   </xsd:complexType>
  </xsd:element>
</xsd:sequence>
<xsd:attribute name="class" type="xsd:string" use="required"
  fixed="MCRMetaDate" />
</xsl:template>

<!-- Template for the metadata MCRMetaISO8601Date -->

<xsl:template match="mcrmetaiso8601date">
<xsd:sequence>
 <xsd:element name="{@name}" minOccurs="{@minOccurs}" maxOccurs="{@maxOccurs}">
   <xsd:complexType>
     <xsd:simpleContent>
	   <!-- sorry, there is not yet a primitive datatype for all formats of http://www.w3.org/TR/NOTE-datetime -->
       <xsd:extension base="xsd:string">
         <xsd:attribute name="type" use="optional" type="mcrdefaulttype" />
         <xsd:attribute name="format" use="optional" type="mcrdateformat" />
         <xsd:attribute name="inherited" use="optional" type="xsd:integer" />
       </xsd:extension>
     </xsd:simpleContent>
   </xsd:complexType>
  </xsd:element>
</xsd:sequence>
<xsd:attribute name="class" type="xsd:string" use="required"
  fixed="MCRMetaISO8601Date" />
</xsl:template>

<!-- Template for the metadata MCRMetaIFS -->

<xsl:template match="mcrmetaifs">
<xsd:sequence>
 <xsd:element name="{@name}" minOccurs="{@minOccurs}" maxOccurs="{@maxOccurs}">
   <xsd:complexType>
     <xsd:attribute name="sourcepath" use="required" type="xsd:string" />
     <xsd:attribute name="maindoc" use="required" type="xsd:string"/>
     <xsd:attribute name="ifsid" use="optional" type="xsd:string"/>
     <xsd:attribute name="type" use="optional" type="mcrdefaulttype" />
     <xsd:attribute ref="xml:lang" use="optional" />
     <xsd:attribute name="inherited" use="optional" type="xsd:integer" />
   </xsd:complexType>
  </xsd:element>
</xsd:sequence>
<xsd:attribute name="class" type="xsd:string" use="required" 
  fixed="MCRMetaIFS"/>
</xsl:template>

<!-- Template for the metadata MCRMetaNumber -->

<xsl:template match="mcrmetanumber">
<xsd:sequence>
 <xsd:element name="{@name}" minOccurs="{@minOccurs}" maxOccurs="{@maxOccurs}">
   <xsd:complexType>
     <xsd:simpleContent>
       <xsd:extension base="xsd:string">
         <xsd:attribute name="type" use="optional" type="mcrdefaulttype" />
         <xsd:attribute name="inherited" use="optional" type="xsd:integer" />
         <xsd:attribute ref="xml:lang" />
         <xsd:attribute name="dimension" use="optional" type="mcrdimension" />
         <xsd:attribute name="measurement" use="optional" type="mcrmeasurement" />
       </xsd:extension>
     </xsd:simpleContent>
   </xsd:complexType>
  </xsd:element>
</xsd:sequence>
<xsd:attribute name="class" type="xsd:string" use="required"
  fixed="MCRMetaNumber" />
</xsl:template>

<!-- Template for the metadata MCRMetaPerson -->

<xsl:template match="mcrmetaperson">
<xsd:sequence>
 <xsd:element name="{@name}" minOccurs="{@minOccurs}" maxOccurs="{@maxOccurs}">
  <xsd:complexType>
   <xsd:sequence>
    <xsd:element name="firstname" type="xsd:string" minOccurs='0' 
     maxOccurs='1'/>
    <xsd:element name="callname" type="xsd:string" minOccurs='0' 
     maxOccurs='1'/>
    <xsd:element name="surname" type="xsd:string" minOccurs='0' 
     maxOccurs='1'/>
    <xsd:element name="academic" type="xsd:string" minOccurs='0' 
     maxOccurs='1'/>
    <xsd:element name="peerage" type="xsd:string" minOccurs='0' 
     maxOccurs='1'/>
   </xsd:sequence>
   <xsd:attribute name="type" use="optional" type="mcrdefaulttype" />
   <xsd:attribute ref="xml:lang" />
   <xsd:attribute name="inherited" use="optional" type="xsd:integer" />
  </xsd:complexType>
 </xsd:element>
</xsd:sequence>
<xsd:attribute name="class" type="xsd:string" use="required" 
  fixed="MCRMetaPerson"/>
</xsl:template>

<!-- Template for the metadata MCRMetaAddress -->

<xsl:template match="mcrmetaaddress">
<xsd:sequence>
 <xsd:element name="{@name}" minOccurs="{@minOccurs}" maxOccurs="{@maxOccurs}">
  <xsd:complexType>
   <xsd:all>
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
   </xsd:all>
   <xsd:attribute name="type" use="optional" type="mcrdefaulttype" />
   <xsd:attribute name="inherited" use="optional" type="xsd:integer" />
   <xsd:attribute ref="xml:lang" />
  </xsd:complexType>
 </xsd:element>
</xsd:sequence>
<xsd:attribute name="class" type="xsd:string" use="required" 
  fixed="MCRMetaAddress"/>
</xsl:template>

<!-- Template for the metadata MCRMetaCorporation -->

<xsl:template match="mcrmetacorporation">
<xsd:sequence>
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
   <xsd:attribute name="type" use="optional" type="mcrdefaulttype" />
   <xsd:attribute name="inherited" use="optional" type="xsd:integer" />
   <xsd:attribute ref="xml:lang" />
  </xsd:complexType>
 </xsd:element>
</xsd:sequence>
<xsd:attribute name="class" type="xsd:string" use="required" 
  fixed="MCRMetaCorporation"/>
</xsl:template>

<xsl:template match="mcrmetaboolean">
<xsd:sequence>
 <xsd:element name="{@name}" minOccurs="{@minOccurs}" maxOccurs="{@maxOccurs}">
   <xsd:complexType>
     <xsd:simpleContent>
       <xsd:extension base="xsd:string">
         <xsd:attribute name="type" use="optional" type="mcrdefaulttype" />
         <xsd:attribute name="inherited" use="optional" type="xsd:integer" />
         <xsd:attribute ref="xml:lang" />
       </xsd:extension>
     </xsd:simpleContent>
   </xsd:complexType>
 </xsd:element>
</xsd:sequence>
<xsd:attribute name="class" type="xsd:string" use="required"
  fixed="MCRMetaBoolean" />
</xsl:template>

<!-- Template for the metadata MCRMetaAccessRule -->

<xsl:template match="mcrmetaaccessrule">
<xsd:sequence>
 <xsd:element name="{@name}" minOccurs="{@minOccurs}" maxOccurs="{@maxOccurs}">
   <xsd:complexType>
    <xsd:sequence>
     <xsd:any processContents="skip"/>
    </xsd:sequence>
    <xsd:attribute name="permission" use="required" type="xsd:string" />
    <xsd:attribute name="inherited" use="optional" type="xsd:string" />
    <xsd:attribute name="type" use="optional" type="mcrdefaulttype" />
    <xsd:attribute ref="xml:lang" />
   </xsd:complexType>
 </xsd:element>
</xsd:sequence>
<xsd:attribute name="class" type="xsd:string" use="required"
  fixed="MCRMetaAccessRule" />
</xsl:template>

</xsl:stylesheet>

