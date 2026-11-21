package org.sil.hearthis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;

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

    private static SyncActivity.AddressCandidate candidate(String ip, boolean pointToPoint) throws Exception {
        return new SyncActivity.AddressCandidate(InetAddress.getByName(ip), pointToPoint);
    }

    @Test
    public void pickBestAddress_prefersLanOverCellularRegardlessOfOrder() throws Exception {
        // Cellular (point-to-point, CGNAT range) listed before Wi-Fi.
        assertEquals("192.168.1.20", SyncActivity.pickBestAddress(Arrays.asList(
                candidate("100.72.5.9", true), candidate("192.168.1.20", false))));
        // Same result with the order reversed.
        assertEquals("192.168.1.20", SyncActivity.pickBestAddress(Arrays.asList(
                candidate("192.168.1.20", false), candidate("100.72.5.9", true))));
    }

    @Test
    public void pickBestAddress_prefersSiteLocalOverOtherOrdinaryAddress() throws Exception {
        assertEquals("10.0.0.5", SyncActivity.pickBestAddress(Arrays.asList(
                candidate("203.0.113.7", false), candidate("10.0.0.5", false))));
    }

    @Test
    public void pickBestAddress_acceptsNonSiteLocalLanWhenNothingBetter() throws Exception {
        assertEquals("100.64.1.2", SyncActivity.pickBestAddress(Arrays.asList(
                candidate("100.64.1.2", false))));
    }

    @Test
    public void pickBestAddress_usesPointToPointAsLastResort() throws Exception {
        assertEquals("10.9.8.7", SyncActivity.pickBestAddress(Arrays.asList(
                candidate("10.9.8.7", true))));
    }

    @Test
    public void pickBestAddress_skipsLoopbackLinkLocalIpv6AndWildcard() throws Exception {
        assertNull(SyncActivity.pickBestAddress(Arrays.asList(
                candidate("127.0.0.1", false), candidate("169.254.3.4", false),
                candidate("fe80::1", false), candidate("2001:db8::1", false),
                candidate("0.0.0.0", false))));
    }

    @Test
    public void pickBestAddress_firstWinsWithinRank() throws Exception {
        assertEquals("192.168.1.2", SyncActivity.pickBestAddress(Arrays.asList(
                candidate("192.168.1.2", false), candidate("192.168.43.1", false))));
    }

    @Test
    public void pickBestAddress_emptyList() {
        assertNull(SyncActivity.pickBestAddress(new ArrayList<>()));
    }
}
