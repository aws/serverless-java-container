package com.amazonaws.serverless.proxy.internal.testutils;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
