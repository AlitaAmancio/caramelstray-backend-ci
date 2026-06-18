package br.com.AllTallent.caramelstray;

import br.com.AllTallent.CaramelStrayApplication;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CaramelStrayApplicationTests {

    @Test
    void contextLoads() {
        assertNotNull(CaramelStrayApplication.class,
            "Application main class must be present on the classpath");
    }
}
