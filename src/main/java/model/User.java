package model;

import java.util.Collections;
import java.util.Set;

import io.quarkiverse.renarde.security.RenardeUser;
import io.quarkiverse.renarde.security.RenardeUserWithPassword;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.NotBlank;

@Entity(name = "user_table")
public class User extends PanacheEntity implements RenardeUserWithPassword {

	@NotBlank
	public String firstName, lastName, userName;
	// This is hashed with bCrypt
	@NotBlank
//	@Password
	public String password;
	
	public Boolean isBCrypt;

	@PrePersist
	@PreUpdate
	public void _save() {
	    // allows us to create users with plain text passwords in the admin CRUD pages
	    if(isBCrypt == null || !isBCrypt) {
	        password = BcryptUtil.bcryptHash(password);
	        isBCrypt = true;
	    } else {
	        if(!password.startsWith("$2a$"))
	            throw new IllegalStateException("Password is not hashed with BCrypt");
	    }
	}
	
	@Override
	public String toString() {
		return firstName+" "+lastName+" ("+userName+")";
	}

    public boolean checkPassword(String sentPassword) {
        if(isBCrypt != null && isBCrypt)
            return BcryptUtil.matches(sentPassword, password);
        return password.equals(sentPassword);
    }

	@Override
	public Set<String> roles() {
		return Collections.emptySet();
	}

	@Override
	public String userId() {
		return userName;
	}

	@Override
	public boolean registered() {
		return true;
	}

	@Override
	public String password() {
		return password;
	}

	public static User findByUsername(String userName) {
		return find("userName", userName).firstResult();
	}
}
