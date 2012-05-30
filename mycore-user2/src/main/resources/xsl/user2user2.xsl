<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:date="http://exslt.org/dates-and-times"
  extension-element-prefixes="date">
  <xsl:template match="user">
    <user name="{@ID}" realm="local">
      <realName>
        <xsl:value-of select="concat(user.contact/contact.firstname,' ',user.contact/contact.lastname)" />
      </realName>
      <eMail>
        <xsl:value-of select="user.contact/contact.email" />
      </eMail>
      <xsl:if test="@id_enabled='false'">
        <validUntil>
          <xsl:value-of select="date:date-time()" />
        </validUntil>
      </xsl:if>
      <roles>
        <xsl:apply-templates select="user.groups/groups.groupID" />
      </roles>
      <attributes>
        <xsl:apply-templates mode="attribute"
          select="user.creator|
          user.creation_date|
          user.last_modified|
          user.description|
          user.primary_group|
          user.contact/contact.salutation|
          user.contact/contact.street|
          user.contact/contact.city|
          user.contact/contact.postalcode|
          user.contact/contact.country|
          user.contact/contact.state|
          user.contact/contact.institution|
          user.contact/contact.faculty|
          user.contact/contact.department|
          user.contact/contact.institute|
          user.contact/contact.telephone|
          user.contact/contact.fax|
          user.contact/contact.cellphone" />
      </attributes>
      <password hashType="crypt" hash="{user.password}" />
    </user>
  </xsl:template>
  <xsl:template match="groups.groupID">
    <role name="{.}" />
  </xsl:template>
  <xsl:template match="*" mode="attribute">
    <xsl:if test="string-length(.)&gt;0">
      <attribute name="{local-name()}" value="{.}" />
    </xsl:if>
  </xsl:template>
</xsl:stylesheet>