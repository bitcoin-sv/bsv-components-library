package io.bitcoinsv.bsvcl.bsv;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class HashTest {
    @Test
    public void testEquality() {
        // two Hash objects with the same values must be equal() and their hashCode must be equal()
        byte[] b = {-123, 43, -111, -98, 55, 123, -119, -72, -37, -82, 16, 95, 9, 62, -23, 75, 2, 62, 15, -4, 49, -28, -53, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        Hash hash1 = Hash.hashValue("000000000000000000bc4e13cff0e320b49ee390f501eabd8b98b773e919b258");
        Hash hash2 = Hash.hashValue(b);
        assertArrayEquals(b, hash1.asBinary());
        assertArrayEquals(hash1.asBinary(), hash2.asBinary());
        assertEquals(hash1, hash2);
        assertEquals(hash2, hash1);
        assertNotEquals(0, hash1.hashCode()); // make sure its not using the start of hash, too many with value zero
        assertEquals(hash1.hashCode(), hash2.hashCode());
        assertEquals(hash2.hashCode(), hash1.hashCode());

        String[] hashes = { "1aa9f5e42d25fb7ad27a7da99fb3e3927f123ce82d88c109bb868d036ee03ed6", "3a425e07c957a4389746838d4db5f91efc18b74036a6b30107aebd025bcd4694" };
        for (String s : hashes) {
            Hash h = Hash.hashValue(s);
            assertEquals(h.asString(), s);
            assertNotEquals(hash1, h);
            assertNotEquals(hash1.hashCode(), h.hashCode());
        }
    }
}