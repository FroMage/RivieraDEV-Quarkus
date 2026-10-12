package util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;

import org.junit.jupiter.api.Test;

public class DateUtilsTest {

    private static final long DAY = 24 * 60 * 60 * 1000L;

    @Test
    public void daysBetweenDates() {
        Date start = new Date(1_000_000_000_000L);
        assertEquals(0, DateUtils.getDaysBetweenDates(start, start));
        assertEquals(1, DateUtils.getDaysBetweenDates(start, new Date(start.getTime() + DAY)));
        // whole days only
        assertEquals(2, DateUtils.getDaysBetweenDates(start, new Date(start.getTime() + 3 * DAY - 1)));
        assertEquals(30, DateUtils.getDaysBetweenDates(start, new Date(start.getTime() + 30 * DAY)));
    }
}
