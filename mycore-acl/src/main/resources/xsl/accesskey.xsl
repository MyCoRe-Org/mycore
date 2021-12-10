<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:param name="MCR.ACL.AccessKey.Strategy.AllowedObjectTypes" select="MCR.ACL.AccessKey.Strategy.AllowedObjectTypes" />
  <xsl:param name="allowedAccessKeyObjectTypes" select="$MCR.ACL.AccessKey.Strategy.AllowedObjectTypes" />
  <xsl:param name="MCR.ACL.AccessKey.Strategy.AllowedSessionPermissionTypes" />
  <xsl:param name="allowedSessionPermissionTypes" select="$MCR.ACL.AccessKey.Strategy.AllowedSessionPermissionTypes" />
  <xsl:param name="isAccessKeyForDerivateEnabled" select="contains($MCR.ACL.AccessKey.Strategy.AllowedObjectTypes, 'derivate')" />
  <xsl:param name="isReadAccessKeyForSessionEnabled" select="contains($MCR.ACL.AccessKey.Strategy.AllowedSessionPermissionTypes, 'read')" />
  <xsl:param name="isWriteAccessKeyForSessionEnabled" select="contains($MCR.ACL.AccessKey.Strategy.AllowedSessionPermissionTypes, 'writedb')" />
  <xsl:param name="isAccessKeyForSessionEnabled" select="$isReadAccessKeyForSessionEnabled or $isWriteAccessKeyForSessionEnabled" />

  <xsl:template name="isAccessKeyForObjectTypeIdEnabled">
    <xsl:param name="typeId" />
    <xsl:value-of select="contains($allowedAccessKeyObjectTypes, $typeId)" />
  </xsl:template>

  <xsl:template name="isAccessKeyWithPermissionForSessionEnabled">
    <xsl:param name="permission" />
    <xsl:value-of select="contains($allowedSessionPermissionTypes, $permission)" />
  </xsl:template>

  <xsl:template name="getRestPathForObjectTypeId">
    <xsl:param name="typeId" />
    <xsl:choose>
      <xsl:when test="$typeId='derivate'">
        <xsl:value-of select="'derivates/{derid}/accesskeys'" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="'accesskeys'" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="checkRestAccessKeyPathPermission">
    <xsl:param name="typeId" />
    <xsl:param name="restPath">
      <xsl:call-template name="getRestPathForObjectTypeId">
        <xsl:with-param name="typeId" select="$typeId" />
      </xsl:call-template>
    </xsl:param>
    <xsl:param name="permission" select="'writedb'" />
    <xsl:value-of select="(document(concat('checkrestapiaccess:', $restPath, ':', $permission))/boolean)" />
  </xsl:template>
</xsl:stylesheet>
