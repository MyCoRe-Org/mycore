<!--
  ~ This file is part of ***  M y C o R e  ***
  ~ See http://www.mycore.de/ for details.
  ~
  ~ MyCoRe is free software: you can redistribute it and/or modify
  ~ it under the terms of the GNU General Public License as published by
  ~ the Free Software Foundation, either version 3 of the License, or
  ~ (at your option) any later version.
  ~
  ~ MyCoRe is distributed in the hope that it will be useful,
  ~ but WITHOUT ANY WARRANTY; without even the implied warranty of
  ~ MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  ~ GNU General Public License for more details.
  ~
  ~ You should have received a copy of the GNU General Public License
  ~ along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
  -->

<!-- This stylesheet performs an identity transformation,
     except that it also changes the namespace declarations
     in the result such that they all appear on the document
     element, while preserving all the same effective element
     and attribute node names througout the document.
-->
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:nn="http://lenzconsulting.com/namespace-normalizer"
                exclude-result-prefixes="xs nn">

    <!-- Set to true to print debugging info -->
    <xsl:param name="DEBUG" select="false()"/>

    <!-- By default, namespace-normalize the input -->
    <xsl:template match="/">
        <xsl:copy-of select="nn:normalize(.)"/>
    </xsl:template>

    <!-- This is the main function of interest -->
    <xsl:function name="nn:normalize" as="document-node()">
        <xsl:param name="doc" as="document-node()"/>
        <xsl:sequence select="nn:normalize($doc,(),false())"/>
    </xsl:function>

    <!-- You can optionally supply a set of preferred prefix/URI mappings;
         the expected format of $ns-prefs is as follows:

           <ns prefix=""    uri="http://example.com"/>
           <ns prefix="foo" uri="http://example.com/ns"/>
           ...

         The <ns> can actually be any element name.
         Only the attribute names are important ("prefix" and "uri").
    -->
    <xsl:function name="nn:normalize" as="document-node()">
        <xsl:param name="doc"      as="document-node()"/>
        <xsl:param name="ns-prefs" as="element()*"/>

        <!-- Set to true if you want to disallow the use of preferred prefixes
             except as specifically allowed, e.g. if "foo" appears in the
             document bound to a different namespace than the one you specified.

             This treats "" as a prefix too, which means that if $ns-prefs
             includes a default namespace, then this could cause the result to
             have xmlns="" un-declarations or default declarations *not* on
             the document element (if unqualified ancestors are present).
        -->
        <xsl:param name="disallow-other-uses-of-preferred-prefixes" as="xs:boolean"/>

        <!-- Get the new, normalized namespace nodes for this document -->
        <xsl:variable name="ns-nodes" as="item()*">
            <xsl:apply-templates mode="new-namespace-nodes" select="$doc">
                <xsl:with-param name="ns-prefs" select="$ns-prefs" tunnel="yes"/>
                <xsl:with-param name="disallow-other-uses"
                                select="$disallow-other-uses-of-preferred-prefixes"
                                tunnel="yes"/>
            </xsl:apply-templates>
        </xsl:variable>
        <!-- Return a new document with the normalized namespace nodes applied -->
        <xsl:document>
            <xsl:apply-templates mode="normalize-namespaces" select="$doc">
                <xsl:with-param name="ns-nodes" select="$ns-nodes" tunnel="yes"/>
            </xsl:apply-templates>
        </xsl:document>
    </xsl:function>


    <!-- The first namespace node for each unique namespace URI -->
    <xsl:function name="nn:unique-uri-namespace-nodes">
        <xsl:param name="doc" as="document-node()"/>
        <xsl:sequence select="for $uri in distinct-values($doc//namespace::*[not(name() eq 'xml')])
                          return ($doc//namespace::*[. eq $uri])[1]"/>
    </xsl:function>


    <!-- These candidate bindings disallow default declarations
         for namespaces that need a prefix, but they don't yet
         prevent duplicate prefixes; that comes later. -->
    <xsl:template mode="candidate-bindings-doc" match="/">
        <xsl:param name="ns-prefs" as="element()*" tunnel="yes"/>
        <xsl:param name="disallow-other-uses" tunnel="yes"/>
        <xsl:document>
            <xsl:for-each select="nn:unique-uri-namespace-nodes(.)">
                <!-- Process the pre-existing URIs (from the user-supplied list) first;
                     that way, their prefixes will have precedence when removing conflicts. -->
                <xsl:sort select="if (. = $ns-prefs/@uri) then 'first' else 'last'"/>

                <!-- is this a default namespace? -->
                <xsl:variable name="is-default" select="not(name(.))"/>

                <!-- must the URI have a prefix? -->
                <!--
                 If there are any unqualified element names
                 then force default namespaces to use a prefix,
                 because we want to guarantee that the only
                 namespace declarations in our result will be
                 attached to the root element.

                 The exception is when we are forcing a different
                 default namespace. In that case, the guarantee
                 no longer applies.
                -->
                <xsl:variable name="cannot-be-default" as="xs:boolean">
                    <xsl:variable name="unqualified-elements-are-present" select="//*[not(namespace-uri())]"/>
                    <xsl:variable name="force-the-preferred-default-namespace"
                                  select="$ns-prefs[@prefix eq ''] and $disallow-other-uses"/>
                    <xsl:sequence select="$unqualified-elements-are-present and
                                not($force-the-preferred-default-namespace)"/>
                </xsl:variable>

                <xsl:choose>
                    <!-- do we need to force a non-empty prefix? -->
                    <xsl:when test="$is-default and $cannot-be-default">
                        <xsl:copy-of select="nn:binding-with-nonempty-prefix(., $ns-prefs)"/>
                    </xsl:when>

                    <!-- otherwise, we can use the existing (possibly empty) prefix -->
                    <xsl:otherwise>
                        <xsl:variable name="preferred-prefix"
                                      select="nn:choose-prefix(., $ns-prefs, name(.), false())"/>
                        <binding>
                            <uri>
                                <xsl:value-of select="."/>
                            </uri>
                            <prefix>
                                <xsl:value-of select="$preferred-prefix"/>
                            </prefix>
                        </binding>

                        <!-- Create an additional namespace node if needed specifically for qualified attributes -->
                        <xsl:if test="not($preferred-prefix) and //@*[namespace-uri() eq current()]">
                            <xsl:copy-of select="nn:binding-with-nonempty-prefix(., $ns-prefs)"/>
                        </xsl:if>

                    </xsl:otherwise>
                </xsl:choose>
            </xsl:for-each>
        </xsl:document>
    </xsl:template>

    <xsl:function name="nn:binding-with-nonempty-prefix" as="element(binding)">
        <xsl:param name="ns-node"/>
        <xsl:param name="ns-prefs"/>
        <!-- is there an existing prefix from the document we can use? -->
        <xsl:variable name="prefix-from-document"
                      select="name((root($ns-node)//namespace::*[. eq $ns-node][name()])[1])"/>
        <binding>
            <uri>
                <xsl:value-of select="$ns-node"/>
            </uri>
            <xsl:variable name="preferred-prefix" select="nn:choose-prefix($ns-node, $ns-prefs, $prefix-from-document, true())"/>
            <xsl:choose>
                <xsl:when test="$preferred-prefix">
                    <!-- If a suitable prefix is found, use it! -->
                    <prefix>
                        <xsl:value-of select="$preferred-prefix"/>
                    </prefix>
                </xsl:when>
                <xsl:otherwise>
                    <!-- Otherwise, leave a note to ourselves that we need to generate a new prefix -->
                    <generate-prefix/>
                </xsl:otherwise>
            </xsl:choose>
        </binding>
    </xsl:function>

    <!-- Use the user-supplied preferred prefixes whenever possible -->
    <xsl:function name="nn:choose-prefix" as="xs:string">
        <xsl:param name="ns-node"/>
        <xsl:param name="ns-prefs"          as="element()*"/>
        <xsl:param name="given-prefix"      as="xs:string"/>
        <xsl:param name="nonempty-required" as="xs:boolean"/>
        <!-- If the URI has a preferred prefix, then use it; otherwise, use the given prefix -->
        <xsl:choose>
            <xsl:when test="$ns-node = $ns-prefs/@uri">
                <!-- Use the preferred prefix, unless it's empty (default) and required to be non-empty. -->
                <xsl:variable name="preferred-prefix" select="$ns-prefs[@uri eq $ns-node][1]/@prefix/string(.)"/>
                <xsl:sequence select="if ($nonempty-required and not($preferred-prefix))
                                      then $given-prefix
                                      else $preferred-prefix"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:sequence select="$given-prefix"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>


    <!-- Remove conflicts (same prefix occurring more than once) -->
    <xsl:template mode="unconflicted-bindings-doc" match="/">
        <xsl:variable name="candidate-bindings-doc">
            <xsl:apply-templates mode="candidate-bindings-doc" select="."/>
        </xsl:variable>
        <xsl:document>
            <xsl:apply-templates mode="remove-conflicts" select="$candidate-bindings-doc/binding"/>
        </xsl:document>
    </xsl:template>

    <!-- Generate a prefix if it's already being used -->
    <xsl:template mode="remove-conflicts" match="prefix[. = preceding::prefix]">
        <generate-prefix/>
    </xsl:template>

    <!-- By default, copy the bindings as is -->
    <xsl:template mode="remove-conflicts" match="@* | node()">
        <xsl:copy>
            <xsl:apply-templates mode="#current" select="@* | node()"/>
        </xsl:copy>
    </xsl:template>


    <!-- Create the final list of bindings, removing any remaining redundant
         declarations (those which could only come about because an extra declaration
         was added to handle qualified attributes but has since become unnecessary).
    -->
    <xsl:template mode="final-bindings-doc" match="/">
        <xsl:param name="disallow-other-uses" as="xs:boolean" tunnel="yes"/>
        <xsl:document>
            <xsl:variable name="unconflicted-bindings-doc">
                <xsl:apply-templates mode="unconflicted-bindings-doc" select="."/>
            </xsl:variable>
            <xsl:variable name="redundancies-removed">
                <xsl:apply-templates mode="remove-redundancies"
                                     select="$unconflicted-bindings-doc/binding"/>
            </xsl:variable>
            <xsl:choose>
                <!-- When the user demands that their preferred prefixes not be used
                     for anything but the specified URIs, we make one additional pass. -->
                <xsl:when test="$disallow-other-uses">
                    <xsl:apply-templates mode="remove-disallowed" select="$redundancies-removed"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:copy-of select="$redundancies-removed"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:document>
    </xsl:template>

    <!-- Remove a generated-prefix in favor of an already-present, non-empty prefix -->
    <xsl:template mode="remove-redundancies"
                  match="binding[generate-prefix][../binding[uri eq current()/uri][string(prefix)]]"
                  priority="1"/>

    <!-- Or, if there's more than one generated-prefix for the same URI, we only need one of them -->
    <xsl:template mode="remove-redundancies"
                  match="binding[generate-prefix][uri = preceding::uri[../generate-prefix]]"/>

    <!-- By default, copy the bindings as is -->
    <xsl:template mode="remove-redundancies
                              remove-disallowed" match="@* | node()">
        <xsl:copy>
            <xsl:apply-templates mode="#current" select="@* | node()"/>
        </xsl:copy>
    </xsl:template>


    <!-- Change to <generate-prefix/> if the prefix is reserved for a different URI -->
    <xsl:template mode="remove-disallowed" match="prefix">
        <xsl:param name="ns-prefs" tunnel="yes"/>
        <xsl:choose>
            <xsl:when test="some $pref in $ns-prefs satisfies (.      eq $pref/@prefix
                                                             and ../uri ne $pref/@uri)">
                <generate-prefix/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy-of select="."/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>


    <!-- Generate a namespace node for each of the final bindings -->
    <xsl:template mode="new-namespace-nodes" match="/">
        <xsl:param name="disallow-other-uses" tunnel="yes"/>
        <xsl:param name="ns-prefs" tunnel="yes"/>
        <!-- print a DEBUG message, if applicable -->
        <xsl:if test="$DEBUG">
            <xsl:message>
                <xsl:apply-templates mode="diagnostics" select="."/>
            </xsl:message>
        </xsl:if>
        <xsl:variable name="final-bindings-doc">
            <xsl:apply-templates mode="final-bindings-doc" select="."/>
        </xsl:variable>
        <xsl:for-each select="$final-bindings-doc/binding">
            <xsl:variable name="prefix">
                <xsl:choose>
                    <xsl:when test="generate-prefix">
                        <!-- Generate in the form "ns1", "ns2", etc. -->
                        <xsl:variable name="auto-prefix"
                                      select="concat('ns',1+count(preceding::generate-prefix))"/>
                        <xsl:variable name="already-taken" select="$auto-prefix = ../binding/prefix
                                                   or ($auto-prefix = $ns-prefs/@prefix and $disallow-other-uses)"/>

                        <!-- But if the document already has "ns1", etc. then punt and call generate-id() -->
                        <xsl:sequence select="if (not($already-taken))
                                  then $auto-prefix
                                  else generate-id(.)"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="prefix"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <xsl:namespace name="{$prefix}" select="string(uri)"/>
        </xsl:for-each>
    </xsl:template>


    <!-- Give every element the same namespace nodes (the ones we've decided on above) -->
    <xsl:template match="*" mode="normalize-namespaces">
        <xsl:param name="ns-nodes" tunnel="yes"/>
        <xsl:element name="{nn:new-qname(.,$ns-nodes)}" namespace="{namespace-uri()}">
            <!-- except, don't insert the default namespace (i.e. where name() is empty)
                 if this element is unqualified (i.e. where namespace-uri(current()) is empty).
                 This scenario only occurs when $disallow-other-uses is set to true.
            -->
            <xsl:copy-of select="$ns-nodes[name() or namespace-uri(current())]"/>
            <xsl:apply-templates mode="#current" select="@* | node()"/>
        </xsl:element>
    </xsl:template>

    <!-- Replicate attributes -->
    <xsl:template match="@*" mode="normalize-namespaces">
        <xsl:param name="ns-nodes" tunnel="yes"/>
        <xsl:attribute name="{nn:new-qname(.,$ns-nodes)}" namespace="{namespace-uri()}" select="."/>
    </xsl:template>

    <!-- Do a simple copy of the other nodes -->
    <xsl:template match="text() | comment() | processing-instruction()" mode="normalize-namespaces">
        <xsl:copy/>
    </xsl:template>

    <!-- Get the lexical QName based on the bindings we've chosen -->
    <xsl:function name="nn:new-qname">
        <xsl:param name="node"/>
        <xsl:param name="ns-nodes"/>
        <xsl:variable name="prefix" select="$ns-nodes[. eq namespace-uri($node)]
                                                 [if ($node instance of element())
                                                  then 1       (: preferred prefix for elements comes first :)
                                                  else last()  (: but attributes *must* use a prefix (comes last) :)
                                                 ]
                                                 /name(.)"/>
        <xsl:variable name="maybe-colon" select="if ($prefix) then ':' else ''"/>
        <xsl:sequence select="concat($prefix, $maybe-colon, local-name($node))"/>
    </xsl:function>

    <!-- Print out some diagnostics to show what's going on beneath the covers. -->
    <xsl:template mode="diagnostics" match="/">
        <diagnostics>
            <diagnostic name="candidate-bindings-doc">
                <xsl:apply-templates mode="candidate-bindings-doc" select="."/>
            </diagnostic>
            <diagnostic name="unconflicted-bindings-doc">
                <xsl:apply-templates mode="unconflicted-bindings-doc" select="."/>
            </diagnostic>
            <diagnostic name="final-bindings-doc">
                <xsl:apply-templates mode="final-bindings-doc" select="."/>
            </diagnostic>
        </diagnostics>
    </xsl:template>

</xsl:stylesheet>