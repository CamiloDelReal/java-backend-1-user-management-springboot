package org.xapps.services.usermanagementservice.services;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xapps.services.usermanagementservice.dtos.RoleResponse;
import org.xapps.services.usermanagementservice.entities.Role;
import org.xapps.services.usermanagementservice.repositories.RoleRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoleService {

    private final ModelMapper modelMapper;
    private final RoleRepository roleRepository;

    @Autowired
    public RoleService(ModelMapper modelMapper, RoleRepository roleRepository) {
        this.modelMapper = modelMapper;
        this.roleRepository = roleRepository;
    }

    public List<RoleResponse> getAll() {
        List<Role> roles = roleRepository.findAll();
        return roles.stream().map(r -> modelMapper.map(r, RoleResponse.class)).collect(Collectors.toList());
    }

}
