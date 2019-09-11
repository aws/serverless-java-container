package com.amazonaws.serverless.proxy.internal.servlet;

import org.junit.Test;

import java.time.Instant;
import java.util.Enumeration;

import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.*;

public class AwsHttpSessionTest {

    @Test
    public void new_withNullId_throwsException() {
        try {
            AwsHttpSession session = new AwsHttpSession(null);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("cannot be null"));
            return;
        }
        fail("Did not throw exception with null ID");
    }

    @Test
    public void new_withValidId_setsIdCorrectly() {
        AwsHttpSession session = new AwsHttpSession("id");
        assertEquals("id", session.getId());
    }

    @Test
    public void new_creationTimePopulatedCorrectly() {
        AwsHttpSession session = new AwsHttpSession("id");
        assertTrue(session.getCreationTime() > Instant.now().getEpochSecond() - 1);
        assertEquals(AwsHttpSession.SESSION_DURATION_SEC, session.getMaxInactiveInterval());
        assertEquals(session.getLastAccessedTime(), session.getCreationTime());
    }

    @Test
    public void values_throwsUnsupportedOperationException() {
        int exCount = 0;
        AwsHttpSession sess = new AwsHttpSession("id");

        try {
            sess.putValue("test", "test");
        } catch (UnsupportedOperationException e) {
            exCount++;
        }
        try {
            sess.removeValue("test");
        } catch (UnsupportedOperationException e) {
            exCount++;
        }
        try {
            sess.getValue("test");
        } catch (UnsupportedOperationException e) {
            exCount++;
        }
        try {
            sess.getValueNames();
        } catch (UnsupportedOperationException e) {
            exCount++;
        }
        assertEquals(4, exCount);
    }

    @Test
    public void attributes_dataStoredCorrectly() throws InterruptedException {
        AwsHttpSession sess = new AwsHttpSession("id");
        sess.setAttribute("test", "test");
        sess.setAttribute("test2", "test2");
        Enumeration<String> attrs = sess.getAttributeNames();
        int attrsCnt = 0;
        while (attrs.hasMoreElements()) {
            attrs.nextElement();
            attrsCnt++;
        }
        assertEquals(2, attrsCnt);
        assertEquals(sess.getAttribute("test"), "test");
        sess.removeAttribute("test2");
        attrs = sess.getAttributeNames();
        attrsCnt = 0;
        while (attrs.hasMoreElements()) {
            attrs.nextElement();
            attrsCnt++;
        }
        assertEquals(1, attrsCnt);


        // changing attribute should touch the session
        Thread.sleep(1000);
        sess.setAttribute("test3", "test3");
        assertTrue(sess.getLastAccessedTime() > sess.getCreationTime());
    }

    @Test
    public void validSession_expectCorrectValidationOrInvalidation() throws InterruptedException {
        AwsHttpSession sess = new AwsHttpSession("id");
        assertTrue(sess.isValid());
        assertTrue(sess.isNew());

        Thread.sleep(1000);
        sess.setAttribute("test", "test");
        assertFalse(sess.isNew());
        sess.invalidate();
        assertFalse(sess.isValid());
        assertNull(sess.getAttribute("test"));
    }
}
