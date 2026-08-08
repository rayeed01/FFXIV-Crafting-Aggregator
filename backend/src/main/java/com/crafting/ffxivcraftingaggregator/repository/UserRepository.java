package com.crafting.ffxivcraftingaggregator.repository;

import com.crafting.ffxivcraftingaggregator.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Looks a user up by username, which is the JWT subject and so the lookup every authenticated
     * request performs.
     *
     * <p>Username is unique in the schema, so this can safely return at most one.
     */
    Optional<User> findByUsername(String username);

}
