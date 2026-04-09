package com.example.chatApp.repository;

<<<<<<< HEAD

=======
import com.example.chatApp.model.User;
>>>>>>> 336049e9327ef3bc762643b5dee206ef27479048
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.chatApp.model.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}
