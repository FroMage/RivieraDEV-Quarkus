package util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.github.rjeschke.txtmark.Configuration;
import com.github.rjeschke.txtmark.Processor;

import io.quarkiverse.renarde.util.I18N;
import io.quarkus.arc.Arc;
import io.quarkus.qute.RawString;
import io.quarkus.qute.TemplateExtension;

@TemplateExtension
public class JavaExtensions {

    public static String slugify(String string) {
        return slugify(string, Boolean.TRUE);
    }

    public static String slugify(String string, Boolean lowercase) {
        string = removeAccents(string);
        // Apostrophes.
        string = string.replaceAll("([a-z])'s([^a-z])", "$1s$2");
        string = string.replaceAll("[^\\w]", "-").replaceAll("-{2,}", "-");
        // Get rid of any - at the start and end.
        string = string.replaceAll("-+$", "").replaceAll("^-+", "");

        return (lowercase ? string.toLowerCase() : string);
    }

    public static String format(Date date, String pattern) {
    	I18N i18n = Arc.container().instance(I18N.class).get();
        return format(date, pattern, i18n.get());
    }

    public static String format(Date date, String pattern, String lang) {
        return new SimpleDateFormat(pattern, Locale.forLanguageTag(lang)).format(date);
    }
	
	public static String urlEncode(String str) {
		return URLEncoder.encode(str, StandardCharsets.UTF_8);
	}
	
	public static RawString urlPathEncode(String str){
		return new RawString(urlEncode(str).replace("+", "%20"));
	}
	
    public static RawString jsonEscape(String str) {
        // quick check
        char[] chars = str.toCharArray();
        boolean needsEncoding = false;
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if(Character.isHighSurrogate(c)){
                // skip the next one
                i++;
                continue;
            }
            // every char allowed except " / and control chars 
            // \" \\ \/ \b \f \n \r \t
            if(c == '"'
                    || c == '\\'
                    || c == '\b'
                    || c == '\f'
                    || c == '\n'
                    || c == '\r'
                    || c == '\t'
                    || c <= 0x1f){
                needsEncoding = true;
                break;
            }
        }
        if(!needsEncoding)
            return new RawString(str);
        // now use code points to be safe
        StringBuilder b = new StringBuilder(str.length());
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if(Character.isHighSurrogate(c)){
                // skip the whole pair
                b.append(c);
                b.append(chars[++i]);
                continue;
            }
            if(c == '"')
                b.append("\\\"");
            else if(c == '\\')
                b.append("\\\\");
            else if(c == '\b')
                b.append("\\b");
            else if(c == '\f')
                b.append("\\f");
            else if(c == '\n')
                b.append("\\n");
            else if(c == '\r')
                b.append("\\r");
            else if(c == '\t')
                b.append("\\t");
            else if(c <= 0x1f)
                b.append("\\u").append(String.format("%04x", (int)c));
            else
                b.append(c);
        }
        return new RawString(b.toString());
    }

	public static RawString md(String mdString) {
	    Configuration config = Configuration.builder()
	            .forceExtentedProfile()
	            .setEnablePanicMode(true)
	            .build();
	    String html = Processor.process(mdString, config);
	    html = cleanupEscapingMess(html);

	    return new RawString(html);
	}
	
	private static String cleanupEscapingMess(String html) {
	    return html.replaceAll("&amp;(quot|lt|gt|amp|mdash);", "&$1;");
    }

	public static String removeAccents(String name) {
        return Normalizer.normalize(name, Normalizer.Form.NFKC).replaceAll("[??????????????????]", "a").replaceAll("[??????????]", "c")
                .replaceAll("[??????]", "d").replaceAll("[??????????????????]", "e").replaceAll("[????]", "f").replaceAll("[????????]", "g")
                .replaceAll("[????]", "h").replaceAll("[??????????????????]", "i").replaceAll("[????]", "j").replaceAll("[????]", "k")
                .replaceAll("[??????????]", "l").replaceAll("[????????????]", "n").replaceAll("[????????????????????]", "o").replaceAll("[????]", "p")
                .replaceAll("[??????]", "r").replaceAll("[??????????]", "s").replaceAll("[????????]", "t").replaceAll("[????????????????????]", "u")
                .replaceAll("[??]", "w").replaceAll("[??????]", "y").replaceAll("[??????]", "z").replaceAll("[??]", "ae")
                .replaceAll("[??????????????????]", "A").replaceAll("[??????????]", "C").replaceAll("[??????]", "D").replaceAll("[??????????????????]", "E")
                .replaceAll("[????????]", "G").replaceAll("[????]", "H").replaceAll("[??????????????????]", "I").replaceAll("[??]", "J")
                .replaceAll("[??]", "K").replaceAll("[??????????]", "L").replaceAll("[??????????]", "N").replaceAll("[??????????????????]", "O")
                .replaceAll("[??????]", "R").replaceAll("[??????????]", "S").replaceAll("[????????????????????]", "U").replaceAll("[??]", "W")
                .replaceAll("[??????]", "Y").replaceAll("[??????]", "Z").replaceAll("[??]", "ss");
	}
}
