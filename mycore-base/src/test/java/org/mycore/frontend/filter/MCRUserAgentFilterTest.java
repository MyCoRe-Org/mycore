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
