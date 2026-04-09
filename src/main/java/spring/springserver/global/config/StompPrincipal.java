package spring.springserver.global.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.security.Principal;

@Getter
@RequiredArgsConstructor
public class StompPrincipal implements Principal {

    private String username;
    private String role;


    @Override
    public String getName() {
        return username;
    }
}