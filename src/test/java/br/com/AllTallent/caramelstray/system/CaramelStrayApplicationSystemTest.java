package br.com.AllTallent.caramelstray.system;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CaramelStrayApplicationSystemTest extends BaseSystemTest {

    @Test
    void contextLoads() {
        assertNotNull(jdbcTemplate,
            "Spring context must wire JdbcTemplate confirming DB connectivity");
    }
}
