package rest;

import io.quarkiverse.renarde.backoffice.BackofficeController;
import io.quarkus.security.Authenticated;
import model.Organiser;

@Authenticated
public class Organisers extends BackofficeController<Organiser> {

}