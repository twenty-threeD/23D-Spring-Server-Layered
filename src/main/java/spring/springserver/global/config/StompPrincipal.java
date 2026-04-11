package spring.springserver.global.config;

import lombok.Getter;

import java.security.Principal;

@Getter
public class StompPrincipal implements Principal {

    private String username;
    private String role;

    public StompPrincipal(String username, String role) {

        this.username = username;
        this.role = role;
    }

    @Override
    public String getName() {

        return username;
    }
}
