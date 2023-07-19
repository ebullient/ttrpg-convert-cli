package dev.ebullient.convert;

import org.junit.jupiter.api.BeforeAll;

import io.quarkus.test.junit.main.QuarkusMainIntegrationTest;

@QuarkusMainIntegrationTest
public class Tools5eDataConvertIT extends Tools5eDataConvertTest {
    @BeforeAll
    public static void setupDir() {
        setupDir("RpgDataConvertIT");
    }
}
