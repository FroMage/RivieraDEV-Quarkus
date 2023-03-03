package rest;

import io.quarkiverse.renarde.backoffice.BackofficeController;
import io.quarkus.security.Authenticated;
import model.User;

@Authenticated
public class Users extends BackofficeController<User> {

}