package org.xapps.services.usermanagementservice.dtos;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class UserResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private List<RoleResponse> roles;
}
