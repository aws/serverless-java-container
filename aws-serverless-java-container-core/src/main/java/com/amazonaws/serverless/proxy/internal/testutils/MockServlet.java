package com.amazonaws.serverless.proxy.internal.testutils;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MockServlet extends HttpServlet {

    private int serviceCalls = 0;

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.service(req, resp);
        serviceCalls++;
    }

    public int getServiceCalls() {
        return serviceCalls;
    }
}
