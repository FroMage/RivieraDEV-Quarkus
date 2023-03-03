package rest;

import io.quarkiverse.renarde.backoffice.BackofficeController;
import io.quarkus.security.Authenticated;
import model.Configuration;

@Authenticated
public class Configurations extends BackofficeController<Configuration> {

}