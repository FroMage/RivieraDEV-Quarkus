package rest;

import io.quarkiverse.renarde.backoffice.BackofficeController;
import io.quarkus.security.Authenticated;
import model.Sponsor;

@Authenticated
public class Sponsors extends BackofficeController<Sponsor> {

}