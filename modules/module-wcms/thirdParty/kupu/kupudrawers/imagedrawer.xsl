<?xml version="1.0" encoding="UTF-8" ?>
<!--
##############################################################################
#
# Copyright (c) 2003-2004 Kupu Contributors. All rights reserved.
#
# This software is distributed under the terms of the Kupu
# License. See LICENSE.txt for license text. For a list of Kupu
# Contributors see CREDITS.txt.
#
##############################################################################

XSL transformation from Kupu Library XML to HTML for the image library
drawer.

$Id$
-->
<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  version="1.0">

  <xsl:import
    href="librarydrawer.xsl"
    />

  <xsl:template match="resource|collection" mode="properties">
    <xsl:if test="preview">      
    <div><strong>Preview</strong></div>
    <div id="epd-imgpreview">
      <img src="{preview}" title="{title}" alt="{title}" />
    </div>
    </xsl:if>
    <table>
      <tr>
        <td>
          <strong>Title</strong><br />
          <xsl:value-of select="title" />
        </td>
      </tr>
      <tr>
        <td>
          <strong>Size</strong><br />
          <xsl:value-of select="size" />
        </td>
      </tr>
      <tr>
        <td>
          <strong>Description</strong><br />
          <xsl:value-of select="description" />
        </td>
      </tr>
      <tr>
        <td>
          <strong>ALT-text</strong><br />
          <form onsubmit="return false;">
            <input type="text" id="image_alt" size="10" />
          </form>
        </td>
      </tr>
    </table>
  </xsl:template>
</xsl:stylesheet>
