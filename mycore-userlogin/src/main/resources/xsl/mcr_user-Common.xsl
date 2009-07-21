<?xml version="1.0" encoding="UTF-8"?>

<!-- ============================================== -->
<!-- $Revision: 1.2 $ $Date: 2008/04/11 11:47:59 $ -->
<!-- ============================================== -->

<!-- +
| This stylesheet controls the Web-Layout of the Login Servlet. The Login Servlet
| gathers information about the session, user ID, password and calling URL and
| then tries to login the user by delegating the login request to the user manager.
| Depending on whether the login was successful or not, the Login Servlet generates
| the following XML output stream:
|
| <mcr_user unknown_user="true|false"
|           user_disabled="true|false"
|           invalid_password="true|false">
|   <guest_id>...</guest_id>
|   <guest_pwd>...</guest_pwd>
|   <backto_url>...<backto_url>
| </mcr_user>
|
| The XML stream is sent to the Layout Servlet and finally handled by this stylesheet.
|
| Authors: Detlev Degenhardt, Thomas Scheffler
+ -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:encoder="xalan://java.net.URLEncoder" exclude-result-prefixes="xlink encoder">
  
  <xsl:variable name="backto_url" select="/mcr_user/backto_url"/>
  <xsl:variable name="guest_id" select="/mcr_user/guest_id"/>
  <xsl:variable name="guest_pwd" select="/mcr_user/guest_pwd"/>
  <xsl:variable name="href-login"
    select="concat($ServletsBaseURL, 'MCRLoginServlet',$HttpSession,'?url=', encoder:encode(string($backto_url)))">
  </xsl:variable>
  <xsl:variable name="href-user"
    select="concat($ServletsBaseURL, 'MCRUserServlet',$HttpSession,'?url=', encoder:encode(string($backto_url)))">
  </xsl:variable>
  
  <!-- The main template -->
  <xsl:template match="/mcr_user">
    <div id="userlogin">
      <center>
        
        <!-- At first we display the current user in a head line. -->
        <p class="header">
          <xsl:value-of select="$heading"/>
        </p>
        
        <!-- +
        | There are three possible error-conditions: wrong password, unknown user and disabled
        | user. If one of these conditions occured, the corresponding information will be
        | presented at the top of the page.
        + -->
        <xsl:call-template name="userStatus"/>
        
        <xsl:call-template name="userAction"/>
        
      </center>
    </div>
  </xsl:template>
  
</xsl:stylesheet>