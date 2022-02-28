package org.xapps.services.usermanagementservice.seeders;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.xapps.services.usermanagementservice.entities.Role;
import org.xapps.services.usermanagementservice.entities.User;
import org.xapps.services.usermanagementservice.repositories.RoleRepository;
import org.xapps.services.usermanagementservice.repositories.UserRepository;

import java.util.List;

@Component
public class DatabaseSeeder {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public DatabaseSeeder(UserRepository userRepository, RoleRepository roleRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener
    public void seed(ContextRefreshedEvent event) {
        if (roleRepository.count() == 0) {
            Role administratorRole = new Role(Role.ADMINISTRATOR);
            roleRepository.save(administratorRole);

            Role guestRole = new Role(Role.GUEST);
            roleRepository.save(guestRole);
        }
        if (userRepository.count() == 0) {
            Role administratorRole = roleRepository.findByName(Role.ADMINISTRATOR).orElse(null);
            User administrator = new User("root@gmail.com", passwordEncoder.encode("123456"), "Root", "Administrator", List.of(administratorRole));
            userRepository.save(administrator);
        }
    }

}
