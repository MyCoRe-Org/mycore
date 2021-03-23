<?xml version="1.0"?>
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
  ~
  ~
  -->
<xsl:stylesheet version="3.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">


    <xsl:mode on-no-match="shallow-copy" />

    <!--
     Retrives a basket and turns it into a <list><mycoreobject>...</mycoreobject>...</list>
    -->
    <xsl:template match="/basket">
        <list>
            <xsl:apply-templates select="entry" />
            <xsl:copy-of select="entry/mycoreobject"  />
        </list>
    </xsl:template>

    <xsl:template match="entry">
        <xsl:choose>
            <xsl:when test="count(mycoreobject)&gt;0">
                <xsl:copy-of select="mycoreobject" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy-of select="document(concat('mcrobject:', @id))" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

</xsl:stylesheet>