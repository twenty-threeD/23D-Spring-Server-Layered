package spring.springserver.global.config.websocket;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.security.Principal;

@Getter
@AllArgsConstructor
public class StompPrincipal implements Principal {

    private String username;
    private String role;


    @Override
    public String getName() {
        return username;
    }
}