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
                xmlns:cr="http://www.crossref.org/schema/4.4.1"
                xmlns:xlink="http://www.w3.org/1999/xlink"
                version="3.0"
>

  <!--
  Book
    Required Elements
    Series: titles, ISSN, volume, ISBN, publication_date (year), publisher (publisher_name)
    Set: titles, ISBN, volume
    Book: titles, publication_date (year), ISBN‡if no ISBN is available, use <noisbn> element, publisher
    Chapter:doi_data

  Recommended Elements
    Series: doi_data,edition_number,contributors, coden, series_number, citation_list
    Set: contributors, doi_data, edition_number, contributors, citation_list, doi_data
    Book: contributors, ORCIDs, edition_number, doi_data, citation_list, metadata for funding, license, and CrossMark
    Chapter: contributors, titles, pages, publication_date, citation_list
    also recommended:  funding, license, and CrossMark metadata

  Optional Elements
    publisher_item
    part_number
    component_number
    component_list
  -->
<!--
  <xsl:template
      match="mods:mods[contains(mods:classification/@valueURI, '#monograph') or contains(mods:classification/@valueURI, '#edited_book') or contains(mods:classification/@valueURI, '#reference') or contains(mods:classification/@valueURI, '#other')]">
    <xsl:call-template name="crossrefContainer">
      <xsl:with-param name="content">

        <xsl:variable name="bookType">
           edited_book|monograph|other|reference

        </xsl:variable>
        <cr:book book_type="{$bookType}">
          <cr:book_metadata>
            <xsl:call-template name="bookMetadata">
              <xsl:with-param name="modsNode" select="."/>
            </xsl:call-template>
          </cr:book_metadata>
          <xsl:variable name="seriesNode" select="mods:relatedItem[@type='series']"/>
          <xsl:choose>
            <xsl:when test="count($seriesNode)&gt;0">
              <cr:book_series_metadata>
                <xsl:call-template name="seriesMetadata">
                  <xsl:with-param name="parentModsNode" select="$seriesNode"/>
                </xsl:call-template>
              </cr:book_series_metadata>
            </xsl:when>
          </xsl:choose>
        </cr:book>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:template> -->

  <xsl:template name="bookMetadata">
    <xsl:param name="modsNode"/>
    <cr:titles>
      <xsl:for-each select="mods:titleInfo">
        <cr:title>
          <xsl:if test="mods:nonSort">
            <xsl:value-of select="concat(mods:nonSort/text(), ' ')"/>
          </xsl:if>
          <xsl:value-of select="mods:title/text()"/>
        </cr:title>
        <xsl:if test="mods:subTitle">
          <cr:subtitle>
            <xsl:value-of select="mods:subTitle"/>
          </cr:subtitle>
        </xsl:if>
      </xsl:for-each>
    </cr:titles>

    <xsl:variable name="publicationNode" select="mods:originInfo[@eventType='publication']"/>
    <xsl:if test="count($publicationNode) &gt; 0">
      <xsl:call-template name="publicationYear">
        <xsl:with-param name="modsNode" select="." />
      </xsl:call-template>
      <cr:publisher>
        <xsl:if test="$publicationNode/mods:publisher">
          <cr:publisher_name>
            <xsl:value-of select="$publicationNode/mods:publisher/text()"/>
          </cr:publisher_name>
        </xsl:if>
      </cr:publisher>
    </xsl:if>
  </xsl:template>

  <xsl:template name="seriesMetadata">
    <xsl:param name="parentModsNode"/>

  </xsl:template>

</xsl:stylesheet>