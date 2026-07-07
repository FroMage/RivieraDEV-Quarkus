package email;

import io.quarkus.mailer.MailTemplate.MailTemplateInstance;
import io.quarkus.qute.CheckedTemplate;
import model.Talk;

public class Emails {

    private static final String FROM = "Riviera DEV <info@rivieradev.fr>";

    @CheckedTemplate
    static class Templates {
        public static native MailTemplateInstance slidesRequest(Talk talk);
    }

    public static void slidesRequest(Talk talk, String... toEmails) {
        Templates.slidesRequest(talk)
            .subject("[Riviera DEV] Please share your slides")
            .to(toEmails)
            .from(FROM)
            .send().await().indefinitely();
    }
}
