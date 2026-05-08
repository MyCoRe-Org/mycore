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

package org.mycore.frontend.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.glassfish.grizzly.servlet.HttpSessionImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mycore.test.MyCoReTest;

import static org.mockito.Mockito.when;

@MyCoReTest
public class MCRUserAgentFilterTest {

    @Test
    public void testDoFilter() throws Exception {
        HttpServletRequest sreq = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse sres = Mockito.mock(HttpServletResponse.class);
        FilterChain chain = Mockito.mock(FilterChain.class);
        HttpSessionImpl session = Mockito.mock(HttpSessionImpl.class);

        when(sreq.getHeader("User-Agent")).thenReturn("valid agent");
        when(sreq.getSession(false))
            .thenReturn(null)
            .thenReturn(session);
        when(sres.getOutputStream()).thenReturn(Mockito.mock(ServletOutputStream.class));

        MCRUserAgentFilter filter = new MCRUserAgentFilter();
        filter.init(null);

        // user agent valid
        filter.doFilter(sreq, sres, chain);
        Mockito.verify(session, Mockito.never()).invalidate();

        // user agent too few letters (MCR.Filter.UserAgent.MinLength)
        when(sreq.getHeader("User-Agent")).thenReturn("ag");
        when(sreq.getSession(false))
            .thenReturn(null)
            .thenReturn(session);
        filter.doFilter(sreq, sres, chain);
        Mockito.verify(session, Mockito.times(1)).invalidate();

        // user agent matches bot regex
        when(sreq.getHeader("User-Agent")).thenReturn("iamabot");
        when(sreq.getSession(false))
            .thenReturn(null)
            .thenReturn(session);
        filter.doFilter(sreq, sres, chain);
        Mockito.verify(session, Mockito.times(2)).invalidate();

        // user agent not fully matches bot regex
        when(sreq.getHeader("User-Agent")).thenReturn("thisisaresolve");
        when(sreq.getSession(false))
            .thenReturn(null)
            .thenReturn(session);
        filter.doFilter(sreq, sres, chain);
        Mockito.verify(session, Mockito.times(2)).invalidate();
    }
}
