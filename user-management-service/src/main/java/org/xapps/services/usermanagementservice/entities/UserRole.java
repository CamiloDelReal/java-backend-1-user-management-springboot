package org.xapps.services.usermanagementservice.entities;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
@Entity
@Table(name = "users_roles")
public class UserRole {
    @EmbeddedId
    private UserRoleId id;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    @EqualsAndHashCode
    @Embeddable
    public static class UserRoleId implements Serializable {
        static final long serialVersionUID = 3472478579236454587L;

        @Column(name = "user_id")
        private Long userId;

        @Column(name = "role_id")
        private Long roleId;
    }
}
