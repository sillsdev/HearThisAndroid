package org.sil.hearthis;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Unit tests for the IPv4 validation applied to scanned barcode contents.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class SyncActivityTest {

    @Test
    public void isValidIpv4_typicalAddresses() {
        assertTrue(SyncActivity.isValidIpv4("192.168.1.10"));
        assertTrue(SyncActivity.isValidIpv4("10.0.0.1"));
        assertTrue(SyncActivity.isValidIpv4("172.16.254.3"));
    }

    @Test
    public void isValidIpv4_boundaryValues() {
        assertTrue(SyncActivity.isValidIpv4("0.0.0.0"));
        assertTrue(SyncActivity.isValidIpv4("255.255.255.255"));
    }

    @Test
    public void isValidIpv4_null() {
        assertFalse(SyncActivity.isValidIpv4(null));
    }

    @Test
    public void isValidIpv4_empty() {
        assertFalse(SyncActivity.isValidIpv4(""));
    }

    @Test
    public void isValidIpv4_wrongNumberOfOctets() {
        assertFalse(SyncActivity.isValidIpv4("1.2.3"));
        assertFalse(SyncActivity.isValidIpv4("1.2.3.4.5"));
        assertFalse(SyncActivity.isValidIpv4("192"));
    }

    @Test
    public void isValidIpv4_emptyOctets() {
        assertFalse(SyncActivity.isValidIpv4("1..3.4"));
        assertFalse(SyncActivity.isValidIpv4(".1.2.3"));
        assertFalse(SyncActivity.isValidIpv4("1.2.3."));
        assertFalse(SyncActivity.isValidIpv4("..."));
    }

    @Test
    public void isValidIpv4_octetOutOfRange() {
        assertFalse(SyncActivity.isValidIpv4("256.1.1.1"));
        assertFalse(SyncActivity.isValidIpv4("1.1.1.999"));
        assertFalse(SyncActivity.isValidIpv4("1000.1.1.1"));
    }

    @Test
    public void isValidIpv4_nonDigits() {
        assertFalse(SyncActivity.isValidIpv4("a.b.c.d"));
        assertFalse(SyncActivity.isValidIpv4("1.2.3.x"));
        assertFalse(SyncActivity.isValidIpv4("-1.2.3.4"));
        assertFalse(SyncActivity.isValidIpv4("+1.2.3.4"));
        assertFalse(SyncActivity.isValidIpv4("1.2.3.4a"));
    }

    @Test
    public void isValidIpv4_whitespace() {
        assertFalse(SyncActivity.isValidIpv4(" 192.168.1.10"));
        assertFalse(SyncActivity.isValidIpv4("192.168.1.10 "));
        assertFalse(SyncActivity.isValidIpv4("192.168. 1.10"));
        assertFalse(SyncActivity.isValidIpv4("192.168.1.10\n"));
    }

    @Test
    public void isValidIpv4_leadingZeros() {
        assertFalse(SyncActivity.isValidIpv4("01.2.3.4"));
        assertFalse(SyncActivity.isValidIpv4("1.2.3.004"));
        assertFalse(SyncActivity.isValidIpv4("192.168.001.010"));
    }

    @Test
    public void isValidIpv4_otherFormats() {
        assertFalse(SyncActivity.isValidIpv4("localhost"));
        assertFalse(SyncActivity.isValidIpv4("example.com"));
        assertFalse(SyncActivity.isValidIpv4("::1"));
        assertFalse(SyncActivity.isValidIpv4("192.168.1.10:8080"));
        assertFalse(SyncActivity.isValidIpv4("http://192.168.1.10"));
    }
}
