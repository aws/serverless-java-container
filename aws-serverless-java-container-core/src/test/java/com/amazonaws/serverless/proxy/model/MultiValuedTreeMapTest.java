package com.amazonaws.serverless.proxy.model;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class MultiValuedTreeMapTest {

    @Test
    void add_sameNameCaseSensitive_expectBothValues() {
        MultiValuedTreeMap<String, String> map = new MultiValuedTreeMap<>();
        map.add("Test", "test");
        map.add("Test", "test2");

        assertNotNull(map.get("Test"));
        assertEquals(2, map.get("Test").size());
        assertEquals("test", map.getFirst("Test"));
        assertEquals("test2", map.get("Test").get(1));
        assertNull(map.get("test"));

        map.add("test", "test");
        assertNotNull(map.get("test"));
        assertEquals(1, map.get("test").size());
    }

    @Test
    void add_sameNameCaseInsensitive_expectOneValue() {
        Headers map = new Headers();
        map.add("Test", "test");
        assertNotNull(map.get("Test"));
        assertNotNull(map.get("test"));
        assertEquals(1, map.get("Test").size());

        map.add("test", "test2");
        assertNotNull(map.get("Test"));
        assertEquals(2, map.get("Test").size());
    }

    @Test
    void addFirst_sameNameKey_ExpectFirstReplaced() {
        MultiValuedTreeMap<String, String> map = new MultiValuedTreeMap<>();
        map.add("Test", "test1");
        map.add("Test", "test2");

        assertNotNull(map.get("Test"));
        assertEquals(2, map.get("Test").size());
        assertEquals("test1", map.getFirst("Test"));

        map.addFirst("Test", "test3");
        assertEquals(3, map.get("Test").size());
        assertEquals("test3", map.getFirst("Test"));
    }
}
