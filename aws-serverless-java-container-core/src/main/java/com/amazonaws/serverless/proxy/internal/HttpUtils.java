package com.amazonaws.serverless.proxy.internal;

import org.apache.commons.io.Charsets;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;

public final class HttpUtils {

    static final String HEADER_KEY_VALUE_SEPARATOR = "=";
    static final String HEADER_VALUE_SEPARATOR = ";";
    static final String ENCODING_VALUE_KEY = "charset";


    static public Charset parseCharacterEncoding(String contentTypeHeader,Charset defaultCharset) {
        // we only look at content-type because content-encoding should only be used for
        // "binary" requests such as gzip/deflate.
        if (contentTypeHeader == null) {
            return defaultCharset;
        }

        String[] contentTypeValues = contentTypeHeader.split(HEADER_VALUE_SEPARATOR);
        if (contentTypeValues.length <= 1) {
            return defaultCharset;
        }

        for (String contentTypeValue : contentTypeValues) {
            if (contentTypeValue.trim().startsWith(ENCODING_VALUE_KEY)) {
                String[] encodingValues = contentTypeValue.split(HEADER_KEY_VALUE_SEPARATOR);
                if (encodingValues.length <= 1) {
                    return defaultCharset;
                }
                try {
                    return Charsets.toCharset(encodingValues[1]);
                } catch (UnsupportedCharsetException ex) {
                    return defaultCharset;
                }
            }
        }
        return defaultCharset;
    }


    static public String appendCharacterEncoding(String currentContentType, String newEncoding) {
        if (currentContentType == null || currentContentType.trim().isEmpty()) {
            return null;
        }

        if (currentContentType.contains(HEADER_VALUE_SEPARATOR)) {
            String[] contentTypeValues = currentContentType.split(HEADER_VALUE_SEPARATOR);
            StringBuilder contentType = new StringBuilder(contentTypeValues[0]);

            for (int i = 1; i < contentTypeValues.length; i++) {
                String contentTypeValue = contentTypeValues[i];
                String contentTypeString = HEADER_VALUE_SEPARATOR + " " + contentTypeValue;
                if (contentTypeValue.trim().startsWith(ENCODING_VALUE_KEY)) {
                    contentTypeString = HEADER_VALUE_SEPARATOR + " " + ENCODING_VALUE_KEY + HEADER_KEY_VALUE_SEPARATOR + newEncoding;
                }
                contentType.append(contentTypeString);
            }

            return contentType.toString();
        } else {
            return currentContentType + HEADER_VALUE_SEPARATOR + " " + ENCODING_VALUE_KEY + HEADER_KEY_VALUE_SEPARATOR + newEncoding;
        }
    }
}
