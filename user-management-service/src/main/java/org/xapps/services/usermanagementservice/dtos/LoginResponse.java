package org.xapps.services.usermanagementservice.dtos;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class LoginResponse {
    private String token;
    private String tokenType;
    private Long validity;
}
