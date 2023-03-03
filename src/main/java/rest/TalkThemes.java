package rest;

import io.quarkiverse.renarde.backoffice.BackofficeController;
import io.quarkus.security.Authenticated;
import model.TalkTheme;

@Authenticated
public class TalkThemes extends BackofficeController<TalkTheme> {

}