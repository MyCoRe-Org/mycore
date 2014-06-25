<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:mcr="xalan://org.mycore.common.xml.MCRXMLFunctions" xmlns:mets="http://www.loc.gov/METS/" xmlns:decoder="xalan://java.net.URLDecoder"
  xmlns:iview2="xalan://org.mycore.iview2.frontend.MCRIView2XSLFunctions" xmlns:str="http://exslt.org/strings" extension-element-prefixes="str" version="1.0">
  <xsl:output method="text" media-type="application/x-json" />
  <xsl:param name="derivateID" />
  <xsl:param name="objectID" />

  <!-- add keys for every mets:file -->
  <xsl:key name="fileref" match="mets:file" use="@ID" />
  <!-- add keys for every mets:div -->
  <xsl:key name="divref" match="mets:div" use="@ID" />

  <xsl:template match="/mets:mets">
    <!-- delivers outer json bracket -->
    <xsl:text>{identifier: "id",label: "name",items: [</xsl:text>
    <xsl:apply-templates select="mets:structMap[@TYPE='LOGICAL']" />
    <xsl:text>&#xA;]}</xsl:text>
  </xsl:template>

  <xsl:template match="mets:structMap[@TYPE='LOGICAL']">
    <xsl:for-each select="mets:div">
      <xsl:if test="position()!=1">
        <xsl:value-of select="','" />
      </xsl:if>
      <xsl:apply-templates select="." mode="logical" />
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="mets:div" mode="logical">
    <xsl:variable name="logID" select="@ID" />
    <xsl:choose>
      <xsl:when test="@TYPE != 'page'">
        <xsl:text>&#xA;{id: "</xsl:text>
        <xsl:value-of select="@ID" />
        <xsl:text>", name:"</xsl:text>
        <xsl:value-of select="@LABEL" />
        <xsl:text>", structureType:"</xsl:text>
        <xsl:value-of select="@TYPE" />
        <xsl:text>", type: "category"</xsl:text>
        <xsl:text>, children:[ </xsl:text>
        <xsl:for-each select="/mets:mets/mets:structLink/mets:smLink[@xlink:from = $logID] | mets:div">
          <xsl:sort select="iview2:getOrder(.)" data-type="number" />
          <xsl:if test="position()!=1">
            <xsl:value-of select="','" />
          </xsl:if>
          <xsl:apply-templates select="." mode="logical">
            <xsl:with-param name="logID" select="$logID" />
          </xsl:apply-templates>
        </xsl:for-each>
        <xsl:text>&#xA; ]</xsl:text>
        <xsl:text>}</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates select="/mets:mets/mets:structLink/mets:smLink[@xlink:from = $logID]">
          <xsl:with-param name="label" select="@LABEL" />
        </xsl:apply-templates>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="mets:smLink" mode="logical">
    <!-- linked files -->
    <xsl:apply-templates select="." />
  </xsl:template>

  <xsl:template match="mets:smLink">
    <xsl:param name="label" />
    <xsl:variable name="target" select="@xlink:to" />
    <xsl:variable name="physical" select="key('divref', @xlink:to)" />
    <xsl:variable name="fptr" select="$physical/mets:fptr" />
    <xsl:variable name="file" select="key('fileref', $fptr/@FILEID)" />
    <xsl:text>&#xA;   {id: "</xsl:text>
    <xsl:value-of select="$file/@ID" />
    <xsl:text>", name:"</xsl:text>
    <xsl:choose>
      <xsl:when test="$label">
        <xsl:call-template name="encode-value">
          <xsl:with-param name="value" select="$label" />
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:variable name="fileName">
          <xsl:call-template name="substring-after-last">
            <xsl:with-param name="string" select="decoder:decode($file/mets:FLocat/@xlink:href,'UTF-8')" />
            <xsl:with-param name="delimiter" select="'/'" />
          </xsl:call-template>
        </xsl:variable>
        <xsl:call-template name="encode-value">
          <xsl:with-param name="value" select="$fileName" />
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:text>", path:"</xsl:text>
    <xsl:call-template name="encode-value">
      <xsl:with-param name="value" select="decoder:decode($file/mets:FLocat/@xlink:href,'UTF-8')" />
    </xsl:call-template>
    <xsl:text>", orderLabel:"</xsl:text>
    <xsl:value-of select="$physical/@ORDERLABEL" />
    <xsl:text>", structType: "page", type: "item" }</xsl:text>
    <!-- { id: '05.tif', name:'05.tif',orderLabel:'iii', structureType:'page', type:'item' } -->
  </xsl:template>

  <xsl:template name="substring-after-last">
    <xsl:param name="string" />
    <xsl:param name="delimiter" />
    <xsl:choose>
      <xsl:when test="contains($string, $delimiter)">
        <xsl:call-template name="substring-after-last">
          <xsl:with-param name="string" select="substring-after($string, $delimiter)" />
          <xsl:with-param name="delimiter" select="$delimiter" />
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$string" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- JSON templates -->
  <xsl:variable name="jsonTemp">
    <replace src="&quot;" dst="\&quot;" />
    <replace src="\" dst="\\" />
    <replace src="&#xA;" dst="\n" />
    <replace src="&#xD;" dst="\r" />
    <replace src="&#x9;" dst="\t" />
    <replace src="\n" dst="\n" />
    <replace src="\r" dst="\r" />
    <replace src="\t" dst="\t" />
  </xsl:variable>
  <xsl:variable name="jsonSearch" select="xalan:nodeset($jsonTemp)" />
  <xsl:template name="replace-string">
    <xsl:param name="input" />
    <xsl:param name="src" />
    <xsl:param name="dst" />
    <xsl:choose>
      <xsl:when test="contains($input, $src)">
        <xsl:value-of select="concat(substring-before($input, $src), $dst)" />
        <xsl:call-template name="replace-string">
          <xsl:with-param name="input" select="substring-after($input, $src)" />
          <xsl:with-param name="src" select="$src" />
          <xsl:with-param name="dst" select="$dst" />
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$input" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="encode">
    <xsl:param name="input" />
    <xsl:param name="index" select="1" />
    <xsl:variable name="text">
      <xsl:call-template name="replace-string">
        <xsl:with-param name="input" select="$input" />
        <xsl:with-param name="src" select="$jsonSearch/replace[$index]/@src" />
        <xsl:with-param name="dst" select="$jsonSearch/replace[$index]/@dst" />
      </xsl:call-template>
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="$index &lt; count($jsonSearch/mcr:replace)">
        <xsl:call-template name="encode">
          <xsl:with-param name="input" select="$text" />
          <xsl:with-param name="index" select="$index + 1" />
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$text" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="encode-value">
    <xsl:param name="value" />
    <xsl:choose>
      <xsl:when
        test="(string(number($value)) = 'NaN' or (substring($value , string-length($value), 1) = '.') or (substring($value, 1, 1) = '0') and not($value = '0')) and not($value = 'false') and not($value = 'true') and not($value = 'null')">
        <xsl:call-template name="encode">
          <xsl:with-param name="input" select="$value" />
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="normalize-space($value)" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
</xsl:stylesheet>
