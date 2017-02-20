<!--
  ~  This file is part of ***  M y C o R e  ***
  ~  See http://www.mycore.de/ for details.
  ~
  ~  This program is free software; you can use it, redistribute it
  ~  and / or modify it under the terms of the GNU General Public License
  ~  (GPL) as published by the Free Software Foundation; either version 2
  ~  of the License or (at your option) any later version.
  ~
  ~  This program is distributed in the hope that it will be useful, but
  ~  WITHOUT ANY WARRANTY; without even the implied warranty of
  ~  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  ~  GNU General Public License for more details.
  ~
  ~  You should have received a copy of the GNU General Public License
  ~  along with this program, in a file called gpl.txt or license.txt.
  ~  If not, write to the Free Software Foundation Inc.,
  ~  59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
  ~
  -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:mods="http://www.loc.gov/mods/v3"
                xmlns:xlink="http://www.w3.org/1999/xlink">

  <xsl:include href="copynodes.xsl" />

  <xsl:variable name="sortTemp">
    <element name="typeOfResource"/>
    <element name="titleInfo"/>
    <element name="name"/>
    <element name="genre"/>
    <element name="originInfo"/>
    <element name="language"/>
    <element name="abstract"/>
    <element name="note"/>
    <element name="subject"/>
    <element name="classification"/>
    <element name="relatedItem"/>
    <element name="identifier"/>
    <element name="location"/>
    <element name="accessCondition"/>
  </xsl:variable>

  <xsl:variable name="sort" select="xalan:nodeset($sortTemp)" xmlns:xalan="http://xml.apache.org/xalan" />

  <xsl:key name="sortOrder" match="element" use="@name" />

  <xsl:template
    match="mods:mods[/mycoreobject/structure/parents/parent and not(./mods:relatedItem/@xlink:href=/mycoreobject/structure/parents/parent/@xlink:href)]">
    <xsl:copy>
      <xsl:variable name="mods" select="." />
      <xsl:variable name="hostID" select="/mycoreobject/structure/parents/parent/@xlink:href" />
      <xsl:comment>first elements</xsl:comment>
      <xsl:for-each select="$sort/element[not(preceding-sibling::element[@name='relatedItem'])]">
        <xsl:variable name="localName" select="@name" />
        <xsl:apply-templates select="$mods/mods:*[local-name()=$localName]" />
      </xsl:for-each>
      <xsl:comment>related items</xsl:comment>
      <mods:relatedItem type="host" xlink:href="{$hostID}">
        <!-- MCRMODSMetadataShareAgent does the rest -->
      </mods:relatedItem>
      <xsl:apply-templates select="mods:relatedItem" />
      <xsl:comment>other important stuff</xsl:comment>
      <xsl:for-each select="$sort/element[not(following-sibling::element[@name='relatedItem'])]">
        <xsl:variable name="localName" select="@name" />
        <xsl:apply-templates select="$mods/mods:*[local-name()=$localName]" />
      </xsl:for-each>
      <xsl:comment>the rest goes here</xsl:comment>
      <xsl:apply-templates select="mods:*[not(local-name() = $sort/element/@name)]" />
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
