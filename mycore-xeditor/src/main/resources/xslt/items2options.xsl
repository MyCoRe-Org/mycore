<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <!-- Transforms output of "classification:editorComplete:*" URIs to xeditor compatible format -->
  <xsl:param name="CurrentLang" />
  <xsl:param name="DefaultLang" />
  <xsl:param name="MaxLengthVisible" />
  <xsl:param name="Mode" />
  
  <xsl:param name="MCR.XEditor.Items2Options.DisabledSelectable" select="'false'" />
  <xsl:param name="MCR.XEditor.Items2Options.GroupsSelectable" select="'false'" />
  <xsl:param name="MCR.XEditor.Items2Options.GroupsAsOptions" select="'false'" />
  <xsl:param name="MCR.XEditor.Items2Options.HideDisabled" select="'false'" />
  <xsl:param name="MCR.XEditor.Items2Options.InheritDisabled" select="'false'" />
  <xsl:param name="MCR.XEditor.Items2Options.Indent" select="'&#160;&#160;&#160;'" />
  
  <xsl:param name="allSelectable" select="''" />

  <!--
    allSelectable, if present, overrides disabledSelectable and groupsSelectable
    otherwise use respective value or default value from config
  -->
  <xsl:variable name="disabledSelectableDefault" >
    <xsl:choose>
      <xsl:when test="$allSelectable!=''">
        <xsl:value-of select="$allSelectable"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$MCR.XEditor.Items2Options.DisabledSelectable"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:variable name="groupsSelectableDefault" >
    <xsl:choose>
      <xsl:when test="$allSelectable!=''">
        <xsl:value-of select="$allSelectable"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$MCR.XEditor.Items2Options.GroupsSelectable"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  
  <xsl:param name="disabledSelectable" select="$disabledSelectableDefault" />
  <xsl:param name="groupsSelectable" select="$groupsSelectableDefault" />
  <xsl:param name="groupsAsOptions" select="$MCR.XEditor.Items2Options.GroupsAsOptions" />
  <xsl:param name="hideDisabled" select="$MCR.XEditor.Items2Options.HideDisabled" />
  <xsl:param name="inheritDisabled" select="$MCR.XEditor.Items2Options.InheritDisabled" />

  <xsl:template match="items">
    <xsl:choose>
      <xsl:when test="$Mode='editor'">
        <xsl:apply-templates select="." mode="editor" />
      </xsl:when>
      <xsl:otherwise>
        <select>
          <xsl:copy-of select="@*" />
          <xsl:apply-templates />
        </select>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="item">
    <xsl:param name="indent" select="''" />
    <xsl:param name="disabled" select="false()" />

    <xsl:variable name="toolTip">
      <xsl:apply-templates select="." mode="toolTip" />
    </xsl:variable>
    
    <xsl:variable name="isGroup" select="label[lang('x-group')]='true'"/>
    <xsl:variable name="isGroupsAsOptions" select="$groupsAsOptions='true'"/>
    <xsl:variable name="isGroupsSelectable" select="$groupsSelectable='true' or $allSelectable='true'"/>
    <xsl:variable name="isDisabled" select="label[lang('x-disable')]='true' or $disabled"/>
    <xsl:variable name="isHideDisabled" select="$hideDisabled='true'"/>
    <xsl:variable name="isDisabledSelectable" select="$disabledSelectable='true' or $allSelectable='true'"/>
    
    <!--
      ┌─ A isGroup
      │  ┌─ B isGroupsSelectable
      │  │  ┌─ C isGroupsAsOptions
      │  │  │  ┌─ D isDisabled
      │  │  │  │  ┌─ E isDisabledSelectable
      │  │  │  │  │  ┌─ F isHideDisabled
      │  │  │  │  │  │
      │  │  │  │  │  │      ┌─ G doRenderAtAll
      │  │  │  │  │  │      │  ┌─ H doRenderAsDisabled
      │  │  │  │  │  │      │  │  ┌─ I doRenderAsOptionGroup
      │  │  │  │  │  │      │  │  │
      0  X  X  0  0  0      1  0  0      normal entry
      0  X  X  0  0  1      1  0  0      normal entry
      0  X  X  0  1  0      1  0  0      normal entry
      0  X  X  0  1  1      1  0  0      normal entry
      0  X  X  1  0  0      1  1  0      disabled entry
      0  X  X  1  0  1      0  X  X      hidden disabled entry
      0  X  X  1  1  0      1  0  0      selectable disabled entry
      0  X  X  1  1  1      1  0  0      selectable disabled entry
      1  0  0  0  0  0      1  X  1      normal group
      1  0  0  0  0  1      1  X  1      normal group
      1  0  0  0  1  0      1  X  1      normal group
      1  0  0  0  1  1      1  X  1      normal group    
      1  0  0  1  0  0      1  1  0      disabled (therefore option group not possible) group
      1  0  0  1  0  1      0  X  X      hidden disabled group
      1  0  0  1  1  0      1  1  0      disabled (therefore option group not possible) group
      1  0  0  1  1  1      1  1  0      hidden disabled group [rendered because disabled selectable but still disabled because groups are not selectable]
      1  0  1  0  0  0      1  1  0      normal group (not as option group)
      1  0  1  0  0  1      1  1  0      normal group (not as option group)
      1  0  1  0  1  0      1  1  0      normal group (not as option group)
      1  0  1  0  1  1      1  1  0      normal group (not as option group)
      1  0  1  1  0  0      1  1  0      disabled group (not as option group)
      1  0  1  1  0  1      0  X  X      hidden disabled group (not as option group)
      1  0  1  1  1  0      1  1  0      disabled group (not as option group)
      1  0  1  1  1  1      1  1  0      hidden disabled group [rendered because disabled selectable but still disabled because groups are not selectable]
      1  1  0  0  0  0      1  0  0      selectable normal group
      1  1  0  0  0  1      1  0  0      selectable normal group
      1  1  0  0  1  0      1  0  0      selectable normal group
      1  1  0  0  1  1      1  0  0      selectable normal group
      1  1  0  1  0  0      1  1  0      disabled (therefore not selectable) selectable (therefore option group not possible) group 
      1  1  0  1  0  1      0  X  X      hidden disabled selectable group
      1  1  0  1  1  0      1  0  0      disabled selectable (therefore option group not possible) group
      1  1  0  1  1  1      1  0  0      disabled selectable (therefore option group not possible) group
      1  1  1  0  0  0      1  0  0      selectable normal group (not as option group)
      1  1  1  0  0  1      1  0  0      selectable normal group (not as option group)
      1  1  1  0  1  0      1  0  0      selectable normal group (not as option group)
      1  1  1  0  1  1      1  0  0      selectable normal group (not as option group)
      1  1  1  1  0  0      1  1  0      disabled (therefore not selectable) selectable (therefore option group not possible) group
      1  1  1  1  0  1      0  X  X      hidden disabled selectable group (not as option group)
      1  1  1  1  1  0      1  0  0      disabled selectable (therefore option group not possible) group (not as option group)
      1  1  1  1  1  1      1  0  0      disabled selectable (therefore option group not possible) group (not as option group)
      
      G = ¬(D¬EF)
      H = A¬B + D¬E
      I = A¬B¬C¬D
    -->
        
    <xsl:variable name="doRenderAtAll" select="not($isDisabled and not($isDisabledSelectable) and $isHideDisabled)"/>
    <xsl:variable name="doRenderAsDisabled" select="($isGroup and not($isGroupsSelectable)) or ($isDisabled and not($isDisabledSelectable))"/>
    <xsl:variable name="doRenderAsOptionGroup" select="$isGroup and not($isGroupsSelectable) and not($isGroupsAsOptions) and not($isDisabled)"/>

    <xsl:variable name="nextIndent" select="concat($MCR.XEditor.Items2Options.Indent,$indent)"/>
    <xsl:variable name="nextDisabled" select="$inheritDisabled='true' and $isDisabled"/>

    <xsl:choose>
      <xsl:when test="$doRenderAtAll">
        <xsl:choose>
          <xsl:when test="$doRenderAsOptionGroup">
            <optgroup title="{$toolTip}">
              <xsl:attribute name="label">
                <xsl:value-of select="$indent" disable-output-escaping="yes" />
                <xsl:apply-templates select="." mode="label" />
              </xsl:attribute>
              <xsl:copy-of select="@*" />
              <xsl:apply-templates select="item">
                <xsl:with-param name="indent" select="$nextIndent" />
                <xsl:with-param name="disabled" select="$nextDisabled" />
              </xsl:apply-templates>
            </optgroup>
          </xsl:when>
          <xsl:otherwise>
            <option title="{$toolTip}">
              <xsl:if test="$doRenderAsDisabled">
                <xsl:attribute name="disabled"/>
              </xsl:if>
              <xsl:copy-of select="@*" />
              <xsl:value-of select="$indent" disable-output-escaping="yes" />
              <xsl:apply-templates select="." mode="label" />
            </option>
            <xsl:apply-templates select="item">
              <xsl:with-param name="indent" select="$nextIndent" />
              <xsl:with-param name="disabled" select="$nextDisabled" />
            </xsl:apply-templates>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates select="item">
          <xsl:with-param name="indent" select="$indent" />
          <xsl:with-param name="disabled" select="$nextDisabled" />
        </xsl:apply-templates>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="item" mode="label">
    <xsl:variable name="onDisplay">
      <xsl:choose>
        <xsl:when test="label[lang($CurrentLang)]">
          <xsl:value-of select="label[lang($CurrentLang)]" />
        </xsl:when>
        <xsl:when test="label[lang($DefaultLang)]">
          <xsl:value-of select="label[lang($DefaultLang)]" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="label[1]" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:call-template name="shorten">
      <xsl:with-param name="onDisplay" select="$onDisplay" />
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="item" mode="toolTip">
    <xsl:choose>
      <xsl:when test="label[lang($CurrentLang)]">
        <xsl:value-of select="label[lang($CurrentLang)]" />
      </xsl:when>
      <xsl:when test="label[lang($DefaultLang)]">
        <xsl:value-of select="label[lang($DefaultLang)]" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="." />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="items" mode="editor">
    <items>
      <xsl:apply-templates mode="editor" />
    </items>
  </xsl:template>

  <xsl:template match="item" mode="editor">
    <item value="{@value}">
      <xsl:for-each select="label">
        <xsl:apply-templates select="." mode="editor" />
      </xsl:for-each>
      <!-- ==== handle children ==== -->
      <xsl:apply-templates select="item" mode="editor" />
    </item>
  </xsl:template>

  <xsl:template match="label" mode="editor">
    <label xml:lang="{@xml:lang}">
      <xsl:call-template name="shorten">
        <xsl:with-param name="onDisplay" select="." />
      </xsl:call-template>
    </label>
  </xsl:template>

  <xsl:template name="shorten">
    <xsl:param name="onDisplay" />
    <xsl:choose>
      <xsl:when test="$MaxLengthVisible and (string-length($onDisplay) &gt; $MaxLengthVisible) ">
        <xsl:value-of select="concat(substring($onDisplay, 0, $MaxLengthVisible), ' [...]')" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$onDisplay" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>
