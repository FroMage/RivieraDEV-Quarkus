package rest;

import io.quarkiverse.renarde.backoffice.BackofficeController;
import io.quarkus.security.Authenticated;
import model.PricePack;

@Authenticated
public class PricePacks extends BackofficeController<PricePack> {

}