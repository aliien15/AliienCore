package com.aliiensmp.core.menu;

import static org.junit.jupiter.api.Assertions.*;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

class PaginationTest {
    @Property
    void pagesCoverEveryItemWithNoExtraPage(
            @ForAll @IntRange(min = 1, max = 54) int slots,
            @ForAll @IntRange(min = 1, max = 1000000) int items) {
        int pages = new AliienGUI("Menu").getTotalPages(slots, items);

        assertTrue((long) pages * slots >= items);
        assertTrue((long) (pages - 1) * slots < items);
    }

    @Property
    void requestedPageIsAlwaysClamped(
            @ForAll int requested, @ForAll @IntRange(min = 1, max = 1000000) int total) {
        int actual = new AliienGUI("Menu").sanitizePage(requested, total);

        assertTrue(actual >= 1 && actual <= total);
        if (requested >= 1 && requested <= total) assertEquals(requested, actual);
    }
}
