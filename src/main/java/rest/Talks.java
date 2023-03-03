package rest;

import io.quarkiverse.renarde.backoffice.BackofficeController;
import io.quarkus.security.Authenticated;
import model.Talk;

@Authenticated
public class Talks extends BackofficeController<Talk> {

}