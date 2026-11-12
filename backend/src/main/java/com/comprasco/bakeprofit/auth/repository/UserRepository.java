package com.comprasco.bakeprofit.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.comprasco.bakeprofit.auth.entity.Role;
import com.comprasco.bakeprofit.auth.entity.User;

import java.util.List;
import java.util.Optional;



public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndActiveTrue(String email);

    List<User> findByActiveTrue();

    List<User> findByActiveFalse();

    List<User> findByRole(Role role);

}
