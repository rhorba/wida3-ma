package com.wida3.auth.repository;

import com.wida3.auth.entity.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Short> {

    Optional<Role> findByName(String name);
}
