package org.mycore.services.imaging.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface MCRServletJobProcessing {
    public void process(HttpServletRequest request, HttpServletResponse response);
}
