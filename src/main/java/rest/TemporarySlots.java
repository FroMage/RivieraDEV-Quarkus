package rest;

import io.quarkiverse.renarde.backoffice.BackofficeController;
import io.quarkus.security.Authenticated;
import model.TemporarySlot;

@Authenticated
public class TemporarySlots extends BackofficeController<TemporarySlot> {

}