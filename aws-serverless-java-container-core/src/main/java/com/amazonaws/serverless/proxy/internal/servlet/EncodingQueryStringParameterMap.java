package com.amazonaws.serverless.proxy.internal.servlet;


import com.amazonaws.serverless.proxy.internal.SecurityUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MultivaluedHashMap;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * query string parameters container. The main purpose of this object is to apply the required transformation to query
 * string parameters coming from API Gateway. Specifically, parameters names and values need to be un-escaped and url-encoded
 * again.
 */
public class EncodingQueryStringParameterMap extends MultivaluedHashMap<String, String> {

    private boolean isCaseSensitive;
    private String encoding;
    private static Logger log = LoggerFactory.getLogger(EncodingQueryStringParameterMap.class);

    private static final long serialVersionUID = 42L;

    /**
     * Creates a new instance of the parameters map. This allows the configuration to specify whether query string parameter
     * names should be case sensitive or not.
     * @param caseSensitive Whether parameters should be case sensitive. If the value is <code>true</code>, parameters names
     *  are automatically trnasformed to lower case as they are added to the map.
     */
    public EncodingQueryStringParameterMap(final boolean caseSensitive, final String enc) {
        isCaseSensitive = caseSensitive;
        encoding = enc;
    }

    public void putAllMapEncoding(final Map<String, String> parametersMap) {
        if (parametersMap == null) {
            return;
        }
        parametersMap.entrySet().stream().forEach(e -> {
            String key = e.getKey();
            if (!isCaseSensitive) {
                key = key.toLowerCase(Locale.getDefault());
            }
            key = unescapeAndEncode(key);

            String value = unescapeAndEncode(e.getValue());
            putSingle(key, value);
        });
    }

    public void putAllMultiValuedMapEncoding(final MultivaluedHashMap<String, String> parametersMap) {
        if (parametersMap == null) {
            return;
        }
        parametersMap.entrySet().stream().forEach(e -> {
            String key = e.getKey();
            if (!isCaseSensitive) {
                key = key.toLowerCase(Locale.getDefault());
            }
            key = unescapeAndEncode(key);

            List newValueList = new ArrayList();
            // we don't expect the values to be many so we don't parallel()
            e.getValue().stream().forEach(v -> {
                newValueList.add(unescapeAndEncode(v));
            });

            put(key, newValueList);
        });
    }

    private String unescapeAndEncode(final String value) {
        try {
            return URLEncoder.encode(value, encoding);
        } catch (UnsupportedEncodingException e) {
            log.error("Could not url encode parameter value: " + SecurityUtils.crlf(value), e);
        }

        return null;
    }
}
