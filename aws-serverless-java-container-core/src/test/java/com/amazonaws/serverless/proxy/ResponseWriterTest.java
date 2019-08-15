package com.amazonaws.serverless.proxy;

import com.amazonaws.serverless.exceptions.InvalidResponseObjectException;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletRequest;
import com.amazonaws.services.lambda.runtime.Context;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

import java.nio.ByteBuffer;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class ResponseWriterTest {
    private static int[][] NAUGHTY_STRINGS = {
       new int[] { 0b11111110 }, new int[] { 0xff }, new int[] {0xfe, 0xfe, 0xff, 0xff }
    };

    private static String[] VALID_STRINGS = {
         "ᚠᛇᚻ᛫ᛒᛦᚦ᛫ᚠᚱᚩᚠᚢᚱ᛫ᚠᛁᚱᚪ᛫ᚷᛖᚻᚹᛦᛚᚳᚢᛗ\nᛋᚳᛖᚪᛚ᛫ᚦᛖᚪᚻ᛫ᛗᚪᚾᚾᚪ᛫ᚷᛖᚻᚹᛦᛚᚳ᛫ᛗᛁᚳᛚᚢᚾ᛫ᚻᛦᛏ᛫ᛞᚫᛚᚪᚾ\nᚷᛁᚠ᛫ᚻᛖ᛫ᚹᛁᛚᛖ᛫ᚠᚩᚱ᛫ᛞᚱᛁᚻᛏᚾᛖ᛫ᛞᚩᛗᛖᛋ᛫ᚻᛚᛇᛏᚪᚾ᛬",
         "Τη γλώσσα μου έδωσαν ελληνική\nτο σπίτι φτωχικό στις αμμουδιές του Ομήρου.\nΜονάχη έγνοια η γλώσσα μου στις αμμουδιές του Ομήρου.",
         "ვეპხის ტყაოსანი შოთა რუსთაველი\nღმერთსი შემვედრე, ნუთუ კვლა დამხსნას სოფლისა შრომასა, ცეცხლს, წყალსა და მიწასა, ჰაერთა თანა მრომასა; მომცნეს ფრთენი და აღვფრინდე, მივჰხვდე მას ჩემსა ნდომასა, დღისით და ღამით ვჰხედვიდე მზისა ელვათა კრთომაასა.",
         "ಬಾ ಇಲ್ಲಿ ಸಂಭವಿಸು ಇಂದೆನ್ನ ಹೃದಯದಲಿ \nನಿತ್ಯವೂ ಅವತರಿಪ ಸತ್ಯಾವತಾರ\nಮಣ್ಣಾಗಿ ಮರವಾಗಿ ಮಿಗವಾಗಿ ಕಗವಾಗೀ... \nಮಣ್ಣಾಗಿ ಮರವಾಗಿ ಮಿಗವಾಗಿ ಕಗವಾಗಿ \nಭವ ಭವದಿ ಭತಿಸಿಹೇ ಭವತಿ ದೂರ \nನಿತ್ಯವೂ ಅವತರಿಪ ಸತ್ಯಾವತಾರ || ಬಾ ಇಲ್ಲಿ ||"
    };

    @Test
    public void isValidUtf8_testNaughtyStrings_allShouldFail() {
        MockResponseWriter rw = new MockResponseWriter();
        for (int[] s : NAUGHTY_STRINGS) {
            byte[] buf = new byte[s.length * 4];
            int pos = 0;
            for (int v : s) {
                for (byte b : convert2Bytes(v)) {
                    buf[pos] = b;
                    pos++;
                }
            }
            assertFalse(rw.isValidUtf8(buf));
        }
    }

    @Test
    public void isValidUtf8_testUtf8Strings_allShouldSucceed() {
        MockResponseWriter rw = new MockResponseWriter();
        for (String s : VALID_STRINGS) {
            assertTrue(rw.isValidUtf8(s.getBytes()));
        }
    }

    //little endian
    public static byte[] convert2Bytes(int src) {
        //an int is equivalent to 32 bits, 4 bytes
        byte tgt[] = new byte[4];
        int mask = 0377; /* 0377 in octal*/

        tgt[3] = (byte)(src >>> 24);
        tgt[2] = (byte)((src >> 16) & 0xff);
        tgt[1] = (byte)((src >> 8)  & 0xff);
        tgt[0] = (byte)(src & 0xff);

        return tgt;
    }

    public class MockResponseWriter extends ResponseWriter<AwsProxyHttpServletRequest, HttpServletRequest> {

        @Override
        public HttpServletRequest writeResponse(AwsProxyHttpServletRequest containerResponse, Context lambdaContext) throws InvalidResponseObjectException {
            return null;
        }

        public boolean testValidUtf8(final byte[] input) {
            return isValidUtf8(input);
        }
    }
}
