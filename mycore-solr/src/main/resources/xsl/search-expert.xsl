<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE xsl:stylesheet [
  <!ENTITY html-output SYSTEM "xsl/xsl-output-html.fragment">
]>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0" xmlns:encoder="xalan://java.net.URLEncoder"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation">
  &html-output;
  <xsl:include href="MyCoReLayout.xsl" />
  <xsl:param name="WebApplicationBaseURL" />
  <xsl:param name="MCR.Module-solr.ServerURL" />
  <xsl:variable name="PageTitle" select="i18n:translate('component.solr.expertsearch.headline')" />

  <xsl:template match="search-expert">
    <p id="expertSearchFormContainer">
      <form id="expertSearchForm" action="{concat($WebApplicationBaseURL, 'servlets/SolrSelectProxy')}" method="get" accept-charset="UTF-8">
        <input type="hidden" name="start" value="0" />

        <table class="editor">

          <tr>
            <td>
              <label for="q" class="editorText">Anfrage: </label>
            </td>
            <td align="right" colspan="3">
              <input type="text" size="70" id="q" name="q" />
            </td>
          </tr>

          <tr id="rows">
            <td colspan="2">
              <label for="rows " class="editorText" align="right">Treffer pro Seite</label>
            </td>
            <td align="right" colspan="2">
              <select name="rows" tabindex="1" class="editorList">
                <option value="10">10</option>
                <option value="20">20</option>
                <option value="30">30</option>
                <option value="40">40</option>
                <option value="50">50</option>
                <option value="100">100</option>
              </select>
            </td>
          </tr>
          <tr>
            <td />
            <td style="text-align:right" colspan="3">
              <input type="submit" class="button" id="submitSearchButton" value="Suchen" />
            </td>
          </tr>

          <tr>
            <td colspan="4" style="border-top-width: 2px; border-top-style: dotted; border-top-color:grey" />
          </tr>

          <tr>
            <td>
              <label class="editorText">
                <b>
                  <xsl:value-of select="i18n:translate('component.solr.expertsearch.field')" />

                </b>
              </label>
            </td>
            <td />
            <td>
              <label class="editorText">
                <b>
                  <xsl:value-of select="i18n:translate('component.solr.expertsearch.type')" />
                </b>
              </label>
            </td>
          </tr>

          <xsl:variable name="availableFields" select="document(concat($MCR.Module-solr.ServerURL, 'admin/luke'))" />
          <xsl:for-each select="$availableFields/response/lst[@name='fields']/lst">
            <xsl:sort select="@name" />
            <tr>
              <td>
                <label class="editorText">
                  <xsl:value-of select="@name" />
                </label>
              </td>
              <td />
              <td>
                <label class="editorText">
                  <xsl:value-of select="str[@name='type']" />
                </label>
              </td>
            </tr>
          </xsl:for-each>
        </table>
      </form>
    </p>
  </xsl:template>
</xsl:stylesheet>