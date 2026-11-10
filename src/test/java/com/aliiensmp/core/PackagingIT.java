package com.aliiensmp.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.jar.JarFile;

/** Checks the deliverable after Shade runs, not just the compiler classpath. */
class PackagingIT {
    @Test
    void distributableContainsRuntimeLibrariesButExcludesProvidedDriversAndTestTools()
            throws Exception {
        try (var jar = new JarFile(Path.of("target", "AliienCore-1.0.0.jar").toFile())) {
            for (String entry :
                    new String[] {
                        "com/aliiensmp/core/AliienCore.class",
                        "com/aliiensmp/core/lib/hikari/HikariConfig.class",
                        "com/aliiensmp/core/lib/boostedyaml/YamlDocument.class"
                    }) assertNotNull(jar.getEntry(entry), entry);
            for (String prefix :
                    new String[] {
                        "com/zaxxer/hikari/",
                        "dev/dejvokep/boostedyaml/",
                        "org/mariadb/",
                        "com/mysql/",
                        "org/h2/",
                        "org/sqlite/",
                        "com/aliiensmp/core/lib/mariadb/",
                        "com/aliiensmp/core/lib/mysql/",
                        "org/junit/",
                        "be/seeseemelk/mockbukkit/",
                        "com/google/protobuf/"
                    }) {
                assertFalse(
                        jar.stream().anyMatch(entry -> entry.getName().startsWith(prefix)), prefix);
            }
        }
    }

    @Test
    void distributableDoesNotAdvertiseUnbundledJdbcProviders() throws Exception {
        try (var jar = new JarFile(Path.of("target", "AliienCore-1.0.0.jar").toFile())) {
            assertNull(jar.getEntry("META-INF/services/java.sql.Driver"));
        }
    }
}
