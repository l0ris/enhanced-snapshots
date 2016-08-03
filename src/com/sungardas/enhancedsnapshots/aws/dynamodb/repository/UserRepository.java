package com.sungardas.enhancedsnapshots.aws.dynamodb.repository;

import java.util.List;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.User;

import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.repository.CrudRepository;

@EnableScan
public interface UserRepository extends CrudRepository<User, String> {
    List<User> findByRole(String role);

    List<User> findByEmailAndPassword(String email, String password);

    List<User> findByEmail(String email);

}