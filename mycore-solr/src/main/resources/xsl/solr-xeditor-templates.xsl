<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xed="http://www.mycore.de/xeditor"
  xmlns:mcrsolr="http://www.mycore.de/components/solr" xmlns:mcrxml="xalan://org.mycore.common.xml.MCRXMLFunctions">
  <xsl:param name="MCR.Solr.ServerURL" />
  <xsl:param name="MCR.Solr.Core.main.Name" />
  <xsl:param name="MCR.Solr.Core.main.ServerURL" />
  <xsl:param name="MCR.Solr.Core.classification.Name" />
  <xsl:param name="MCR.Solr.Core.classification.ServerURL" />
  <xsl:variable name="mcrsolr:label-width" select="9" />
  <xsl:variable name="mcrsolr:input-width" select="9" />

  <xsl:template match="mcrsolr:fieldset">
    <xsl:variable name="paraname">
      <xsl:choose>
        <xsl:when test="@param">
          <xsl:value-of select="@param" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="@name" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="value">
      <xsl:choose>
        <xsl:when test="@param">
          <xsl:value-of select="@name" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="'true'" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <fieldset class="optional col-9">
      <xed:bind xpath="param[@name='{$paraname}']">
        <label class="checkbox-inline" for="{@name}">
          <input type="checkbox" value="{$value}" id="{@name}">
            <xsl:copy-of select="@title" />
          </input>
          <xsl:choose>
            <xsl:when test="mcrsolr:label">
              <xsl:copy-of select="mcrsolr:label/node()" />
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="@name" />
            </xsl:otherwise>
          </xsl:choose>
        </label>
      </xed:bind>
      <div class="fieldset">
        <xsl:apply-templates />
      </div>
    </fieldset>
  </xsl:template>

  <xsl:template match="mcrsolr:textfield">
    <xsl:variable name="id">
      <xsl:call-template name="mcrsolr:getId" />
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="@repeatable='true'">
        <div class="form-group">
          <label class="col-{$mcrsolr:label-width} control-label" for="{id}">
            <xsl:value-of select="@name" />
          </label>
          <div class="col-{$mcrsolr:input-width}">
            <div class="multiple">
              <div class="row clearfix">
                <xed:repeat xpath="param[@name='{@name}']">
                  <xsl:copy-of select="@initially" />
                  <xsl:copy-of select="@default" />
                  <div class="col-9">
                    <input class="form-control" type="text" id="{$id}">
                      <xsl:copy-of select="@title" />
                      <xsl:copy-of select="@placeholder" />
                    </input>
                  </div>
                  <div class="col-3">
                    <span class="float-end">
                      <div class="btn-group">
                        <xed:controls>insert remove</xed:controls>
                      </div>
                    </span>
                  </div>
                </xed:repeat>
              </div>
            </div>
          </div>
        </div>
      </xsl:when>
      <xsl:otherwise>
        <xed:bind xpath="param[@name='{@name}']">
          <xsl:copy-of select="@initially" />
          <xsl:copy-of select="@default" />
          <div class="form-group">
            <label class="col-{$mcrsolr:label-width} control-label" for="{$id}">
              <xsl:value-of select="@name" />
            </label>
            <div class="col-{$mcrsolr:input-width}">
              <input class="form-control" type="text" id="{$id}">
                <xsl:copy-of select="@title" />
                <xsl:copy-of select="@placeholder" />
              </input>
            </div>
          </div>
        </xed:bind>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="mcrsolr:checkbox">
    <xsl:variable name="id">
      <xsl:call-template name="mcrsolr:getId" />
    </xsl:variable>
    <xsl:variable name="paraname">
      <xsl:choose>
        <xsl:when test="@param">
          <xsl:value-of select="@param" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="@name" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="value">
      <xsl:choose>
        <xsl:when test="@param">
          <xsl:value-of select="@name" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="'true'" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="label">
      <xsl:choose>
        <xsl:when test="@label">
          <xsl:value-of select="@label" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="@name" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xed:bind xpath="param[@name='{$paraname}']">
      <xsl:copy-of select="@initially" />
      <xsl:copy-of select="@default" />
      <div class="form-group">
        <div class="col-{$mcrsolr:input-width}">
          <div class="checkbox">
            <label for="{$id}">
              <input type="checkbox" value="{$value}" id="{$id}">
                <xsl:copy-of select="@title" />
              </input>
              <xsl:value-of select="$label" />
            </label>
          </div>
        </div>
      </div>
    </xed:bind>
  </xsl:template>

  <xsl:template match="mcrsolr:textarea">
    <xsl:variable name="id">
      <xsl:call-template name="mcrsolr:getId" />
    </xsl:variable>
    <xed:bind xpath="param[@name='{@name}']">
      <xsl:copy-of select="@initially" />
      <xsl:copy-of select="@default" />
      <div class="form-group">
        <label class="col-{$mcrsolr:label-width} control-label" for="{$id}">
          <xsl:value-of select="@name" />
        </label>
        <div class="col-{$mcrsolr:input-width}">
          <textarea class="form-control" id="{$id}">
            <xsl:copy-of select="@title" />
            <xsl:copy-of select="@placeholder" />
          </textarea>
        </div>
      </div>
    </xed:bind>
  </xsl:template>

  <xsl:template name="mcrsolr:getId">
    <xsl:choose>
      <xsl:when test="@id">
        <xsl:value-of select="@id" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="mcrxml:regexp(@name,'\.','_')" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="mcrsolr:label">
  <!-- Labels are used in head of fieldset only -->
  </xsl:template>

  <xsl:template match="mcrsolr:fieldsHelp">
    <xsl:apply-templates select="." mode="with-core">
      <xsl:with-param name="core" select="'main'" />
    </xsl:apply-templates>
    <xsl:apply-templates select="." mode="with-core">
      <xsl:with-param name="core" select="'classification'" />
    </xsl:apply-templates>
  </xsl:template>

  <xsl:template match="mcrsolr:fieldsHelp" mode="with-core">
    <xsl:param name="core" />
    <xsl:if test="string-length($core) &gt; 0">
    <div class="table-responsive">
      <table class="table table-striped table-hover table-sm">
        <xsl:variable name="url" select="concat('solrfields:', $core)"/>
        <xsl:variable name="availableFields" select="document($url)" />
        <thead><tr><th colspan="3"><xsl:value-of select="concat('Fields of core ',$core)" /></th></tr></thead>
        <tr>
          <th>field</th>
          <th>static/dynamic</th>
          <th>type</th>
        </tr>
        <xsl:for-each select="$availableFields/solrFields/fields/field">
          <xsl:sort select="@name"/>
          <tr>
            <td>
              <xsl:value-of select="@name"/>
            </td>
            <td>
              <xsl:value-of select="'static'"/>
            </td>
            <td>
              <xsl:value-of select="@type"/>
            </td>
          </tr>

        </xsl:for-each>

        <xsl:for-each select="$availableFields/solrFields/dynamicFields/field">
          <xsl:sort select="@name"/>
          <tr>
            <td>
              <xsl:value-of select="@name"/>
            </td>
            <td>
              <xsl:value-of select="'dynamic'"/>
            </td>
            <td>
              <xsl:value-of select="@type"/>
            </td>
          </tr>
        </xsl:for-each>
      </table>
    </div>
    </xsl:if>
  </xsl:template>
</xsl:stylesheet>
