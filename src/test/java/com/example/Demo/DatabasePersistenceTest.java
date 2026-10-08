package com.example.Demo;

import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DatabasePersistenceTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testDataSourceConnection() throws Exception {
        Assertions.assertNotNull(dataSource, "DataSource should not be null");
        try (var connection = dataSource.getConnection()) {
            Assertions.assertNotNull(connection, "SQL Connection should not be null");
            Assertions.assertTrue(connection.isValid(2), "Connection should be valid");
            var metaData = connection.getMetaData();
            Assertions.assertEquals("MySQL", metaData.getDatabaseProductName());
        }
    }

    @Test
    void testHibernateInsertAndReadPersistence() {
        String testUsername = "db_test_" + System.currentTimeMillis();
        String testEmail = testUsername + "@example.com";
        String testPassword = "secureTestPassword123";

        // 1. Insert via Spring Data JPA / Hibernate
        Users newUser = new Users(testUsername, testEmail, testPassword);
        Users savedUser = userRepository.save(newUser);

        Assertions.assertNotNull(savedUser, "Saved user should not be null");
        Assertions.assertTrue(savedUser.getXyz() > 0, "Generated ID should be positive");

        // 2. Read back via ID
        Optional<Users> retrievedById = userRepository.findById(savedUser.getXyz());
        Assertions.assertTrue(retrievedById.isPresent(), "User should be readable by ID");
        Assertions.assertEquals(testUsername, retrievedById.get().getUsername());
        Assertions.assertEquals(testEmail, retrievedById.get().getEmail());

        // 3. Read back via findByUsername
        Users retrievedByUsername = userRepository.findByUsername(testUsername);
        Assertions.assertNotNull(retrievedByUsername, "User should be readable by username");
        Assertions.assertEquals(savedUser.getXyz(), retrievedByUsername.getXyz());

        // 4. Cleanup test record
        userRepository.delete(savedUser);
        Optional<Users> deletedUser = userRepository.findById(savedUser.getXyz());
        Assertions.assertFalse(deletedUser.isPresent(), "User should be deleted after cleanup");
    }
}
