<?xml version="1.0" encoding="UTF-8"?>
  <xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:str="http://exslt.org/strings" exclude-result-prefixes="str">

    <xsl:param name="WebApplicationBaseURL" />
    <xsl:param name="ServletsBaseURL" />
    <xsl:param name="CurrentUser" />
    <xsl:param name="DefaultLang" />

    <xsl:param name="action" />
    <xsl:param name="type" />
    <xsl:param name="MCR.Mail.Sender" />
    <xsl:param name="MCR.Mail.Recipients" />
    <xsl:variable name="newline" select="'&#xA;'" />

    <xsl:template match="/">
      <xsl:message>
        <xsl:value-of select="$newline" />
        <xsl:value-of select="concat('type : ',$type,$newline)" />
        <xsl:value-of select="concat('action : ',$action,$newline)" />
      </xsl:message>
      <email>
        <xsl:choose>
          <xsl:when test="string-length($MCR.Mail.Recipients)&lt;1">
            <xsl:message>
              <xsl:value-of select="'Can not send mail, recipients are missing.'" />
            </xsl:message>
          </xsl:when>
          <xsl:when test="$type = 'MCRObject' and ($action = 'create' or $action = 'update' or $action = 'delete')">
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
        <xsl:value-of select="$MCR.Mail.Sender" />
      </from>
    </xsl:template>

    <xsl:template match="mycoreobject" mode="email_to" priority="0">
      <xsl:for-each select="str:tokenize($MCR.Mail.Recipients,',')">
        <to>
          <xsl:value-of select="." />
        </to>
      </xsl:for-each>
    </xsl:template>

    <xsl:template match="mycoreobject" mode="email_subject" priority="0">
      <subject>
        <xsl:choose>
          <xsl:when test="$action='create'">
            <xsl:value-of select="concat('New object created : ',@ID)" />
          </xsl:when>
          <xsl:when test="$action='update'">
            <xsl:value-of select="concat('Object updated : ',@ID)" />
          </xsl:when>
          <xsl:when test="$action='delete'">
            <xsl:value-of select="concat('Object deleted : ',@ID)" />
          </xsl:when>
        </xsl:choose>
      </subject>
    </xsl:template>

    <xsl:template match="mycoreobject" mode="email_body" priority="0">
      <body>
        <xsl:apply-templates select="document('user:current')/user" mode="info" />
        <xsl:value-of select="concat('MyCoReID : ',@ID,$newline)" />
        <xsl:if test="@label">
          <xsl:value-of select="concat('Label : ',@label,$newline)" />
        </xsl:if>
        <xsl:value-of select="concat('Link : &lt;',$WebApplicationBaseURL,'receive/',@ID, '&gt;')" />
      </body>
    </xsl:template>

    <xsl:template match="user" mode="info">
      <xsl:value-of select="concat('User : ',@name,' (',@realm,')',$newline)" />
      <xsl:value-of select="concat('Name : ',realName,$newline)" />
    </xsl:template>

  </xsl:stylesheet>
