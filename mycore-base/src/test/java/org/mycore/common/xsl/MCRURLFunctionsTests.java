/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.common.xsl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mycore.common.util.MCRTestCaseXSLTUtil;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRURLFunctionsTests {

    private static final String URL_PARAM_NAME = "url";
    private static final String PAR_PARAM_NAME = "par";
    private static final String VALUE_PARAM_NAME = "value";
    private static final String URL_PREFIX = "http://mycore.org";

    @ParameterizedTest(name = "[{index}] {0} | par={1} → \"{2}\"")
    @CsvSource({
        "'?foo=bar', foo, bar",
        "'?foo=bar&os=linux&x=1', os, linux",
        "'?my-param=os', my-param, os",
        // missing parameter
        "'?foo=bar', os, ''",
        // no query string
        "'', foo, ''",
        // empty param
        "'?x=1&foo', foo, ''",
        // empty value
        "'?foo=', foo, ''",
        // multiple params with the same name (only first is returned)
        "'?x=1&foo=bar&foo=baz', foo, 'bar'",
        // url encoded value (no decoding expected)
        "'?foo=bar%20os', foo, bar%20os",
        // duplicate params (first wins)
        "'?foo=bar&foo=os', foo, bar",
        // key without value
        "'?foo', foo, ''"
    })
    @DisplayName("mcrurl:get-param")
    public void testGetParam(String queryUrl, String param, String expected) throws Exception {
        assertEquals(expected, getParam(URL_PREFIX + queryUrl, param));
    }

    @ParameterizedTest(name = "[{index}] {0} | {1}={2} → \"{3}\"")
    @CsvSource({
        // add new param to empty URL
        "'', foo, bar, '?foo=bar'",
        // add param to existing query
        "'?foo=bar', os, linux, '?foo=bar&os=linux'",
        // overwrite existing param
        "'?foo=bar', foo, os, '?foo=os'",
        // multiple params already present
        "'?foo=bar&x=1', os, linux, '?foo=bar&x=1&os=linux'",
        // multiple params with same name (only first will be replaced)
        "'?foo=bar&x=1&foo=baz', foo, ber, '?foo=ber&x=1&foo=baz'",
        // empty value
        "'?foo=bar', foo, '', '?foo='",
        // url encoding (value unchanged expected here)
        "'?foo=bar', foo, bar os, '?foo=bar os'",
        // parameter without existing query
        "'', os, linux, '?os=linux'",
        // special key
        "'?my-param=bar', my-param, os, '?my-param=os'"
    })
    @DisplayName("mcrurl:set-param")
    public void testSetParam(String queryUrl, String param, String value, String expected) throws Exception {
        assertEquals(URL_PREFIX + expected, setParam(URL_PREFIX + queryUrl, param, value));
    }

    @ParameterizedTest(name = "[{index}] {0} | del {1} → \"{2}\"")
    @CsvSource({
        // remove existing param
        "'?foo=bar', foo, ''",
        // remove one of multiple params
        "'?foo=bar&os=linux', foo, '?os=linux'",
        // remove middle param (order preserved)
        "'?foo=bar&os=linux&x=1', os, '?foo=bar&x=1'",
        // param does not exist
        "'?foo=bar', os, '?foo=bar'",
        // empty query input
        "'', foo, ''",
        // special key
        "'?my-param=os', my-param, ''",
        // duplicate keys (removal all)
        "'?foo=bar&foo=os', foo, ''",
        // fragment
        "'?foo=bar&foz=baz#frag?', foo, '?foz=baz#frag?'",
        // keep fragment
        "'#foo', foo, '#foo'"
    })
    @DisplayName("mcrurl:del-param")
    public void testDelParam(String queryUrl, String param, String expected) throws Exception {
        assertEquals(URL_PREFIX + expected, delParam(URL_PREFIX + queryUrl, param));
    }

    private String getParam(String url, String param) throws TransformerException {
        return callFunction("get-param", Map.of(
            URL_PARAM_NAME, url,
            PAR_PARAM_NAME, param
        ));
    }

    private String setParam(String url, String param, String value) throws TransformerException {
        return callFunction("set-param", Map.of(
            URL_PARAM_NAME, url,
            PAR_PARAM_NAME, param,
            VALUE_PARAM_NAME, value
        ));
    }

    private String delParam(String url, String param) throws TransformerException {
        return callFunction("del-param", Map.of(
            URL_PARAM_NAME, url,
            PAR_PARAM_NAME, param
        ));
    }

    private String callFunction(String name, Map<String, String> params) throws TransformerException {
        Map<String, Object> parameters = new HashMap<>(params);
        parameters.put("fn-name", name);
        return MCRTestCaseXSLTUtil.transform("/xslt/functions/url-test.xsl", parameters).getRootElement().getText();
    }
}
