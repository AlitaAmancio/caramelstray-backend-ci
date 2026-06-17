package br.com.AllTallent.caramelstray.system;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CaramelStrayApplicationSystemTest extends BaseSystemTest {

    @Test
    void contextLoads() {
        // Passes when BaseSystemTest's @BeforeAll successfully loads the full Spring context
        // with a live PostgreSQL connection and seeds the base data.
    }
}
