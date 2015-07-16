<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:mods="http://www.loc.gov/mods/v3"
  xmlns:mcrmods="xalan://org.mycore.mods.classification.MCRMODSClassificationSupport" exclude-result-prefixes="mcrmods" version="1.0">

  <xsl:include href="copynodes.xsl" />
  <xsl:include href="coreFunctions.xsl" />

  <xsl:template match="*[@authority or @authorityURI]">
    <xsl:copy>
      <xsl:variable name="classNodes" select="mcrmods:getMCRClassNodes(.)" />
      <xsl:apply-templates select="$classNodes/@*|@*|node()" />
    </xsl:copy>
  </xsl:template>

  <xsl:template match="mods:mods">
    <xsl:copy>
      <xsl:apply-templates select="@*" />
      <xsl:apply-templates
        select="*[not( (local-name()='name' and @ID and @type='corporate') or (starts-with(@xlink:href,'#')) or (starts-with(mods:physicalLocation/@xlink:href,'#')) )]" />
      <xsl:for-each select="mods:name[@ID and @type='corporate']">
        <noteLocationCorp>
          <xsl:variable name="ID" select="@ID" />
          <xsl:apply-templates select=".|../*[@xlink:href=concat('#',$ID) or mods:physicalLocation/@xlink:href=concat('#',$ID)]" />
        </noteLocationCorp>
      </xsl:for-each>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="mods:name[@type='personal' or @authorityURI='http://d-nb.info/' or (@type='corporate' and not (@ID))]">
    <xsl:copy>
      <xsl:apply-templates select="@*[name()!='type']" />
      <xsl:attribute name="type">
        <xsl:choose>
          <xsl:when test="@type='corporate'">
            <xsl:value-of select="'corporate'" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="'personal'" />
          </xsl:otherwise>
        </xsl:choose>
        
      </xsl:attribute>
      <xsl:if test="@valueURI">
        <xsl:attribute name="editor.output">
          <xsl:choose>
            <xsl:when test="mods:namePart[@type='family'] and mods:namePart[@type='given']">
              <xsl:value-of select="concat(mods:namePart[@type='family'],', ',mods:namePart[@type='given'])" />
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="*" />
            </xsl:otherwise>
          </xsl:choose>
        </xsl:attribute>
      </xsl:if>
      <xsl:message>
        count:
        <xsl:value-of select="count(*[not((local-name()='namePart'))])" />
      </xsl:message>
      <nameOrPND>
        <xsl:choose>
          <xsl:when test="@authorityURI='http://d-nb.info/'">
            <xsl:attribute name="editor.output">
              <xsl:value-of select="mods:displayForm" />
            </xsl:attribute>
            <xsl:value-of select="substring-after(@valueURI, 'http://d-nb.info/gnd/')" />
          </xsl:when>
          <xsl:when test="mods:displayForm">
            <xsl:value-of select="mods:displayForm" />
          </xsl:when>
          <xsl:when test="mods:namePart[@type='family'] and mods:namePart[@type='given']">
            <xsl:value-of select="concat(mods:namePart[@type='family'],', ',mods:namePart[@type='given'])" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="*" />
          </xsl:otherwise>
        </xsl:choose>
      </nameOrPND>
      <xsl:apply-templates select="*[not((local-name()='namePart'))]" />
    </xsl:copy>
  </xsl:template>

  <xsl:template match="mods:namePart[@type='date']">
    <mods:namePartDate>
      <xsl:apply-templates select="@*|node()" />
    </mods:namePartDate>
  </xsl:template>

  <xsl:template match="mods:name[@ID]">
    <xsl:copy>
      <xsl:variable name="classNodes" select="mcrmods:getMCRClassNodes(.)" />
      <xsl:apply-templates select='$classNodes/@*|@*' />
      <xsl:if test="count($classNodes/@*)&gt;0">
        <xsl:attribute name="editor.output">
          <xsl:variable name="classlink" select="mcrmods:getClassCategLink(.)" />
          <xsl:choose>
            <xsl:when test="string-length($classlink) &gt; 0">
              <xsl:for-each select="document($classlink)/mycoreclass/categories/category">
                <xsl:variable name="selectLang">
                   <xsl:call-template name="selectLang">
                     <xsl:with-param name="nodes" select="./label" />
                   </xsl:call-template>
                </xsl:variable>
                <xsl:value-of select="./label[lang($selectLang)]/@text" />
              </xsl:for-each>
            </xsl:when>
            <xsl:otherwise>
              <xsl:choose>
                <xsl:when test="@valueURI">
                  <xsl:value-of select="@valueURI" />
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="'bereits gewaehlt'" />
                </xsl:otherwise>
              </xsl:choose>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:attribute>
      </xsl:if>
      <xsl:apply-templates select="*" />
    </xsl:copy>
  </xsl:template>

  <xsl:template match="parent">
    <xsl:copy>
      <xsl:apply-templates select="@*" />
      <xsl:attribute name="editor.parentName">
        <xsl:variable name="parentMods"
        select="document(concat('mcrobject:',@xlink:href))/mycoreobject/metadata/def.modsContainer/modsContainer/mods:mods" />
        <xsl:choose>
          <xsl:when test="$parentMods/mods:titleInfo/mods:title">
            <xsl:value-of select="$parentMods/mods:titleInfo/mods:title" />
          </xsl:when>
          <xsl:when test="$parentMods/mods:name[@type='conference']">
            <xsl:for-each select="$parentMods/mods:name[@type='conference']">
              <xsl:for-each select="mods:namePart[not(@type)]">
                <xsl:choose>
                  <xsl:when test="position()=1">
                    <xsl:value-of select="." />
                  </xsl:when>
                  <xsl:otherwise>
                    <xsl:value-of select="concat(' – ',.)" />
                  </xsl:otherwise>
                </xsl:choose>
              </xsl:for-each>
              <xsl:if test="mods:namePart[@type='date']">
                <xsl:value-of select="', '" />
                <xsl:value-of select="mods:namePart[@type='date']" />
              </xsl:if>
              <xsl:for-each select="mods:affiliation">
                <xsl:value-of select="concat(', ',.)" />
              </xsl:for-each>
            </xsl:for-each>
          </xsl:when>
        </xsl:choose>
      </xsl:attribute>
    </xsl:copy>
  </xsl:template>

  <!-- workaround for bug #2595855 -->
  <xsl:template match="@xml:lang">
    <xsl:attribute name="lang">
      <xsl:value-of select="." />
    </xsl:attribute>
  </xsl:template>

  <!-- workaround for bug #3479633 -->
  <xsl:template match="@transliteration">
    <xsl:attribute name="transliteration">
      <xsl:value-of select="'html'" />
    </xsl:attribute>
  </xsl:template>

</xsl:stylesheet>