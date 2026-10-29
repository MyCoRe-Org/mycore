<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:mcrproperty="http://www.mycore.de/xslt/property"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="#all">

  <xsl:include href="xslInclude:functions" />

  <xsl:param name="WebApplicationBaseURL" />

  <xsl:param name="action" as="xs:string" select="''" />
  <xsl:param name="type" as="xs:string" select="''" />

  <xsl:variable name="sender" select="mcrproperty:one('MCR.Mail.Sender')" />
  <xsl:variable name="recipients" select="
    (tokenize(mcrproperty:one('MCR.Mail.Recipients'), ',') ! normalize-space())[.]
  " />
  <xsl:variable name="newline" select="'&#xA;'" />

  <xsl:template match="/">
    <xsl:message>
      <xsl:value-of select="$newline" />
      <xsl:value-of select="concat('type : ', $type, $newline)" />
      <xsl:value-of select="concat('action : ', $action, $newline)" />
    </xsl:message>
    <email>
      <xsl:choose>
        <xsl:when test="empty($recipients)">
          <xsl:message>
            <xsl:value-of select="'Can not send mail, recipients are missing.'" />
          </xsl:message>
        </xsl:when>
        <xsl:when test="$type = 'MCRObject' and $action = ('create', 'update', 'delete')">
          <xsl:apply-templates select="/*" mode="email_from" />
          <xsl:apply-templates select="/*" mode="email_to" />
          <xsl:apply-templates select="/*" mode="email_subject" />
          <xsl:apply-templates select="/*" mode="email_body" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:message>
            <xsl:value-of select="'Do not send mail for this action and type.'" />
          </xsl:message>
        </xsl:otherwise>
      </xsl:choose>
    </email>
  </xsl:template>

  <xsl:template match="mycoreobject" mode="email_from" priority="0">
    <from>
      <xsl:value-of select="$sender" />
    </from>
  </xsl:template>

  <xsl:template match="mycoreobject" mode="email_to" priority="0">
    <xsl:for-each select="$recipients">
      <to>
        <xsl:value-of select="." />
      </to>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="mycoreobject" mode="email_subject" priority="0">
    <subject>
      <xsl:choose>
        <xsl:when test="$action = 'create'">
          <xsl:value-of select="concat('New object created : ', @ID)" />
        </xsl:when>
        <xsl:when test="$action = 'update'">
          <xsl:value-of select="concat('Object updated : ', @ID)" />
        </xsl:when>
        <xsl:when test="$action = 'delete'">
          <xsl:value-of select="concat('Object deleted : ', @ID)" />
        </xsl:when>
      </xsl:choose>
    </subject>
  </xsl:template>

  <xsl:template match="mycoreobject" mode="email_body" priority="0">
    <body>
      <xsl:apply-templates select="document('user:current')/user" mode="info" />
      <xsl:value-of select="concat('MyCoReID : ', @ID, $newline)" />
      <xsl:if test="@label">
        <xsl:value-of select="concat('Label : ', @label, $newline)" />
      </xsl:if>
      <xsl:value-of select="concat('Link : &lt;', $WebApplicationBaseURL, 'receive/', @ID, '&gt;')" />
    </body>
  </xsl:template>

  <xsl:template match="user" mode="info">
    <xsl:value-of select="concat('User : ', @name, ' (', @realm, ')', $newline)" />
    <xsl:value-of select="concat('Name : ', realName[1], $newline)" />
  </xsl:template>

</xsl:stylesheet>
