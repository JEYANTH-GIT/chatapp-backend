package com.example.chatApp.repository;

import com.example.chatApp.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    List<Group> findByCreatedBy(Long createdBy);

    List<Group> findByNameContainingIgnoreCase(String name);
}
