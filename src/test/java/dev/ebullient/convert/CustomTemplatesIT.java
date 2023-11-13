package dev.ebullient.convert;

import org.junit.jupiter.api.BeforeAll;

import io.quarkus.test.junit.main.QuarkusMainIntegrationTest;

@QuarkusMainIntegrationTest
public class CustomTemplatesIT extends CustomTemplatesTest {
    @BeforeAll
    public static void setupDir() {
        setupDir("Tools5eDataConvertIT");
    }
}
