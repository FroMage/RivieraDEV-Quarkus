package rest;

import io.quarkiverse.renarde.backoffice.BackofficeController;
import io.quarkus.security.Authenticated;
import model.PricePackDate;

@Authenticated
public class PricePackDates extends BackofficeController<PricePackDate> {

}