<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
    xmlns:xalan="http://xml.apache.org/xalan" exclude-result-prefixes="i18n">

    <xsl:include href="MyCoReLayout.xsl" />

    <xsl:param name="PageTitle" select="i18n:translate('sessionListing.pageTitle')" />

    <!-- URL's -->
    <xsl:variable name="sessionListing.baseURL">
        <xsl:value-of select="concat($ServletsBaseURL,'MCRSessionListingServlet')" />
    </xsl:variable>
    <xsl:variable name="sessionListing.listSessionsURL">
        <xsl:value-of select="concat($sessionListing.baseURL,'?sessionListing.mode=listSessions')" />
    </xsl:variable>

    <!-- default sort -->
    <xsl:param name="sessionListing.sort" select="'userRealName'" />

    <!-- handle tabs reqs -->
    <xsl:param name="sessionListing.toc.pageSize" select="20" />
    <xsl:param name="sessionListing.toc.pos" select="1" />
    <xsl:variable name="sessionListing.toc.pos.verif">
        <xsl:call-template name="get.sessionListing.toc.pos.verif" />
    </xsl:variable>

    <!-- ============================================================================================================= -->

    <xsl:template match="/sessionListing">
        <table width="100%">
            <xsl:call-template name="sessionListing.summary" />
        </table>
        <br />
        <xsl:call-template name="sessionListing.grouping" />
        <table width="100%" style="border:solid 2px;border-style:outset;">
            <xsl:call-template name="sessionListing.table.head" />
            <xsl:call-template name="sessionListing.table.body" />
        </table>
    </xsl:template>

    <!-- ============================================================================================================= -->

    <xsl:template name="sessionListing.summary">
        <tr>
            <td colspan="7">
                <b>
                    <xsl:value-of
                        select="concat(i18n:translate('sessionListing.userTotal'),' ',count(session),', ',i18n:translate('sessionListing.userLoggedIn'),' ',count(session/login[text()!='gast']))" />
                </b>
            </td>
        </tr>
    </xsl:template>

    <!-- ============================================================================================================= -->

    <xsl:template name="sessionListing.table.head">
        <xsl:variable name="sortURL">
            <xsl:value-of select="concat($sessionListing.listSessionsURL,'&amp;XSL.sessionListing.sort.SESSION')" />
        </xsl:variable>
        <tr>
            <th align="left">#</th>
            <th align="left">
                <xsl:value-of select="i18n:translate('sessionListing.name')" />
                <a href="{$sortURL}=userRealName" alt="sortieren" title="sortieren">
                    <xsl:value-of select="' ^'" />
                </a>
            </th>
            <th align="left">
                <xsl:value-of select="i18n:translate('sessionListing.login')" />
                <a href="{$sortURL}=login" alt="sortieren" title="sortieren">
                    <xsl:value-of select="' ^'" />
                </a>
            </th>
            <th align="left">
                <xsl:value-of select="i18n:translate('sessionListing.ip')" />
                <a href="{$sortURL}=ip" alt="sortieren" title="sortieren">
                    <xsl:value-of select="' ^'" />
                </a>
            </th>
            <th align="left">
                <xsl:value-of select="i18n:translate('sessionListing.firstAccess')" />
                <a href="{$sortURL}=createTime" alt="sortieren" title="sortieren">
                    <xsl:value-of select="' ^'" />
                </a>
            </th>
            <th align="left">
                <xsl:value-of select="i18n:translate('sessionListing.lastAccess')" />
                <a href="{$sortURL}=lastAccessTime" alt="sortieren" title="sortieren">
                    <xsl:value-of select="' ^'" />
                </a>
            </th>
            <th align="left">
                <xsl:value-of select="i18n:translate('sessionListing.loginSince')" />
                <a href="{$sortURL}=loginTime" alt="sortieren" title="sortieren">
                    <xsl:value-of select="' ^'" />
                </a>
            </th>
        </tr>
    </xsl:template>

    <!-- ============================================================================================================= -->

    <xsl:template name="sessionListing.table.body">
        <xsl:for-each select="session">
            <xsl:sort select="*[name()=$sessionListing.sort]" order="ascending" />
            <xsl:call-template name="sessionListing.printSession" />
        </xsl:for-each>
    </xsl:template>

    <!-- ============================================================================================================= -->

    <xsl:template name="sessionListing.printSession">
        <xsl:if test="(position()>=$sessionListing.toc.pos.verif) and ($sessionListing.toc.pos.verif+$sessionListing.toc.pageSize>position())">
            <tr>
                <td>
                    <xsl:value-of select="position()" />
                </td>
                <td>
                    <b>
                        <xsl:apply-templates select="userRealName" />
                    </b>
                </td>
                <td>
                    <xsl:apply-templates select="login" />
                </td>
                <td>
                    <xsl:apply-templates select="ip" />
                </td>
                <td>
                    <xsl:apply-templates select="createTime" />
                </td>
                <td>
                    <xsl:apply-templates select="lastAccessTime" />
                </td>
                <td>
                    <xsl:apply-templates select="loginTime" />
                </td>
            </tr>
        </xsl:if>
    </xsl:template>

    <!-- ============================================================================================================= -->

    <xsl:template match="login">
        <xsl:copy-of select="text()" />
    </xsl:template>
    <!-- ============================================================================================================= -->

    <xsl:template match="ip">
        <xsl:copy-of select="text()" />
    </xsl:template>
    <!-- ============================================================================================================= -->

    <xsl:template match="userRealName">
        <xsl:copy-of select="text()" />
    </xsl:template>
    <!-- ============================================================================================================= -->

    <xsl:template match="createTime">
        <xsl:call-template name="sessionListing.formatLongTime">
            <xsl:with-param name="time" select="text()" />
        </xsl:call-template>
    </xsl:template>
    <!-- ============================================================================================================= -->

    <xsl:template match="lastAccessTime">
        <xsl:call-template name="sessionListing.formatLongTime">
            <xsl:with-param name="time" select="text()" />
        </xsl:call-template>
    </xsl:template>

    <!-- ============================================================================================================= -->

    <xsl:template match="loginTime">
        <xsl:call-template name="sessionListing.formatLongTime">
            <xsl:with-param name="time" select="text()" />
        </xsl:call-template>
    </xsl:template>

    <!-- ============================================================================================================= -->

    <xsl:template name="sessionListing.formatLongTime">
        <xsl:param name="time" />
        <xsl:variable name="isoTime">
            <xsl:value-of xmlns:mcrxml="xalan://org.mycore.common.xml.MCRXMLFunctions" select="mcrxml:getISODate($time, 'long' )" />
        </xsl:variable>
        <xsl:variable name="format">
            <xsl:choose>
                <xsl:when test="string-length(normalize-space($isoTime))=4">
                    <xsl:value-of select="i18n:translate('metaData.dateYear')" />
                </xsl:when>
                <xsl:when test="string-length(normalize-space($isoTime))=7">
                    <xsl:value-of select="i18n:translate('metaData.dateYearMonth')" />
                </xsl:when>
                <xsl:when test="string-length(normalize-space($isoTime))=10">
                    <xsl:value-of select="i18n:translate('metaData.dateYearMonthDay')" />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="i18n:translate('metaData.dateTime')" />
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:call-template name="formatISODate">
            <xsl:with-param name="date" select="$isoTime" />
            <xsl:with-param name="format" select="$format" />
        </xsl:call-template>
    </xsl:template>

    <!-- ============================================================================================================= -->

    <xsl:template name="sessionListing.grouping">
        <form xmlns:encoder="xalan://java.net.URLEncoder" xmlns:xalan="http://xml.apache.org/xalan" action="{$sessionListing.baseURL}" accept-charset="UTF-8">
            <input type="hidden" name="sessionListing.mode" value="listSessions" />
            <table style="border-top:solid 1px;border-left:solid 1px;border-right:solid 1px;">
                <tr>
                    <td>
                        <b>
                            <xsl:value-of select="i18n:translate('sessionListing.sizeOfTable')" />
                        </b>
                        <input name="XSL.sessionListing.toc.pageSize.SESSION" value="{$sessionListing.toc.pageSize}" size="3" />
                        <xsl:value-of select="i18n:translate('sessionListing.rowsPerPage')" />
                        <xsl:call-template name="sessionListing.grouping.chooseHitPage">
                            <xsl:with-param name="children" select="." />
                        </xsl:call-template>
                    </td>
                </tr>
            </table>
        </form>
    </xsl:template>

    <!-- ============================================================================================================= -->

    <xsl:template name="sessionListing.grouping.chooseHitPage">
        <xsl:param name="children" />

        <xsl:variable name="numberOfChildren">
            <xsl:value-of select="count(xalan:nodeset($children)/session)" />
        </xsl:variable>
        <xsl:variable name="numberOfHitPages">
            <xsl:value-of select="ceiling(number($numberOfChildren) div number($sessionListing.toc.pageSize))" />
        </xsl:variable>
        <xsl:if test="number($numberOfChildren)>number($sessionListing.toc.pageSize)">
            <b>
                <xsl:value-of select="i18n:translate('sessionListing.choosePage')" />
            </b>
            <xsl:for-each select="xalan:nodeset($children)/session[number($numberOfHitPages)>=position()]">
                <xsl:variable name="jumpToPos">
                    <xsl:value-of select="(position()*number($sessionListing.toc.pageSize))-number($sessionListing.toc.pageSize)" />
                </xsl:variable>
                <xsl:choose>
                    <xsl:when test="number($jumpToPos)+1=number($sessionListing.toc.pos)">
                        <xsl:value-of select="concat(' [',position(),'] ')" />
                    </xsl:when>
                    <xsl:otherwise>
                        <a href="{concat($sessionListing.listSessionsURL,'&amp;XSL.sessionListing.toc.pos.SESSION=',$jumpToPos+1)}"
                            alt="{i18n:translate('sessionListing.goToPage')}{concat(' ',position())}"
                            title="{i18n:translate('sessionListing.goToPage')} {concat(' ',position())}">
                            <xsl:value-of select="concat(' ',position(),' ')" />
                        </a>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:for-each>
        </xsl:if>

    </xsl:template>

    <!-- ============================================================================================================= -->

    <xsl:template name="get.sessionListing.toc.pos.verif">
        <xsl:choose>
            <xsl:when test="$sessionListing.toc.pageSize>count(/sessionListing/session)">
                <xsl:value-of select="1" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$sessionListing.toc.pos" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- ============================================================================================================= -->

</xsl:stylesheet>