package ar.edu.um.backend;

import ar.edu.um.backend.config.AsyncSyncConfiguration;
import ar.edu.um.backend.config.EmbeddedRedis;
import ar.edu.um.backend.config.EmbeddedSQL;
import ar.edu.um.backend.config.JacksonConfiguration;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Base composite annotation for integration tests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(classes = { BackendCatedraApp.class, JacksonConfiguration.class, AsyncSyncConfiguration.class })
@EmbeddedRedis
@EmbeddedSQL
public @interface IntegrationTest {
}
