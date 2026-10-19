package io.github.elr.blog;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class PostgresImageConsistencyTest {

    private static final Pattern POSTGRES_IMAGE = Pattern.compile("image:\\s*(postgres:\\S+)");

    @Test
    void testcontainers_uses_the_same_postgres_image_as_compose() throws IOException {
        String compose = Files.readString(Path.of("compose.yaml"));
        Matcher matcher = POSTGRES_IMAGE.matcher(compose);

        assertThat(matcher.find())
                .as("compose.yaml should declare a postgres image")
                .isTrue();
        assertThat(TestcontainersConfiguration.POSTGRES_IMAGE)
                .as("Update TestcontainersConfiguration.POSTGRES_IMAGE to match compose.yaml")
                .isEqualTo(matcher.group(1));
    }
}
