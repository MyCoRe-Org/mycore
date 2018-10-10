<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ This file is part of ***  M y C o R e  ***
  ~ See http://www.mycore.de/ for details.
  ~
  ~ MyCoRe is free software: you can redistribute it and/or modify
  ~ it under the terms of the GNU General Public License as published by
  ~ the Free Software Foundation, either version 3 of the License, or
  ~ (at your option) any later version.
  ~
  ~ MyCoRe is distributed in the hope that it will be useful,
  ~ but WITHOUT ANY WARRANTY; without even the implied warranty of
  ~ MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  ~ GNU General Public License for more details.
  ~
  ~ You should have received a copy of the GNU General Public License
  ~ along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
  -->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fn="http://www.w3.org/2005/xpath-functions"
                xmlns:mods="http://www.loc.gov/mods/v3"
                xmlns:xlink="http://www.w3.org/1999/xlink"
                version="3.0"
>

  <xsl:param name="WebApplicationBaseURL" />
  <xsl:param name="MCR.Module-iview2.SupportedContentTypes" />

  <!--

  Define a transformer
  MCR.ContentTransformer.schemaOrg.Class=org.mycore.common.content.transformer.MCRXSLTransformer
  MCR.ContentTransformer.schemaOrg.Stylesheet=xsl/mods2schemaorg.xsl
  MCR.ContentTransformer.schemaOrg.TransformerFactoryClass=net.sf.saxon.TransformerFactoryImpl

  Prevent the layout for the transformer
  MCR.LayoutTransformerFactory.Default.Ignore=%MCR.LayoutTransformerFactory.Default.Ignore%,schemaOrg

  Include the <script> tag in the page
  <xsl:copy-of select="document(concat('xslTransform:schemaOrg:mcrobject:', $objectID))" />

  -->

  <xsl:include href="xslInclude:schemaorg" />

  <xsl:template match="/">
    <xsl:element name="script">
      <xsl:attribute name="type">application/ld+json</xsl:attribute>
      <xsl:variable name="xmlNode">
        <fn:array>
          <xsl:apply-templates select="mycoreobject/metadata/def.modsContainer/modsContainer/mods:mods" />
        </fn:array>
      </xsl:variable>
      <xsl:copy-of select="fn:xml-to-json($xmlNode)" />
      <!--<xsl:copy-of select="$xmlNode" />-->
    </xsl:element>
  </xsl:template>

  <xsl:template match="mods:mods">
    <!-- do nothing -->
  </xsl:template>

  <xsl:template match="mods:mods[mods:classification/@authorityURI='http://schema.org/']">
    <xsl:call-template name="mods2schemaOrg" />
  </xsl:template>

  <xsl:template name="mods2schemaOrg">
    <xsl:param name="type">
      <!-- detect type if now supplied -->
      <xsl:choose>
        <xsl:when test="count(mods:classification[@authorityURI='http://schema.org/'])&gt;0">
          <xsl:for-each select="mods:classification[@authorityURI='http://schema.org/']">
            <xsl:if test="position() &gt; 1">
              <xsl:value-of select="','" />
            </xsl:if>
            <xsl:value-of select="tokenize(@valueURI, '/')[last()]" />
          </xsl:for-each>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="'CreativeWork'" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:param>
    <xsl:param name="context" select="'http://schema.org/'" />

    <fn:map>
      <!-- output type  -->
      <xsl:choose>
        <xsl:when test="contains($type, ',')">
          <fn:array key="@type">
            <xsl:for-each select="tokenize($type, ',')">
              <fn:string>
                <xsl:value-of select="." />
              </fn:string>
            </xsl:for-each>
          </fn:array>
        </xsl:when>
        <xsl:otherwise>
          <fn:string key="@type">
            <xsl:value-of select="$type" />
          </fn:string>
        </xsl:otherwise>
      </xsl:choose>

      <fn:string key="@context">
        <xsl:value-of select="$context" />
      </fn:string>

      <!-- name -->
      <xsl:if test="mods:titleInfo[1]/mods:title">
        <fn:string key="name">
          <xsl:value-of select="mods:titleInfo[1]/mods:title" />
        </fn:string>
      </xsl:if>

      <!-- headline & alternativeHeadline-->
      <xsl:variable name="firstTitle" select="mods:titleInfo[1]" />
      <xsl:if test="$firstTitle/mods:title">
        <fn:string key="headline">
          <xsl:call-template name="buildHeadline">
            <xsl:with-param name="titleInfo" select="$firstTitle" />
          </xsl:call-template>
        </fn:string>
      </xsl:if>
      <xsl:if test="count(mods:titleInfo) &gt;0 ">
        <fn:array key="alternativeHeadline">
          <xsl:for-each select="mods:titleInfo">
            <xsl:if test="not(position()=1)">
              <fn:string>
                <xsl:call-template name="buildHeadline">
                  <xsl:with-param name="titleInfo" select="." />
                </xsl:call-template>
              </fn:string>
            </xsl:if>
          </xsl:for-each>
        </fn:array>
      </xsl:if>

      <!-- identifiers -->
      <xsl:if test="count(mods:identifier) &gt; 0">
        <!-- output all identifiers -->
        <fn:array key="identifier">
          <xsl:for-each select="mods:identifier">
            <fn:map>
              <fn:string key="@type">PropertyValue</fn:string>
              <fn:string key="propertyID">
                <xsl:value-of select="@type" />
              </fn:string>
              <fn:string key="value">
                <xsl:value-of select="text()" />
              </fn:string>
            </fn:map>
          </xsl:for-each>
        </fn:array>
      </xsl:if>

      <!-- author -->
      <xsl:variable name="authors" select="mods:name[@type='personal' and contains(mods:role/mods:roleTerm, 'aut')]" />
      <xsl:if test="count($authors) &gt; 0">
        <fn:array key="author">
          <xsl:for-each select="$authors">
            <xsl:call-template name="person">
              <xsl:with-param name="modsName" select="." />
            </xsl:call-template>
          </xsl:for-each>
        </fn:array>
      </xsl:if>

      <!-- sponsor -->
      <xsl:variable name="sponsors" select="mods:name[@type='personal' and contains(mods:role/mods:roleTerm, 'fnd')]" />
      <xsl:if test="count($sponsors) &gt; 0">
        <fn:array key="sponsor">
          <xsl:for-each select="$sponsors">
            <xsl:call-template name="person">
              <xsl:with-param name="modsName" select="." />
            </xsl:call-template>
          </xsl:for-each>
        </fn:array>
      </xsl:if>

      <!-- datePublished -->
      <xsl:if test="mods:originInfo[@eventType='publication']/mods:dateIssued[@encoding='w3cdtf']">
        <fn:string key="datePublished">
          <xsl:value-of select="mods:originInfo[@eventType='publication']/mods:dateIssued[@encoding='w3cdtf']" />
        </fn:string>
      </xsl:if>

      <!-- dateModified -->
      <xsl:if test="mods:originInfo[@eventType='update']/mods:dateModified[@encoding='w3cdtf']">
        <fn:string key="dateModified">
          <xsl:value-of select="mods:originInfo[@eventType='update']/mods:dateModified[@encoding='w3cdtf']" />
        </fn:string>
      </xsl:if>

      <!-- dateCreated -->
      <xsl:if test="mods:originInfo[@eventType='creation']/mods:dateModified[@encoding='w3cdtf' and not(@type)]">
        <fn:string key="dateModified">
          <xsl:value-of
            select="mods:originInfo[@eventType='creation']/mods:dateModified[@encoding='w3cdtf' and not(@type)]" />
        </fn:string>
      </xsl:if>

      <!-- publisher -->
      <xsl:if test="mods:originInfo[@eventType='publication']/mods:publisher">
        <fn:map key="publisher">
          <fn:string key="@type">Person</fn:string>
          <fn:string key="name">
            <xsl:value-of select="mods:originInfo[@eventType='publication']/mods:publisher" />
          </fn:string>
        </fn:map>

        <!-- mainEntityOfPage -->
        <fn:string key="mainEntityOfPage">
          <xsl:value-of select="concat($WebApplicationBaseURL, 'receive/', /mycoreobject/@ID)" />
        </fn:string>
      </xsl:if>

      <!-- description -->
      <xsl:if test="mods:abstract">
        <fn:array key="description">
          <xsl:for-each select="mods:abstract">
            <fn:string>
              <xsl:value-of select="." />
            </fn:string>
          </xsl:for-each>
        </fn:array>
      </xsl:if>

      <!-- provider -->
      <xsl:variable name="provider" select="mods:name[@type='personal' and contains(mods:role/mods:roleTerm, 'orm')]" />
      <xsl:if test="count($provider) &gt; 0">
        <fn:array key="provider">
          <xsl:for-each select="$provider">
            <xsl:call-template name="person">
              <xsl:with-param name="modsName" select="." />
            </xsl:call-template>
          </xsl:for-each>
        </fn:array>
      </xsl:if>

      <!-- thumbnailUrl & image-->
      <xsl:variable name="imgSrc">
        <xsl:call-template name="displayPreviewURL" />
      </xsl:variable>
      <xsl:if test="string-length($imgSrc)&gt;0">
        <fn:string key="thumbnailUrl">
          <xsl:value-of select="$imgSrc" />
        </fn:string>
        <fn:string key="image">
          <xsl:value-of select="$imgSrc" />
        </fn:string>
      </xsl:if>

      <xsl:apply-templates select="*" mode="extension" />

      <!-- child files -->
      <xsl:call-template name="addFiles" />

      <!-- Article Stuff-->
      <xsl:if test="contains($type, 'Article')">
        <!-- backstory -->
        <xsl:if test="count(mods:abstract) &gt; 0">
          <fn:array key="backstory">
            <xsl:for-each select="mods:abstract">
              <fn:string>
                <xsl:value-of select="." />
              </fn:string>
            </xsl:for-each>
          </fn:array>
        </xsl:if>
      </xsl:if>

      <!-- Book Stuff-->
      <xsl:if test="contains($type, 'Book')">
        <!-- isbn -->
        <xsl:if test="count(mods:identifier[@type='isbn'])&gt;0">
          <fn:array key="isbn">
            <xsl:for-each select="mods:identifier[@type='isbn']">
              <fn:string>
                <xsl:value-of select="." />
              </fn:string>
            </xsl:for-each>
          </fn:array>
        </xsl:if>

        <!-- illustrator -->
        <xsl:variable name="illustrators"
                      select="mods:name[@type='personal' and contains(mods:role/mods:roleTerm, 'ill')]" />
        <xsl:if test="count($illustrators) &gt; 0">
          <fn:array key="illustrator">
            <xsl:for-each select="$illustrators">
              <xsl:call-template name="person">
                <xsl:with-param name="modsName" select="." />
              </xsl:call-template>
            </xsl:for-each>
          </fn:array>
        </xsl:if>

        <!-- bookEdition -->
        <xsl:if test="mods:originInfo[@eventType='publication']/mods:edition">
          <fn:string key="bookEdition">
            <xsl:value-of select="mods:originInfo[@eventType='publication']/mods:edition" />
          </fn:string>
        </xsl:if>
      </xsl:if>

      <!-- thesis -->
      <xsl:if test="contains($type, 'Thesis')">
        <xsl:variable name="inSupport">
          <xsl:choose>
            <xsl:when test="mods:genre[contains(@valueURI, '#bachelor_thesis')]">
              <xsl:value-of select="'Bachelor'" />
            </xsl:when>
            <xsl:when test="mods:genre[contains(@valueURI, '#master_thesis')]">
              <xsl:value-of select="'Master'" />
            </xsl:when>
            <xsl:when test="mods:genre[contains(@valueURI, '#dissertation')]">
              <xsl:value-of select="'doctor'" />
            </xsl:when>
            <xsl:when test="mods:genre[contains(@valueURI, '#diploma_thesis')]">
              <xsl:value-of select="'diploma'" />
            </xsl:when>
            <xsl:when test="mods:genre[contains(@valueURI, '#magister_thesis')]">
              <xsl:value-of select="'Master'" />
            </xsl:when>
          </xsl:choose>
        </xsl:variable>
        <xsl:if test="string-length($inSupport)&gt;0">
          <fn:string key="inSupportOf">
            <xsl:value-of select="$inSupport" />
          </fn:string>
        </xsl:if>
      </xsl:if>
    </fn:map>
  </xsl:template>

  <xsl:template match="*" mode="extension">
    <!-- nothing -->
  </xsl:template>

  <!-- Used to build the headline and alternative headline for all mods objects -->
  <xsl:template name="buildHeadline">
    <xsl:param name="titleInfo" />
    <xsl:if test="$titleInfo/mods:nonSort">
      <xsl:value-of select="concat($titleInfo/mods:nonSort, ' ')" />
    </xsl:if>
    <xsl:value-of select="$titleInfo/mods:title" />
    <xsl:if test="$titleInfo/mods:subTitle">
      <xsl:value-of select="concat(' : ', $titleInfo/mods:subTitle)" />
    </xsl:if>
  </xsl:template>

  <!-- creates a person json object without a key -->
  <xsl:template name="person">
    <xsl:param name="modsName" />

    <fn:map>
      <fn:string key="@type">
        <xsl:value-of select="'Person'" />
      </fn:string>

      <!-- name -->
      <xsl:choose>
        <xsl:when test="$modsName/mods:namePart[@type='given'] and $modsName/mods:namePart[@type='family']">
          <xsl:if test="$modsName/mods:namePart[@type='termsOfAddress']">
            <fn:string key="honorificPrefix">
              <xsl:value-of select="$modsName/mods:namePart[@type='termsOfAddress']" />
            </fn:string>
          </xsl:if>
          <fn:string key="givenName">
            <xsl:value-of select="$modsName/mods:namePart[@type='given']" />
          </fn:string>
          <fn:string key="familyName">
            <xsl:value-of select="$modsName/mods:namePart[@type='family']" />
          </fn:string>
        </xsl:when>
      </xsl:choose>

      <!-- always use the displayName for name-->
      <xsl:if test="$modsName/mods:displayForm">
        <fn:string key="name">
          <xsl:value-of select="$modsName/mods:displayForm" />
        </fn:string>
      </xsl:if>

      <!-- identifiers -->
      <xsl:if test="count($modsName/mods:nameIdentifier) &gt; 0">
        <!-- output all identifiers -->
        <fn:array key="identifier">
          <xsl:for-each select="$modsName/mods:nameIdentifier">
            <fn:string>
              <xsl:call-template name="displayNameIdentifier">
                <xsl:with-param name="nameIdentifier" select="." />
              </xsl:call-template>
            </fn:string>
            <fn:map>
              <fn:string key="@type">PropertyValue</fn:string>
              <fn:string key="propertyID">
                <xsl:value-of select="@type" />
              </fn:string>
              <fn:string key="value">
                <xsl:value-of select="text()" />
              </fn:string>
            </fn:map>
          </xsl:for-each>
        </fn:array>
      </xsl:if>
    </fn:map>
  </xsl:template>

  <!-- displays nameIdentifier as url -->
  <xsl:template name="displayNameIdentifier">
    <xsl:param name="nameIdentifier" />

    <xsl:variable name="type" select="$nameIdentifier/@type" />
    <xsl:variable name="identifier" select="$nameIdentifier/text()" />

    <xsl:choose>
      <xsl:when test="$type='gnd'">
        <xsl:value-of select="concat('https://d-nb.info/gnd/', $identifier)" />
      </xsl:when>
      <xsl:when test="$type='viaf'">
        <xsl:value-of select="concat('https://viaf.org/viaf/', $identifier, '/')" />
      </xsl:when>
      <xsl:when test="$type='orcid'">
        <xsl:value-of select="concat('https://orcid.org/', $identifier)" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$identifier" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="addFiles">
    <xsl:variable name="files">
      <xsl:for-each select="/mycoreobject/structure/derobjects/derobject">
        <xsl:variable name="derivateID" select="@xlink:href" />
        <xsl:variable name="uri" select="concat('ifs:/',$derivateID)" />
        <xsl:variable name="derivateContent" select="document($uri)/mcr_directory" />
        <xsl:variable name="derivate" select="document(concat('mcrobject:',$derivateID))" />
        <xsl:if test="$derivate/mycorederivate/derivate/@display='true'">
          <xsl:apply-templates select="$derivateContent/children/child[@type='file']" />
          <xsl:apply-templates select="$derivateContent/children/child[@type='directory']" />
        </xsl:if>
      </xsl:for-each>
    </xsl:variable>

    <xsl:if test="count($files)&gt;0">
      <fn:array key="hasPart">
        <xsl:copy-of select="$files" />
      </fn:array>
    </xsl:if>
  </xsl:template>

  <xsl:template match="child[@type='directory']">
    <xsl:variable name="derivateID" select="/mcr_directory/ownerID/text()" />
    <xsl:variable name="path" select="/mcr_directory/path/text()" />
    <xsl:variable name="childContent"
                  select="document(concat('ifs:/',$derivateID,$path,name/text() ))" />
    <xsl:apply-templates select="$childContent/children/child" />
  </xsl:template>

  <xsl:template match="child[@type='file']">
    <fn:map>
      <xsl:choose>
        <xsl:when test="contains(contentType/text(), 'image/')">
          <xsl:call-template name="fileImageObject" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="fileMediaObject" />
        </xsl:otherwise>
      </xsl:choose>
    </fn:map>
  </xsl:template>

  <xsl:template name="fileImageObject">
    <xsl:param name="context" select="'http://schema.org/'" />
    <xsl:param name="type" select="'ImageObject'" />
    <xsl:call-template name="fileMediaObject">
      <xsl:with-param name="context" select="$context" />
      <xsl:with-param name="type" select="$type" />
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="fileMediaObject">
    <xsl:param name="context" select="'http://schema.org/'" />
    <xsl:param name="type" select="'MediaObject'" />

    <fn:string key="@context">
      <xsl:value-of select="$context" />
    </fn:string>
    <fn:string key="@type">
      <xsl:value-of select="$type" />
    </fn:string>
    <fn:string key="name">
      <xsl:value-of select="name/text()" />
    </fn:string>
    <fn:string key="contentSize">
      <xsl:value-of select="concat(size/text(), ' Byte')" />
    </fn:string>
    <fn:string key="dateModified">
      <xsl:value-of select="date[@type='lastModified']/text()" />
    </fn:string>
    <fn:string key="uploadDate">
      <xsl:value-of select="date[@type='created']/text()" />
    </fn:string>
    <fn:string key="encodingFormat">
      <xsl:value-of select="contentType/text()" />
    </fn:string>
    <fn:string key="contentUrl">
      <xsl:value-of
        select="concat($WebApplicationBaseURL,
          'servlets/MCRFileNodeServlet/',
           /mcr_directory/ownerID/text(),
           /mcr_directory/path/text(),
           fn:encode-for-uri(name/text()))" />
    </fn:string>
  </xsl:template>

  <xsl:template name="displayPreviewURL">
    <xsl:variable name="derivateID" select="/mycoreobject/structure/derobjects/derobject[1]/@xlink:href" />

    <xsl:message>
      <xsl:value-of select="concat('derivate ID is : ', $derivateID)" />
    </xsl:message>
    <xsl:if test="string-length($derivateID)&gt;0">
      <xsl:variable name="derivate" select="document(concat('mcrobject:',$derivateID))" />
      <xsl:variable name="maindoc"
                    select="$derivate/mycorederivate/derivate/internals[@class='MCRMetaIFS']/internal/@maindoc" />
      <xsl:message>
        <xsl:value-of select="concat('maindoc is : ', $maindoc)" />
      </xsl:message>
      <xsl:variable name="contentType"
                    select="document(concat('ifs:/',$derivateID))/mcr_directory/children/child[name=$maindoc]/contentType" />
      <xsl:variable name="fileEnding" select="lower-case(fn:tokenize($maindoc, '[.]')[position()=last()])" />

      <xsl:choose>
        <xsl:when test="$fileEnding='pdf'">
          <xsl:value-of
            select="concat($WebApplicationBaseURL, 'img/pdfthumb/', $derivateID, '/', fn:encode-for-uri($maindoc), '?centerThumb=no')" />
        </xsl:when>
        <xsl:when test="contains($MCR.Module-iview2.SupportedContentTypes, $contentType)">
          <xsl:value-of
            select="concat($WebApplicationBaseURL, 'servlets/MCRTileCombineServlet/MID/', $derivateID, '/', fn:encode-for-uri($maindoc))" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="''" />
        </xsl:otherwise>
      </xsl:choose>


    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
