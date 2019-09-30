package com.amazonaws.serverless.proxy.internal;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SecurityUtilsTest {

    private static final HashMap<String, String> NAUGHTY_UNICODE_STRINGS = new HashMap<>();
    static {
        NAUGHTY_UNICODE_STRINGS.put("Ω≈ç√∫˜µ≤≥÷", "\\u03A9\\u2248\\u00E7\\u221A\\u222B\\u02DC\\u00B5\\u2264\\u2265\\u00F7");
        NAUGHTY_UNICODE_STRINGS.put("åß∂ƒ©˙∆˚¬…æ", "\\u00E5\\u00DF\\u2202\\u0192\\u00A9\\u02D9\\u2206\\u02DA\\u00AC\\u2026\\u00E6");
        NAUGHTY_UNICODE_STRINGS.put("œ∑´®†¥¨ˆøπ“‘", "\\u0153\\u2211\\u00B4\\u00AE\\u2020\\u00A5\\u00A8\\u02C6\\u00F8\\u03C0\\u201C\\u2018");
        NAUGHTY_UNICODE_STRINGS.put("¡™£¢∞§¶•ªº–≠", "\\u00A1\\u2122\\u00A3\\u00A2\\u221E\\u00A7\\u00B6\\u2022\\u00AA\\u00BA\\u2013\\u2260");
        NAUGHTY_UNICODE_STRINGS.put("¸˛Ç◊ı˜Â¯˘¿", "\\u00B8\\u02DB\\u00C7\\u25CA\\u0131\\u02DC\\u00C2\\u00AF\\u02D8\\u00BF");
        NAUGHTY_UNICODE_STRINGS.put("ÅÍÎÏ˝ÓÔÒÚÆ☃", "\\u00C5\\u00CD\\u00CE\\u00CF\\u02DD\\u00D3\\u00D4\\uF8FF\\u00D2\\u00DA\\u00C6\\u2603");
        NAUGHTY_UNICODE_STRINGS.put("Œ„´‰ˇÁ¨ˆØ∏”’", "\\u0152\\u201E\\u00B4\\u2030\\u02C7\\u00C1\\u00A8\\u02C6\\u00D8\\u220F\\u201D\\u2019");
        NAUGHTY_UNICODE_STRINGS.put("⅛⅜⅝⅞", "\\u215B\\u215C\\u215D\\u215E");
        NAUGHTY_UNICODE_STRINGS.put("ЁЂЃЄЅІЇЈЉЊЋЌЍЎЏАБВГДЕЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдежзийклмнопрстуфхцчшщъыьэюя", "\\u0401\\u0402\\u0403\\u0404\\u0405\\u0406\\u0407\\u0408\\u0409\\u040A\\u040B\\u040C\\u040D\\u040E\\u040F\\u0410\\u0411\\u0412\\u0413\\u0414\\u0415\\u0416\\u0417\\u0418\\u0419\\u041A\\u041B\\u041C\\u041D\\u041E\\u041F\\u0420\\u0421\\u0422\\u0423\\u0424\\u0425\\u0426\\u0427\\u0428\\u0429\\u042A\\u042B\\u042C\\u042D\\u042E\\u042F\\u0430\\u0431\\u0432\\u0433\\u0434\\u0435\\u0436\\u0437\\u0438\\u0439\\u043A\\u043B\\u043C\\u043D\\u043E\\u043F\\u0440\\u0441\\u0442\\u0443\\u0444\\u0445\\u0446\\u0447\\u0448\\u0449\\u044A\\u044B\\u044C\\u044D\\u044E\\u044F");
        NAUGHTY_UNICODE_STRINGS.put("\bhello\nhello\thello\fhello\r", "\\bhello\\nhello\\thello\\fhello\\r");
        NAUGHTY_UNICODE_STRINGS.put("\'", "\'");
        NAUGHTY_UNICODE_STRINGS.put("\"", "\\\"");
        NAUGHTY_UNICODE_STRINGS.put("\\", "\\\\");
        NAUGHTY_UNICODE_STRINGS.put("ò", "\\u00F2");
    }


    @Test
    public void encode_nullString_returnsNullIfStringIsNull() {
        assertNull(SecurityUtils.encode(null));
    }

    @Test
    public void encode_naughtyStrings_encodedCorrectly() {
        for (Map.Entry<String, String> e : NAUGHTY_UNICODE_STRINGS.entrySet()) {
            assertEquals(e.getValue(), SecurityUtils.encode(e.getKey()));
        }
    }

    @Test
    public void getValidFilePath_nullOrEmpty_returnsNull() {
        assertNull(SecurityUtils.getValidFilePath(""));
        assertNull(SecurityUtils.getValidFilePath(null));
    }

    @Test
    public void getValidFilePath_writeToTaskPath_throwsIllegalArgumentException() {
        boolean thrown = false;
        try {
            SecurityUtils.getValidFilePath("/var/task/test.txt", true);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        if (!thrown) {
            fail("Did not throw exception");
        }

        try {
            SecurityUtils.getValidFilePath("file:///var/task/test.txt", true);
        } catch (IllegalArgumentException e) {
            return;
        }

        fail();
    }

    @Test
    public void getValidFilePath_writeToBlockedPath_throwsIllegalArgumentException() {
        try {
            SecurityUtils.getValidFilePath("/usr/lib/test.txt");
        } catch (IllegalArgumentException e) {
            return;
        }
        fail("Did not throw exception");
    }
}
