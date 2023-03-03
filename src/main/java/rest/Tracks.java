package rest;

import io.quarkiverse.renarde.backoffice.BackofficeController;
import io.quarkus.security.Authenticated;
import model.Track;

@Authenticated
public class Tracks extends BackofficeController<Track> {

}