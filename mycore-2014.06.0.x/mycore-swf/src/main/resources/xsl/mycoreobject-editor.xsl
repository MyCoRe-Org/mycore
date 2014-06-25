<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:acl="xalan://org.mycore.access.MCRAccessManager">
  
  <xsl:output method="xml" encoding="UTF-8"/>
  
  <xsl:variable name="newline">
    <xsl:text>
    </xsl:text>
  </xsl:variable>
  
  <xsl:attribute-set name="tag">
    <xsl:attribute name="class">
      <xsl:value-of select="./@class"/>
    </xsl:attribute>
    <xsl:attribute name="heritable">
      <xsl:value-of select="./@heritable"/>
    </xsl:attribute>
    <xsl:attribute name="notinherit">
      <xsl:value-of select="./@notinherit"/>
    </xsl:attribute>
  </xsl:attribute-set>
  
  <xsl:template match="/mycoreobject">
    <mycoreobject>
      <xsl:copy-of select="@ID"/>
      <xsl:copy-of select="@label"/>
      <xsl:copy-of select="@version"/>
      <xsl:copy-of select="@xsi:noNamespaceSchemaLocation"/>
      <!-- check the WRITEDB permission -->
      <xsl:if test="acl:checkPermission(@ID,'writedb')">
        <structure>
          <xsl:copy-of select="structure/parents"/>
        </structure>
        <metadata xml:lang="de">
          <xsl:for-each select="metadata/*">
            <xsl:for-each select=".">
              <xsl:if test="./*/@inherited = '0'">
                <xsl:copy use-attribute-sets="tag">
                  <xsl:for-each select="*">
                    <xsl:if test="@inherited = '0'">
                      <xsl:copy-of select="."/>
                      <xsl:value-of select="$newline"/>
                    </xsl:if>
                  </xsl:for-each>
                </xsl:copy>
                <xsl:value-of select="$newline"/>
              </xsl:if>
            </xsl:for-each>
          </xsl:for-each>
        </metadata>
        <xsl:copy-of select="service"/>
      </xsl:if>
    </mycoreobject>
  </xsl:template>
  
</xsl:stylesheet>