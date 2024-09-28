package dev.ebullient.convert;

import org.junit.jupiter.api.BeforeAll;

import io.quarkus.test.junit.main.QuarkusMainIntegrationTest;

@QuarkusMainIntegrationTest
public class Pf2eDataConvertIT extends Pf2eDataConvertTest {
    @BeforeAll
    public static void setupDir() {
        setupDir("test-cli-IT");
    }
}
