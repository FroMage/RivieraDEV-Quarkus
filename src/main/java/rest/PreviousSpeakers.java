package rest;

import io.quarkiverse.renarde.backoffice.BackofficeController;
import io.quarkus.security.Authenticated;
import model.PreviousSpeaker;

@Authenticated
public class PreviousSpeakers extends BackofficeController<PreviousSpeaker> {

}