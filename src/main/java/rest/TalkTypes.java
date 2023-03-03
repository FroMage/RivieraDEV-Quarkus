package rest;

import io.quarkiverse.renarde.backoffice.BackofficeController;
import io.quarkus.security.Authenticated;
import model.TalkType;

@Authenticated
public class TalkTypes extends BackofficeController<TalkType> {

}