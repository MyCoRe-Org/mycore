<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan" xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:xlink="http://www.w3.org/1999/xlink" version="1.0">

  <xsl:output method="xml" encoding="UTF-8" indent="yes" xalan:indent-amount="2" />
  <xsl:include href="datamodel2ext.xsl"/>

  <xsl:template match="/">
    <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      elementFormDefault="qualified" xmlns:xlink="http://www.w3.org/1999/xlink">
      <xs:import namespace="http://www.w3.org/1999/xlink" schemaLocation="http://www.w3.org/XML/2008/06/xlink.xsd" />
      <xs:import namespace="http://www.w3.org/XML/1998/namespace" schemaLocation="http://www.w3.org/2001/xml.xsd" />
      <xs:include schemaLocation="mcrcommon-datamodel.xsd" />
      <xsl:apply-templates select="objecttype/xsd" />
      <xsl:apply-templates mode="structure" />
      <xsl:apply-templates mode="metadata" />
      <xsl:apply-templates select="objecttype/metadata/element" mode="metadata" />
    </xs:schema>
  </xsl:template>

  <xsl:template match="xsd">
    <xsl:copy-of select="*" />
  </xsl:template>

  <xsl:template match="objecttype" mode="structure">
    <xs:element name="structure">
      <xs:complexType>
        <xs:sequence>
          <xsl:if test="@isChild = 'true'">
            <xs:element ref="parents" minOccurs="0" />
          </xsl:if>
          <xsl:if test="@isParent = 'true'">
            <xs:element ref="children" minOccurs="0" />
          </xsl:if>
          <xsl:if test="@hasDerivates = 'true'">
            <xs:element ref="derobjects" minOccurs="0" />
          </xsl:if>
        </xs:sequence>
      </xs:complexType>
    </xs:element>
  </xsl:template>

  <xsl:template match="objecttype" mode="metadata">
    <xs:element name="metadata">
      <xs:complexType>
        <xs:all>
          <xsl:for-each select="/objecttype/metadata/element/@name">
            <xs:element ref="{concat('def.',.)}">
              <xsl:if test="../@minOccurs = 0">
                <xsl:attribute name="minOccurs">
                <xsl:value-of select="0" />
              </xsl:attribute>
              </xsl:if>
            </xs:element>
          </xsl:for-each>
        </xs:all>
        <xs:attribute ref="xml:lang" use="optional"/>
      </xs:complexType>
    </xs:element>
  </xsl:template>

  <xsl:template match="element" mode="enclosing">
    <xsl:param name="class" />
    <xs:element>
      <xsl:attribute name="name">
        <xsl:choose>
          <xsl:when test="@wrapper">
            <xsl:value-of select="@wrapper" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="concat('def.',@name)" />
          </xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
      <xs:complexType>
        <xs:sequence>
          <xs:element maxOccurs="unbounded" ref="">
            <xsl:if test="@maxOccurs!=1">
              <xsl:attribute name="maxOccurs">
                <xsl:value-of select="@maxOccurs" />
              </xsl:attribute>
            </xsl:if>
            <xsl:if test="@minOccurs &gt; 1">
              <xsl:attribute name="minOccurs">
                <xsl:value-of select="@minOccurs" />
              </xsl:attribute>
            </xsl:if>
            <xsl:attribute name="ref">
              <xsl:value-of select="@name" />
            </xsl:attribute>
          </xs:element>
        </xs:sequence>
        <xs:attribute name="class" use="required" type="xs:NCName" fixed="{$class}" />
        <xsl:choose>
          <xsl:when test="@notinherit='ignore' and @heritable='ignore'">
            <xs:attribute name="notinherit" type="xs:boolean" />
            <xs:attribute name="heritable" type="xs:boolean" />
          </xsl:when>
          <xsl:when test="@notinherit='ignore'">
            <xs:attribute name="notinherit" type="xs:boolean" />
            <xs:attribute name="heritable" type="xs:boolean" use="required" fixed="false" />
          </xsl:when>
          <xsl:when test="@heritable='ignore'">
            <xs:attribute name="notinherit" type="xs:boolean" use="required" fixed="false" />
            <xs:attribute name="heritable" type="xs:boolean" />
          </xsl:when>
          <xsl:otherwise>
            <xs:attributeGroup ref="noInheritance" />
          </xsl:otherwise>
        </xsl:choose>
      </xs:complexType>
    </xs:element>
  </xsl:template>
  <xsl:template match="element" mode="inner">
    <xsl:param name="class" />
    <xsl:param name="containsText" select="false()" />
    <xsl:param name="complexType" select="''" />
    <xsl:param name="textFormat" select="'xs:string'" />
    <xs:element>
      <xsl:attribute name="name">
        <xsl:value-of select="@name" />
      </xsl:attribute>
      <xs:complexType>
        <xsl:choose>
          <xsl:when test="$containsText">
            <xs:simpleContent>
              <xs:extension base="{$textFormat}">
                <xs:attributeGroup ref="{$class}" />
                <xs:attributeGroup ref="notInherited" />
                <xsl:apply-templates select="." mode="types" />
              </xs:extension>
            </xs:simpleContent>
          </xsl:when>
          <xsl:otherwise>
            <xsl:copy-of select="$complexType" />
            <xs:attributeGroup ref="{$class}" />
            <xs:attributeGroup ref="notInherited" />
            <xsl:apply-templates select="." mode="types" />
          </xsl:otherwise>
        </xsl:choose>
      </xs:complexType>
    </xs:element>
  </xsl:template>

  <xsl:template match="element[@type='text']" mode="metadata">
    <xsl:apply-templates select="." mode="enclosing">
      <xsl:with-param name="class" select="'MCRMetaLangText'" />
    </xsl:apply-templates>
    <xsl:apply-templates select="." mode="inner">
      <xsl:with-param name="class" select="'MCRMetaLangText'" />
      <xsl:with-param name="containsText" select="true()" />
    </xsl:apply-templates>
  </xsl:template>

  <xsl:template match="element[@type='boolean']" mode="metadata">
    <xsl:apply-templates select="." mode="enclosing">
      <xsl:with-param name="class" select="'MCRMetaBoolean'" />
    </xsl:apply-templates>
    <xsl:apply-templates select="." mode="inner">
      <xsl:with-param name="class" select="'MCRMetaBoolean'" />
      <xsl:with-param name="containsText" select="true()" />
    </xsl:apply-templates>
  </xsl:template>

  <xsl:template match="element[@type='classification']" mode="metadata">
    <xsl:apply-templates select="." mode="enclosing">
      <xsl:with-param name="class" select="'MCRMetaClassification'" />
    </xsl:apply-templates>
    <xsl:apply-templates select="." mode="inner">
      <xsl:with-param name="class" select="'MCRMetaClassification'" />
    </xsl:apply-templates>
  </xsl:template>

  <xsl:template match="element[@type='link']" mode="metadata">
    <xsl:apply-templates select="." mode="enclosing">
      <xsl:with-param name="class" select="'MCRMetaLinkID'" />
    </xsl:apply-templates>
    <xsl:apply-templates select="." mode="inner">
      <xsl:with-param name="class" select="'MCRMetaLinkID'" />
    </xsl:apply-templates>
  </xsl:template>

  <xsl:template match="element[@type='href']" mode="metadata">
    <xsl:apply-templates select="." mode="enclosing">
      <xsl:with-param name="class" select="'MCRMetaLink'" />
    </xsl:apply-templates>
    <xsl:apply-templates select="." mode="inner">
      <xsl:with-param name="class" select="'MCRMetaLink'" />
    </xsl:apply-templates>
  </xsl:template>

  <xsl:template match="element[@type='derlink']" mode="metadata">
    <xsl:apply-templates select="." mode="enclosing">
      <xsl:with-param name="class" select="'MCRMetaDerivateLink'" />
    </xsl:apply-templates>
    <xsl:variable name="innerSchema">
      <xs:sequence>
        <xs:element name="annotation" minOccurs="0" maxOccurs="unbounded">
          <xs:complexType>
            <xs:simpleContent>
              <xs:extension base="xs:string">
                <xs:attribute ref="xml:lang" />
              </xs:extension>
            </xs:simpleContent>
          </xs:complexType>
        </xs:element>
      </xs:sequence>
    </xsl:variable>
    <xsl:apply-templates select="." mode="inner">
      <xsl:with-param name="class" select="'MCRMetaDerivateLink'" />
      <xsl:with-param name="complexType" select="xalan:nodeset($innerSchema)/*" />
    </xsl:apply-templates>
  </xsl:template>

  <xsl:template match="element[@type='date']" mode="metadata">
    <xsl:apply-templates select="." mode="enclosing">
      <xsl:with-param name="class" select="'MCRMetaISO8601Date'" />
    </xsl:apply-templates>
    <xsl:apply-templates select="." mode="inner">
      <xsl:with-param name="class" select="'MCRMetaISO8601Date'" />
      <xsl:with-param name="containsText" select="true()" />
      <xsl:with-param name="textFormat" select="'mycoreDate'" />
    </xsl:apply-templates>
  </xsl:template>

  <xsl:template match="element[@type='number']" mode="metadata">
    <xsl:apply-templates select="." mode="enclosing">
      <xsl:with-param name="class" select="'MCRMetaNumber'" />
    </xsl:apply-templates>
    <xsl:apply-templates select="." mode="inner">
      <xsl:with-param name="class" select="'MCRMetaNumber'" />
      <xsl:with-param name="containsText" select="true()" />
      <xsl:with-param name="textFormat" select="'mycoreNumber'" />
    </xsl:apply-templates>
  </xsl:template>

  <xsl:template match="element[@type='xml']" mode="metadata">
    <xsl:apply-templates select="." mode="enclosing">
      <xsl:with-param name="class" select="'MCRMetaXML'" />
    </xsl:apply-templates>
    <xsl:apply-templates select="." mode="inner">
      <xsl:with-param name="class" select="'MCRMetaXML'" />
      <xsl:with-param name="complexType" select="xs:*" />
    </xsl:apply-templates>
  </xsl:template>

  <xsl:template match="element[@type='historydate']" mode="metadata">
    <xsl:apply-templates select="." mode="enclosing">
      <xsl:with-param name="class" select="'MCRMetaHistoryDate'" />
    </xsl:apply-templates>
    <xsl:variable name="innerSchema">
      <xs:sequence>
        <xs:element maxOccurs="unbounded" minOccurs="1" name="text">
          <xs:complexType>
            <xs:simpleContent>
              <xs:extension base="xs:string">
                <xs:attribute use="optional" ref="xml:lang" />
              </xs:extension>
            </xs:simpleContent>
          </xs:complexType>
        </xs:element>
        <xs:element maxOccurs="1" minOccurs="0" type="xs:string" name="calendar" />
        <xs:element maxOccurs="1" minOccurs="0" type="xs:integer" name="ivon" />
        <xs:element maxOccurs="1" minOccurs="0" type="xs:string" name="von" />
        <xs:element maxOccurs="1" minOccurs="0" type="xs:integer" name="ibis" />
        <xs:element maxOccurs="1" minOccurs="0" type="xs:string" name="bis" />
      </xs:sequence>
    </xsl:variable>
    <xsl:apply-templates select="." mode="inner">
      <xsl:with-param name="class" select="'MCRMetaHistoryDate'" />
      <xsl:with-param name="complexType" select="xalan:nodeset($innerSchema)/*" />
    </xsl:apply-templates>
  </xsl:template>

  <xsl:template match="element" mode="types">
    <xsl:choose>
      <xsl:when test="type">
        <xs:attribute name="type" use="required">
          <xs:simpleType>
            <xs:restriction base="xs:string">
              <xsl:for-each select="type/@name">
                <xs:enumeration value="{.}" />
              </xsl:for-each>
            </xs:restriction>
          </xs:simpleType>
        </xs:attribute>
      </xsl:when>
      <xsl:otherwise>
        <xs:attribute name="type" type="xs:string" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>