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

XSL transformation from Kupu Library XML to HTML for the link library
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
   <form onsubmit="return false;">
     <table>
       <tr>
         <td>
           <strong>Title</strong><br />
           <xsl:value-of select="title" />
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
           <strong>Name</strong><br />
           <input type="text" id="link_name" size="10" />
         </td>
       </tr>
       <tr>
         <td>
           <strong>Target</strong><br />
           <input type="text" id="link_target" value="_self" size="10" />
         </td>
       </tr>
     </table>
    </form>
  </xsl:template>
</xsl:stylesheet>
