package com.amazonaws.serverless.proxy.spring.extensibility;

import org.springframework.context.ApplicationContext;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CustomServlet extends HttpServlet {
    private ApplicationContext appCtx;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.getWriter().print("Unittest " + (appCtx!=null ? appCtx.getDisplayName() : ""));
    }

    public void setAppCtx(ApplicationContext appCtx) {
        this.appCtx = appCtx;
    }
}
