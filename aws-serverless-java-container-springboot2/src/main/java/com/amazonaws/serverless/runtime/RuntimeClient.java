package com.amazonaws.serverless.runtime;

import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.SecurityUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.HttpHeaders;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class RuntimeClient {
    public static final String API_VERSION = "2018-06-01";
    public static final String API_PROTOCOL = "http";
    public static final String API_HOST_VAR_NAME = "AWS_LAMBDA_RUNTIME_API";

    private static final String REQUEST_ID_HEADER = "Lambda-Runtime-Aws-Request-Id";
    private static final String DEADLINE_HEADER = "Lambda-Runtime-Deadline-Ms";
    private static final String FUNCTION_ARN_HEADER = "Lambda-Runtime-Invoked-Function-Arn";
    private static final String TRACE_ID_HEADER = "Lambda-Runtime-Trace-Id";

    private final Logger log = LoggerFactory.getLogger(RuntimeClient.class);

    private final String apiHost;
    private final URL nextEventUrl;

    public RuntimeClient() {
        try {
            apiHost = System.getenv(API_HOST_VAR_NAME);
            nextEventUrl = new URL(API_PROTOCOL + "://" + apiHost + "/" + API_VERSION + "/runtime/invocation/next");
            InvocationContext.prepareContext();
        } catch (SecurityException e) {
            log.error("Security Exception while reading runtime API host environment variable", e);
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize runtime client", e);
        } catch (MalformedURLException e) {
            log.error("Could not construct URL for runtime API endpoint", e);
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize runtime client", e);
        } catch (NumberFormatException e) {
            log.error("Could not parse memory limit from environment variable", e);
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize runtime client", e);
        }
    }

    public InvocationRequest getNextEvent() throws RuntimeClientException {
        try {
            HttpURLConnection conn = (HttpURLConnection)nextEventUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.setReadTimeout(0);
            conn.setConnectTimeout(0);
            conn.setRequestProperty(HttpHeaders.ACCEPT, "application/json");

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                String errMsg = "Could not retrieve next event. Status code: " + conn.getResponseCode();
                if (conn.getErrorStream() != null) {
                    errMsg += "\n" + String.join("\n", IOUtils.readLines(conn.getErrorStream()));
                }
                throw new RuntimeClientException(
                        errMsg,
                        null,
                        true);
            }

            InvocationRequest req = new InvocationRequest();
            req.setEvent(conn.getInputStream());

            String reqId = conn.getHeaderField(REQUEST_ID_HEADER);
            long deadline = Long.parseLong(conn.getHeaderField(DEADLINE_HEADER));
            String arn = conn.getHeaderField(FUNCTION_ARN_HEADER);
            String trace = conn.getHeaderField(TRACE_ID_HEADER);
            if (reqId == null || arn == null || trace == null) {
                throw new RuntimeClientException("Could not read event header fields", null, true);
            }
            req.setContext(new InvocationContext(reqId, deadline, arn, trace));

            return req;
        } catch (IOException e) {
            log.error("Error while requesting next event", e);
            throw new RuntimeClientException("Error while requesting next event", e, true);
        } catch (NumberFormatException e) {
            log.error("Could not parse deadline ms value", e);
            throw new RuntimeClientException("Error while requesting next event", e, true);
        }
    }

    @SuppressFBWarnings("CRLF_INJECTION_LOGS")
    public void postInvocationResponse(String reqId, OutputStream out) throws RuntimeClientException {
        try {
            URL responseUrl = new URL(API_PROTOCOL + "://" + apiHost + "/" + API_VERSION + "/runtime/invocation/" + reqId + "/response");
            HttpURLConnection conn = (HttpURLConnection)responseUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            if (out instanceof ByteArrayOutputStream) {
                conn.getOutputStream().write(((ByteArrayOutputStream)out).toByteArray());
            } else {
                throw new RuntimeClientException("Only byte array output streams are supported " + out.getClass().getName(), null, false);
            }
            conn.getOutputStream().close();
            if (conn.getResponseCode() != HttpURLConnection.HTTP_ACCEPTED) {
                throw new RuntimeClientException("Could not send invocation response (" + conn.getResponseCode() + ")", null, true);
            }
        } catch (MalformedURLException e) {
            log.error("Could not construct invocation response url for " + SecurityUtils.crlf(reqId), e);
            throw new RuntimeClientException("Error while posting invocation response", e, false);
        } catch (IOException e) {
            log.error("Could not write to runtime API connection for " + SecurityUtils.crlf(reqId), e);
            throw new RuntimeClientException("Error while posting invocation response", e, true);
        }
    }

    @SuppressFBWarnings("CRLF_INJECTION_LOGS")
    public void postInvocationError(String reqId, Throwable error)  throws RuntimeClientException {
        try {
            URL errorUrl = new URL(API_PROTOCOL + "://" + apiHost + "/" + API_VERSION + "/runtime/invocation/" + reqId + "/error");
            HttpURLConnection conn = (HttpURLConnection)errorUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            InvocationError err = new InvocationError(error.getMessage(), error.getClass().getSimpleName());
            conn.getOutputStream().write(LambdaContainerHandler.getObjectMapper().writeValueAsBytes(err));
            conn.getOutputStream().close();
            if (conn.getResponseCode() != HttpURLConnection.HTTP_ACCEPTED) {
                throw new RuntimeClientException("Could not send invocation response (" + conn.getResponseCode() + ")", null, true);
            }
        } catch (MalformedURLException e) {
            log.error("Could not construct invocation error url for " + SecurityUtils.crlf(reqId), e);
            throw new RuntimeClientException("Error while posting invocation error", e, false);
        } catch (IOException e) {
            log.error("Could not write to runtime API connection for " + SecurityUtils.crlf(reqId), e);
            throw new RuntimeClientException("Error while posting invocation error", e, true);
        }
    }

    public void reportInitError(Throwable error)  throws RuntimeClientException {
        try {
            URL errorUrl = new URL(API_PROTOCOL + "://" + apiHost + "/" + API_VERSION + "/runtime/init/error");
            HttpURLConnection conn = (HttpURLConnection)errorUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            InvocationError err = new InvocationError(error.getMessage(), error.getClass().getSimpleName());
            conn.getOutputStream().write(LambdaContainerHandler.getObjectMapper().writeValueAsBytes(err));
            conn.getOutputStream().close();
            if (conn.getResponseCode() != HttpURLConnection.HTTP_ACCEPTED) {
                throw new RuntimeClientException("Could not send invocation response (" + conn.getResponseCode() + ")", null, true);
            }
        } catch (MalformedURLException e) {
            log.error("Could not construct init error url", e);
            throw new RuntimeClientException("Error while posting init error", e, false);
        } catch (IOException e) {
            log.error("Could not write init error to runtime API", e);
            throw new RuntimeClientException("Error while posting init error", e, true);
        }
    }

    static class InvocationError {
        private String errorMessage;
        private String errorType;

        public InvocationError(String errorMessage, String errorType) {
            this.errorMessage = errorMessage;
            this.errorType = errorType;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public String getErrorType() {
            return errorType;
        }

        public void setErrorType(String errorType) {
            this.errorType = errorType;
        }
    }
}
