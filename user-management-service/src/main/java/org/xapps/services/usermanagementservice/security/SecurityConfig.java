package org.xapps.services.usermanagementservice.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class SecurityConfig {
    @Value("${security.token.type}")
    private String type;

    @Value("${security.token.key}")
    private String key;

    @Value("${security.token.validity}")
    private Long validity;
}