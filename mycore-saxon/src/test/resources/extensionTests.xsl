<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:test="mcr:org.mycore.saxon.MCRTestExtensions">
<xsl:output method="text"/>
  <xsl:template name="xsl:initial-template">
    <xsl:call-template name="test:parameter" />
    <xsl:call-template name="test:returnTypes" />
    <xsl:call-template name="test:signatures" />
    <xsl:call-template name="test:uriResolver" />
  </xsl:template>
  
  <xsl:template name="test:parameter">
    <xsl:if test="not(test:isNone())">
      <xsl:message terminate="yes">
        Error test:isNone()
      </xsl:message>
    </xsl:if>
    <xsl:if test="not(test:isInt(1))">
      <xsl:message terminate="yes">
        Error test:isInt(1)
      </xsl:message>
    </xsl:if>
    <xsl:if test="not(test:isInteger(1))">
      <xsl:message terminate="yes">
        Error test:isInteger(1)
      </xsl:message>
    </xsl:if>
    <xsl:if test="not(test:islong(1))">
      <xsl:message terminate="yes">
        Error test:islong(1)
      </xsl:message>
    </xsl:if>
    <xsl:if test="not(test:isLong(1))">
      <xsl:message terminate="yes">
        Error test:isLong(1)
      </xsl:message>
    </xsl:if>
    <xsl:if test="not(test:isBool(true()))">
      <xsl:message terminate="yes">
        Error test:isBool(true())
      </xsl:message>
    </xsl:if>
    <xsl:if test="not(test:isString('password'))">
      <xsl:message terminate="yes">
        Error test:isString('password')
      </xsl:message>
    </xsl:if>
    <xsl:if test="not(test:isBigInteger(1))">
      <xsl:message terminate="yes">
        Error test:isBigInteger(1)
      </xsl:message>
    </xsl:if>
    <xsl:if test="not(test:isBigDecimal(1.1))">
      <xsl:message terminate="yes">
        Error test:isBigDecimal(1.1)
      </xsl:message>
    </xsl:if>
    <xsl:variable name="xsl" select="document('extensionTests.xsl')" />
    <xsl:if test="not(test:isNode($xsl))">
      <xsl:message terminate="yes">
        Error test:isNode($xsl)
      </xsl:message>
    </xsl:if>
    <xsl:if test="not(test:isNodeList($xsl//xsl:if))">
      <xsl:message terminate="yes">
        Error test:isNodeList($xsl)
      </xsl:message>
    </xsl:if>
  </xsl:template>
  
  <xsl:template name="test:returnTypes">
    <xsl:if test="not(test:getInt() = 1)">
      <xsl:message terminate="yes">
        Error test:getInt()
      </xsl:message>
    </xsl:if>
    <xsl:if test="not(test:getInteger() = 1)">
      <xsl:message terminate="yes">
        Error test:getInteger()
      </xsl:message>
    </xsl:if>
    <xsl:if test="not(test:getlong() = 1)">
      <xsl:message terminate="yes">
        Error test:getlong()
      </xsl:message>
    </xsl:if>
    <xsl:if test="not(test:getLong() = 1)">
      <xsl:message terminate="yes">
        Error test:getLong()
      </xsl:message>
    </xsl:if>
    <xsl:if test="not(test:getBool() = true())">
      <xsl:message terminate="yes">
        Error test:getBool()
      </xsl:message>
    </xsl:if>
    <xsl:if test="not(test:getString() = 'test')">
      <xsl:message terminate="yes">
        Error test:getString()
      </xsl:message>
    </xsl:if>
    <xsl:if test="not(test:getBigInteger() = 1)">
      <xsl:message terminate="yes">
        Error test:getBigInteger()
      </xsl:message>
    </xsl:if>
    <xsl:if test="not(test:getBigDecimal() = 1.0)">
      <xsl:message terminate="yes">
        Error test:getBigDecimal()
      </xsl:message>
    </xsl:if>
    <xsl:if test="not(count(test:getNode()) = 1)">
      <xsl:message terminate="yes">
        Error test:getNode()
      </xsl:message>
    </xsl:if>
    <xsl:if test="not(count(test:getNodeList()) &gt; 0 )">
      <xsl:message terminate="yes">
        Error test:getNodeList()
      </xsl:message>
    </xsl:if>
  </xsl:template>
  
  <xsl:template name="test:signatures">
    <xsl:if test="not(test:concat('password') = 'password')">
      <xsl:message terminate="yes">
        Error test:concat('password')
      </xsl:message>
    </xsl:if>
    <xsl:if test="not(test:concat('J', 'Unit') = 'JUnit')">
      <xsl:message terminate="yes">
        Error test:concat('J', 'Unit')
      </xsl:message>
    </xsl:if>
    <xsl:if test="not(test:concat('J', 'Unit', ' Test') = 'JUnit Test')">
      <xsl:message terminate="yes">
        Error test:concat('J', 'Unit', ' Test')
      </xsl:message>
    </xsl:if>
  </xsl:template>
  
  <xsl:template name="test:uriResolver">
    <xsl:variable name="language" select="document('language:de')" />
    <xsl:if test="not($language//label[lang('en')] = 'German')">
      <xsl:message terminate="yes">
        Error test:uriResolver
      </xsl:message>
    </xsl:if>
    <xsl:if test="not(test:isString($language//label[lang('en')]/@xml:lang)) = true()">
      <xsl:message terminate="yes">
        Error test:parameterConversion
      </xsl:message>
    </xsl:if>
  </xsl:template>
</xsl:stylesheet>