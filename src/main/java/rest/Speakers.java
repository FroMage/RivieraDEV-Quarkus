package rest;

import io.quarkiverse.renarde.backoffice.BackofficeController;
import io.quarkus.security.Authenticated;
import model.Speaker;

@Authenticated
public class Speakers extends BackofficeController<Speaker> {

}