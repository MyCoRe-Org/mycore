<?xml version="1.0" encoding="ISO-8859-1"?>

<!--
     This stylesheet controls the Web-Layout of the "List All User "-mode of the UserAdminServlet.
     The UserAdminServlet provides data as an XML stream with the following syntax (an
     example is provided).

	<mycoreuser xsi:noNamespaceSchemaLocation="MCRUser.xsd">
	 <user>
		<user numID="30" ID="00280603320" id_enabled="true" update_allowed="true">
		<user.creator>root</user.creator>
		<user.creation_date>2005-02-24 16:11:02.0</user.creation_date>
		<user.last_modified>2005-02-24 16:11:02.0</user.last_modified>
		<user.password/>
		<user.description>Autor</user.description>
		<user.primary_group>authorgroup1</user.primary_group>
		<user.contact>
			<contact.salutation>Frau</contact.salutation>
			<contact.firstname>Anja</contact.firstname>
			<contact.lastname>Schaar</contact.lastname>
			<contact.street/>
			<contact.city>Rostock</contact.city>
			<contact.postalcode>18057</contact.postalcode>
			<contact.country/>
			<contact.state/>
			<contact.institution/>
			<contact.faculty/>
			<contact.department/>
			<contact.institute>Universitätsbibliothek</contact.institute>
			<contact.telephone/>
			<contact.fax/>
			<contact.email/>
			<contact.cellphone/>
		</user.contact>
		<user.groups/>
	  </user>
	  <user>
		<user numID="3" ID="administrator" id_enabled="true" update_allowed="true">
		<user.creator>root</user.creator>
		<user.creation_date>2004-11-11 17:14:06.0</user.creation_date>
		<user.last_modified>2004-11-11 17:14:06.0</user.last_modified>
		<user.password/>
		<user.description>Project Adminitrator</user.description>
		<user.primary_group>admingroup</user.primary_group>
		<user.contact/>
		<user.groups>
			<groups.groupID>admingroup</groups.groupID>
		</user.groups>
	  </user>
	</mycoreuser>     

    Author: Anja Schaar

    -->

<xsl:stylesheet  version="1.0"  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:xlink="http://www.w3.org/1999/xlink"  exclude-result-prefixes="xlink">

    <xsl:variable
      xmlns:encoder="xalan://java.net.URLEncoder"
      name="href-user"
      select="concat($ServletsBaseURL, 'MCRUserServlet',$HttpSession,'?mode=ShowUser')">
    </xsl:variable>

    <xsl:variable
      xmlns:encoder="xalan://java.net.URLEncoder"
      name="href-modify"
      select="concat($ServletsBaseURL, 'MCRUserAdminServlet',$HttpSession,'?mode=modifyuser')">
    </xsl:variable>

<xsl:template match="/mycoreuser">
 <table class="resultlist">
   <tr>
     <td class="metaname"><xsl:value-of select="$Userdata.ID"/></td>
     <td class="metaname"><xsl:value-of select="$Userdata.firstname"/></td>
     <td class="metaname"><xsl:value-of select="$Userdata.lastname"/></td>
     <td class="metaname"><xsl:value-of select="$Userdata.institution"/></td>
     <td class="metaname"><xsl:value-of select="$Userdata.faculty"/></td>
     <td class="metaname"><xsl:value-of select="$Userdata.primary_group"/></td>
	 <td class="metaname"><xsl:value-of select="$Userdata.enabled"/></td>
     <td class="metaname"><xsl:value-of select="$Userdata.numID"/></td>
   </tr>
   <xsl:for-each select="user">
		<xsl:call-template name="actual_user" />
	</xsl:for-each>
 </table>
</xsl:template>



<xsl:template name="actual_user">
    <!-- Now we present the user data in a table. First the account data ... -->
	  <tr>
		<xsl:variable name="ID" select="./@ID" ></xsl:variable>
	    <xsl:variable
		  xmlns:encoder="xalan://java.net.URLEncoder"
	      name="this-user"
		  select="concat($href-user,'&amp;uid=', encoder:encode(string($ID)))">
        </xsl:variable>
	    <xsl:variable
		  xmlns:encoder="xalan://java.net.URLEncoder"
	      name="this-user-modify"
		  select="concat($href-modify,'&amp;uid=', encoder:encode(string($ID)))">
        </xsl:variable>

        <td class="resultTitle"><a href="{$this-user}" ><xsl:value-of select="$ID"/></a></td>
        <td class="description"><xsl:value-of select="./user.contact/contact.firstname"/></td>
        <td class="description"><xsl:value-of select="./user.contact/contact.lastname"/></td>
        <td class="description"><xsl:value-of select="./user.contact/contact.institution"/></td>
        <td class="description"><xsl:value-of select="./user.contact/contact.faculty"/></td>
        <td class="description"><xsl:value-of select="./user.primary_group"/></td>
        <xsl:choose>
          <xsl:when test="./@id_enabled='true'">
            <td class="description"><xsl:value-of select="$Userdata.state_enabled"/></td>
          </xsl:when>
          <xsl:otherwise>
            <td class="description"><xsl:value-of select="$Userdata.state_disabled"/></td>
          </xsl:otherwise>
        </xsl:choose>
        <td class="description"><xsl:value-of select="./@numID"/></td>
	 <td class="resultTitle"><a href="{$this-user-modify}" >
		<xsl:value-of select="$Userdata.edit" /></a>
	 </td>
	</tr>
</xsl:template>
</xsl:stylesheet>
