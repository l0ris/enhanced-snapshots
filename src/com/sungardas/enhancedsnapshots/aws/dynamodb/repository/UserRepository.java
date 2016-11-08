package com.sungardas.enhancedsnapshots.aws.dynamodb.repository;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.User;
import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

@EnableScan
public interface UserRepository extends CrudRepository<User, String> {
    List<User> findByRole(String role);

    List<User> findByEmailAndPassword(String email, String password);

    List<User> findByEmail(String email);

    default void delete(List<User> entities) {
        entities.forEach(this::delete);
    }
}