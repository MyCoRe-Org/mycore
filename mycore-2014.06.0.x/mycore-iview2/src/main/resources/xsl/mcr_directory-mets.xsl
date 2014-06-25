<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:mets="http://www.loc.gov/METS/" xmlns:xalan="http://xml.apache.org/xalan" version="1.0">
  <xsl:output method="xml" encoding="utf-8" />
  <xsl:param name="MCR.Module-iview2.SupportedContentTypes" />
  <xsl:variable name="lower">abcdefghijklmnopqrstuvwxyz</xsl:variable>
  <xsl:variable name="upper">ABCDEFGHIJKLMNOPQRSTUVWXYZ</xsl:variable>

  <xsl:template match="/mcr_directory">
    <xsl:comment>Start -/mcr_directory (mcr_directory-mets.xsl)</xsl:comment>
    <mets:mets xmlns:mods="http://www.loc.gov/mods/v3" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:zvdd="http://zvdd.gdz-cms.de/"
      xsi:schemaLocation="http://www.loc.gov/METS/ http://www.loc.gov/mets/mets.xsd http://www.loc.gov/mods/v3 http://www.loc.gov/standards/mods/v3/mods-3-4.xsd">
      <mets:dmdSec ID="dmd01">
        <mets:mdWrap MDTYPE="MODS">
          <mets:xmlData>
            <mods:mods>
              <mods:titleInfo>
                <mods:title><xsl:value-of select="ownerID" /> Generic Mets Structure</mods:title>
              </mods:titleInfo>
              <mods:typeOfResource collection="yes">still image</mods:typeOfResource>
              <mods:originInfo>
                <mods:place>
                  <mods:placeTerm type="text">Germany, Jena(needs to be set)</mods:placeTerm>
                </mods:place>
              </mods:originInfo>
            </mods:mods>
          </mets:xmlData>
        </mets:mdWrap>
      </mets:dmdSec>
      <xsl:variable name="structuremap">
        <xsl:apply-templates select="children" mode="structMap" />
      </xsl:variable>
      <mets:fileSec>
        <mets:fileGrp USE="DEFAULT">
          <xsl:apply-templates select="xalan:nodeset($structuremap)" mode="fileGrp">
            <xsl:with-param name="offset">0</xsl:with-param>
          </xsl:apply-templates>
        </mets:fileGrp>
      </mets:fileSec>
      <mets:structMap TYPE="LOGICAL">
        <mets:div TYPE="list" LABEL="Liste">
          <xsl:copy-of select="$structuremap" />
        </mets:div>
      </mets:structMap>
    </mets:mets>
    <xsl:comment>End -/mcr_directory (mcr_directory-mets.xsl)</xsl:comment>
  </xsl:template>

  <xsl:template match="children" mode="structMap">
    <xsl:param name="path" />
    <xsl:for-each select="child">
      <xsl:sort select="name" />
      <xsl:variable name="fileType">
        <xsl:call-template name="lastIndexOf">
          <xsl:with-param name="string" select="name" />
          <xsl:with-param name="char" select="'.'" />
        </xsl:call-template>
      </xsl:variable>
      <xsl:choose>
        <xsl:when test="contains($MCR.Module-iview2.SupportedContentTypes, translate($fileType,$upper,$lower))">
          <xsl:element name="mets:div" namespace="http://www.loc.gov/METS/">
            <xsl:attribute name="TYPE">page</xsl:attribute>
            <xsl:attribute name="DMDID">
              <xsl:choose>
                <xsl:when test="$path = ''">
                  <xsl:value-of select="name" />
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="concat($path, '/', name)" />
                </xsl:otherwise>
              </xsl:choose>
            </xsl:attribute>
            <xsl:if test="$path != ''">
              <xsl:attribute name="LABEL"><xsl:value-of select="name" /></xsl:attribute>
            </xsl:if>
          </xsl:element>
        </xsl:when>
        <xsl:when test="@type = 'directory'">
          <xsl:element name="mets:div" namespace="http://www.loc.gov/METS/">
            <xsl:attribute name="TYPE">chapter</xsl:attribute>
            <xsl:attribute name="LABEL"><xsl:value-of select="name"/></xsl:attribute>
            <xsl:apply-templates select="document(concat('ifs:',/mcr_directory/path,'/',name))/mcr_directory/children" mode="structMap">
              <xsl:with-param name="path" select="substring-after(document(concat('ifs:',/mcr_directory/path,'/',name))/mcr_directory/path, '/')" />
            </xsl:apply-templates>
          </xsl:element>
        </xsl:when>
      </xsl:choose>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="/" mode="fileGrp">
    <xsl:for-each select="//mets:div[@TYPE='page']">
      <xsl:variable name="fileType">
        <xsl:call-template name="lastIndexOf">
          <xsl:with-param name="string" select="@DMDID" />
          <xsl:with-param name="char" select="'.'" />
        </xsl:call-template>
      </xsl:variable>
      <xsl:element name="mets:file">
        <xsl:attribute name="ID"><xsl:value-of select="@DMDID"/></xsl:attribute>
        <xsl:attribute name="MIMETYPE"><xsl:value-of select="concat('image/',$fileType)"/></xsl:attribute>
        <xsl:element name="mets:FLocat">
          <xsl:attribute name="LOCTYPE">URL</xsl:attribute>
          <xsl:attribute name="xlink:href"><xsl:value-of select="@DMDID"/></xsl:attribute>
        </xsl:element>
        <xsl:element name="mets:FLocat">
          <xsl:attribute name="LOCTYPE">OTHER</xsl:attribute>
          <xsl:attribute name="OTHERLOCTYPE">PageNumber</xsl:attribute>
          <xsl:attribute name="xlink:href"><xsl:value-of select="position()-1"/></xsl:attribute>
        </xsl:element>
      </xsl:element>
    </xsl:for-each>
  </xsl:template>
  
  <xsl:template name="lastIndexOf">
    <xsl:param name="string" />
    <xsl:param name="char" />
    <xsl:choose>
      <xsl:when test="contains($string, $char)">
        <xsl:call-template name="lastIndexOf">
          <xsl:with-param name="string" select="substring-after($string, $char)" />
          <xsl:with-param name="char" select="$char" />
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$string" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
</xsl:stylesheet>
