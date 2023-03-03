package rest;

import io.quarkiverse.renarde.backoffice.BackofficeController;
import io.quarkus.security.Authenticated;
import model.Slot;

@Authenticated
public class Slots extends BackofficeController<Slot> {

}