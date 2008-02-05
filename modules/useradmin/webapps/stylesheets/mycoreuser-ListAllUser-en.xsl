<?xml version="1.0" encoding="ISO-8859-1"?>

<!-- ============================================== -->
<!-- $Revision: 1.1 $ $Date: 2005-06-27 13:18:53 $ -->
<!-- ============================================== -->

<!-- +
     | This stylesheet provides the language dependent values (german language) for
     | the presentation of the data of the list off all mycore users. It is called by the
     | MCRUserAdminServlet in the "ListAllUser"-mode by forwarding an XML representation of
     | a user object to the LayoutServlet. After defining the language dependent
     | values this stylesheet finally includes the stylesheet mycoreuser-ListAlluser.xsl.
     |
     | Author: Anja Schaar
     | Last changes: 2005-20-06
     + -->

<xsl:stylesheet
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xlink="http://www.w3.org/1999/xlink"
  exclude-result-prefixes="xlink"
>

<xsl:variable name="Userdata.title"              select="'List of all users'" />
<xsl:variable name="Userdata.numID"              select="'numerical ID'" />
<xsl:variable name="Userdata.ID"                 select="'account ID'" />
<xsl:variable name="Userdata.enabled"            select="'status'" />
<xsl:variable name="Userdata.state_enabled"      select="'active'" />
<xsl:variable name="Userdata.state_disabled"     select="'not active'" />
<xsl:variable name="Userdata.primary_group"      select="'primary group'" />
<xsl:variable name="Userdata.firstname"          select="'first name'" />
<xsl:variable name="Userdata.lastname"           select="'last name'" />
<xsl:variable name="Userdata.institution"        select="'institution'" />
<xsl:variable name="Userdata.faculty"            select="'faculty'" />
<xsl:variable name="Userdata.edit"               select="'edit this user'" />

<xsl:variable name="MainTitle" select="'@libri'"/>
<xsl:variable name="PageTitle" select="$Userdata.title"/>

<xsl:variable name="Servlet" select="'UserAdminServlet'"/>

<xsl:include href="MyCoReLayout-de.xsl" />
<xsl:include href="mycoreuser-ListAllUser.xsl" />

</xsl:stylesheet>
