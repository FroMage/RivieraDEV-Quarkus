package util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.junit.jupiter.api.Test;

public class JavaExtensionsTest {

    @Test
    public void slugify() {
        assertEquals("hello-world", JavaExtensions.slugify("Hello, World!"));
        assertEquals("cafe-creme", JavaExtensions.slugify("Café  Crème"));
        // leading and trailing separators are dropped
        assertEquals("a-b", JavaExtensions.slugify("  --a & b--  "));
        // apostrophes are dropped rather than turned into separators
        assertEquals("peters-talk", JavaExtensions.slugify("Peter's talk"));
        assertEquals("Hello-World", JavaExtensions.slugify("Hello World", false));
    }

    @Test
    public void removeAccents() {
        assertEquals("eauc", JavaExtensions.removeAccents("éàûç"));
        assertEquals("aeOss", JavaExtensions.removeAccents("æÖß"));
        assertEquals("Francois Epardaud", JavaExtensions.removeAccents("François Épardaud"));
        assertEquals("ss", JavaExtensions.removeAccents("ß"));
        assertEquals("plain ascii", JavaExtensions.removeAccents("plain ascii"));
    }

    @Test
    public void nobreak() {
        assertEquals("a&nbsp;b&nbsp;c", JavaExtensions.nobreak("a b c").toString());
    }

    @Test
    public void urlEncoding() {
        assertEquals("a+b%26c", JavaExtensions.urlEncode("a b&c"));
        // in a path a space is %20, a + would be a plus
        assertEquals("a%20b", JavaExtensions.urlPathEncode("a b").toString());
    }

    @Test
    public void jsonEscape() {
        assertEquals("plain", JavaExtensions.jsonEscape("plain").toString());
        assertEquals("say \\\"hi\\\"\\n\\ttab \\\\ slash", JavaExtensions.jsonEscape("say \"hi\"\n\ttab \\ slash").toString());
        assertEquals("\\u0001", JavaExtensions.jsonEscape("\u0001").toString());
        // surrogate pairs (emoji) are kept as they are
        assertEquals("🎙️ \\\"x\\\"", JavaExtensions.jsonEscape("🎙️ \"x\"").toString());
    }

    @Test
    public void formatDateInTheGivenLanguage() {
        Calendar calendar = new GregorianCalendar(2026, Calendar.JULY, 6);
        assertEquals("July 6", JavaExtensions.format(calendar.getTime(), "MMMM d", "en"));
        assertEquals("juillet 6", JavaExtensions.format(calendar.getTime(), "MMMM d", "fr"));
    }

    @Test
    public void markdown() {
        String html = JavaExtensions.md("Some **bold** and a [link](https://rivieradev.fr)").toString();
        assertTrue(html.contains("<strong>bold</strong>"), html);
        assertTrue(html.contains("<a href=\"https://rivieradev.fr\">link</a>"), html);
        // entities should not be double escaped
        String escaped = JavaExtensions.md("a &amp; b").toString();
        assertTrue(escaped.contains("a &amp; b"), escaped);
        assertTrue(!escaped.contains("&amp;amp;"), escaped);
    }
}
