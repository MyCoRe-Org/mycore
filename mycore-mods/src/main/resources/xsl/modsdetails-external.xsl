<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" xmlns:mcrmods="xalan://org.mycore.mods.MCRMODSClassificationSupport"
  xmlns:acl="xalan://org.mycore.access.MCRAccessManager" xmlns:mcr="http://www.mycore.org/" xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:mcrxsl="xalan://org.mycore.common.xml.MCRXMLFunctions" xmlns:mods="http://www.loc.gov/mods/v3"
  exclude-result-prefixes="xlink mcr i18n acl mods mcrmods" version="1.0">


  <!-- copy this stylesheet to your application and overwrite template matches with highest priority=1  -->

  <!-- xsl:template priority="1" mode="present" match="/mycoreobject[contains(@ID,'_mods_')]">

  </xsl:template -->


  <xsl:template name="mods.editobject_without_table">
    <xsl:param name="accessedit" />
    <xsl:param name="accessdelete" />
    <xsl:param name="id" />
    <xsl:param name="hasURN" select="'false'" />
    <xsl:param name="displayAddDerivate" select="'true'" />
    <xsl:param name="layout" select="'$'" />
    <xsl:variable name="layoutparam">
      <xsl:if test="$layout != '$'">
        <xsl:value-of select="concat('&amp;layout=',$layout)" />
      </xsl:if>
    </xsl:variable>
    <xsl:variable name="editURL">
      <xsl:call-template name="mods.getObjectEditURL">
        <xsl:with-param name="id" select="$id" />
        <xsl:with-param name="layout" select="$layout" />
      </xsl:call-template>
    </xsl:variable>
    <xsl:if test="$objectHost = 'local'">
      <xsl:choose>
        <xsl:when test="acl:checkPermission($id,'writedb') or acl:checkPermission($id,'deletedb')">
          <xsl:variable name="type" select="substring-before(substring-after($id,'_'),'_')" />

                <xsl:if test="acl:checkPermission($id,'writedb')">
                  <xsl:choose>
                    <!-- ***************** -->
                    <!-- object has no urn -->
                    <!-- ***************** -->
                    <xsl:when test="not(mcrxsl:hasURNDefined($id))">
                      <a href="{$editURL}">
                        <img src="{$WebApplicationBaseURL}images/workflow_objedit.gif" title="{i18n:translate('object.editObject')}" />
                      </a>
                      <xsl:if test="$displayAddDerivate='true'">
                        <a href="{$ServletsBaseURL}derivate/create{$HttpSession}?id={$id}">
                        </a>
                      </xsl:if>
                      <!-- xsl:if test="mcrxsl:isAllowedObjectForURNAssignment($id)" -->
                      <a
                        href="{$ServletsBaseURL}MCRAddURNToObjectServlet{$HttpSession}?object={$id}&amp;xpath=.mycoreobject/metadata/def.modsContainer[@class='MCRMetaXML' and @heritable='false' and @notinherit='true']/modsContainer/mods:mods/mods:identifier[@type='urn']">
                        <img src="{$WebApplicationBaseURL}images/workflow_addnbn.gif" title="{i18n:translate('derivate.urn.addURN')}" />
                      </a>
                     <!-- /xsl:if -->
                    </xsl:when>
                    <!-- **************** -->
                    <!-- object has a urn -->
                    <!-- **************** -->
                    <xsl:otherwise>
                      <xsl:if test="$CurrentUser=$MCR.Users.Superuser.UserName">
                        <a href="{$editURL}">
                          <img src="{$WebApplicationBaseURL}images/workflow_objedit.gif" title="{i18n:translate('object.editObject')}" />
                        </a>
                      </xsl:if>
                      <xsl:if test="$displayAddDerivate=true()">
                        <a href="{$ServletsBaseURL}derivate/create{$HttpSession}?id={$id}">
                          <img src="{$WebApplicationBaseURL}images/workflow_deradd.gif" title="{i18n:translate('derivate.addDerivate')}" />
                        </a>
                      </xsl:if>
                    </xsl:otherwise>
                  </xsl:choose>
                </xsl:if>
                <xsl:if
                  test="acl:checkPermission($id,'deletedb') and (not(mcrxsl:hasURNDefined($id)) or (mcrxsl:hasURNDefined($id) and $CurrentUser=$MCR.Users.Superuser.UserName))">
                  <a href="{$ServletsBaseURL}object/delete{$HttpSession}?id={$id}" id="confirm_deletion">
                    <img src="{$WebApplicationBaseURL}images/workflow_objdelete.gif" title="{i18n:translate('object.delObject')}" />
                  </a>
                </xsl:if>
        </xsl:when>
      </xsl:choose>
    </xsl:if>
  </xsl:template>


</xsl:stylesheet>