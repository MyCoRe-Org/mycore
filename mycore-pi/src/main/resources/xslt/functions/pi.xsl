<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:mcrpi="http://www.mycore.de/xslt/pi"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="#all">


  <xsl:variable name="piServiceURIPrefix" select="'pi:'" />

  <xsl:function name="mcrpi:has-identifier-created" as="xs:boolean">
    <xsl:param name="service" as="xs:string" />
    <xsl:param name="id" as="xs:string" />
    <xsl:param name="additional" as="xs:string" />

    <xsl:sequence select="xs:boolean(
      document(
        concat($piServiceURIPrefix,
          'hasIdentifierCreated/?service=',
          encode-for-uri($service),
          '&amp;id=',
          encode-for-uri($id),
          '&amp;additional=',
          encode-for-uri($additional)
        )
      )/boolean/text()
    )" />

  </xsl:function>

  <xsl:function name="mcrpi:has-identifier-registration-started" as="xs:boolean">
    <xsl:param name="service" as="xs:string" />
    <xsl:param name="id" as="xs:string" />
    <xsl:param name="additional" as="xs:string" />

    <xsl:sequence select="xs:boolean(
      document(
        concat($piServiceURIPrefix,
          'hasIdentifierRegistrationStarted/?service=',
          encode-for-uri($service),
          '&amp;id=',
          encode-for-uri($id),
          '&amp;additional=',
          encode-for-uri($additional)
        )
      )/boolean/text()
    )" />
  </xsl:function>

  <xsl:function name="mcrpi:has-identifier-registered" as="xs:boolean">
    <xsl:param name="service" as="xs:string" />
    <xsl:param name="id" as="xs:string" />
    <xsl:param name="additional" as="xs:string" />

    <xsl:sequence select="xs:boolean(
      document(
        concat($piServiceURIPrefix,
          'hasIdentifierRegistered/?service=',
          encode-for-uri($service),
          '&amp;id=',
          encode-for-uri($id),
          '&amp;additional=',
          encode-for-uri($additional)
        )
      )/boolean/text()
    )" />

  </xsl:function>

  <xsl:function name="mcrpi:has-managed-pi" as="xs:boolean">
    <xsl:param name="objectID" as="xs:string" />

    <xsl:sequence select="xs:boolean(
      document(
        concat($piServiceURIPrefix,
          'hasManagedPI/?objectID=',
          encode-for-uri($objectID)
        )
      )/boolean/text()
    )" />
  </xsl:function>

  <xsl:function name="mcrpi:is-managed-pi" as="xs:boolean">
    <xsl:param name="pi" as="xs:string" />
    <xsl:param name="id" as="xs:string" />

    <xsl:sequence select="xs:boolean(
      document(
        concat($piServiceURIPrefix,
          'isManagedPI/?pi=',
          encode-for-uri($pi),
          '&amp;id=',
          encode-for-uri($id)
        )
      )/boolean/text()
    )" />
  </xsl:function>

  <xsl:function name="mcrpi:get-pi-service-information" as="element()?">
    <xsl:param name="objectID" as="xs:string" />

    <xsl:sequence select="
      document(
        concat($piServiceURIPrefix,
          'getPIServiceInformation/?objectID=',
          encode-for-uri($objectID)
        )
      )/list/service
    " />
  </xsl:function>


</xsl:stylesheet>
