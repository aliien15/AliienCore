package com.aliiensmp.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.jar.JarFile;

/** Checks the deliverable after Shade runs, not just the compiler classpath. */
class PackagingIT {
    @Test
    void distributableContainsRelocatedLibrariesAndExcludesProvidedDriversAndTestTools()
            throws Exception {
        try (var jar = new JarFile(Path.of("target", "AliienCore-1.0.0.jar").toFile())) {
            for (String entry :
                    new String[] {
                        "com/aliiensmp/core/AliienCore.class",
                        "com/aliiensmp/core/lib/hikari/HikariConfig.class",
                        "com/aliiensmp/core/lib/mariadb/Driver.class",
                        "com/aliiensmp/core/lib/boostedyaml/YamlDocument.class"
                    }) assertNotNull(jar.getEntry(entry), entry);
            for (String entry :
                    new String[] {
                        "com/zaxxer/hikari/HikariConfig.class",
                        "org/mariadb/jdbc/Driver.class",
                        "org/h2/Driver.class",
                        "org/sqlite/JDBC.class",
                        "org/junit/jupiter/api/Test.class",
                        "be/seeseemelk/mockbukkit/MockBukkit.class"
                    }) assertNull(jar.getEntry(entry), entry);
        }
    }

    @Test
    void jdbcServiceDescriptorResolvesToTheRelocatedDriver() throws Exception {
        try (var jar = new JarFile(Path.of("target", "AliienCore-1.0.0.jar").toFile())) {
            var descriptor = jar.getJarEntry("META-INF/services/java.sql.Driver");

            assertNotNull(descriptor);
            String providers;

            try (var input = jar.getInputStream(descriptor)) {
                providers =
                        new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
            assertTrue(providers.contains("com.aliiensmp.core.lib.mariadb.Driver"), providers);
            providers
                    .lines()
                    .map(line -> line.split("#", 2)[0].trim())
                    .filter(line -> !line.isEmpty())
                    .forEach(
                            provider ->
                                    assertNotNull(
                                            jar.getEntry(provider.replace('.', '/') + ".class"),
                                            provider));
        }
    }
}
