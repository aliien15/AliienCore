package com.aliiensmp.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.sql.Driver;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/** Checks the deliverable after Shade runs, not just the compiler classpath. */
class PackagingIT {
    @Test
    void distributableContainsAllFourDriversAndExcludesTestTools() throws Exception {
        try (var jar = new JarFile(Path.of("target", "AliienCore-1.0.0.jar").toFile())) {
            for (String entry :
                    new String[] {
                        "com/aliiensmp/core/AliienCore.class",
                        "com/aliiensmp/core/lib/hikari/HikariConfig.class",
                        "com/aliiensmp/core/lib/mariadb/Driver.class",
                        "com/aliiensmp/core/lib/mysql/cj/jdbc/Driver.class",
                        "org/h2/Driver.class",
                        "org/sqlite/JDBC.class",
                        "com/aliiensmp/core/lib/boostedyaml/YamlDocument.class"
                    }) assertNotNull(jar.getEntry(entry), entry);
            for (String entry :
                    new String[] {
                        "com/zaxxer/hikari/HikariConfig.class",
                        "org/mariadb/jdbc/Driver.class",
                        "com/mysql/cj/jdbc/Driver.class",
                        "org/junit/jupiter/api/Test.class",
                        "be/seeseemelk/mockbukkit/MockBukkit.class"
                    }) assertNull(jar.getEntry(entry), entry);
        }
    }

    @Test
    void jdbcServiceDescriptorResolvesToAllFourBundledDrivers() throws Exception {
        try (var jar = new JarFile(Path.of("target", "AliienCore-1.0.0.jar").toFile())) {
            var descriptor = jar.getJarEntry("META-INF/services/java.sql.Driver");

            assertNotNull(descriptor);
            String providers;

            try (var input = jar.getInputStream(descriptor)) {
                providers =
                        new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
            assertTrue(providers.contains("com.aliiensmp.core.lib.mariadb.Driver"), providers);
            assertTrue(
                    providers.contains("com.aliiensmp.core.lib.mysql.cj.jdbc.Driver"), providers);
            assertTrue(providers.contains("org.h2.Driver"), providers);
            assertTrue(providers.contains("org.sqlite.JDBC"), providers);
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

    @Test
    void packagedDriversLoadAndEmbeddedDatabasesWorkWithoutDependencyJars() throws Exception {
        // Only the deliverable is visible, not Maven's dependency JARs. This also exercises
        // SQLite's bundled native-library loading.
        URL artifact = Path.of("target", "AliienCore-1.0.0.jar").toUri().toURL();
        try (var loader =
                new URLClassLoader(new URL[] {artifact}, ClassLoader.getPlatformClassLoader())) {
            var drivers =
                    ServiceLoader.load(Driver.class, loader).stream()
                            .map(ServiceLoader.Provider::get)
                            .collect(
                                    Collectors.toMap(
                                            driver -> driver.getClass().getName(),
                                            driver -> driver));
            assertEquals(
                    Set.of(
                            "com.aliiensmp.core.lib.mariadb.Driver",
                            "com.aliiensmp.core.lib.mysql.cj.jdbc.Driver",
                            "org.h2.Driver",
                            "org.sqlite.JDBC"),
                    drivers.keySet());
            for (String backend : new String[] {"h2", "sqlite"}) {
                Driver driver =
                        drivers.get(backend.equals("h2") ? "org.h2.Driver" : "org.sqlite.JDBC");
                assertSame(loader, driver.getClass().getClassLoader());
                String url =
                        backend.equals("h2") ? "jdbc:h2:mem:packaging" : "jdbc:sqlite::memory:";
                try (var connection = driver.connect(url, new Properties());
                        var statement = connection.createStatement();
                        var result = statement.executeQuery("SELECT 1")) {
                    assertTrue(result.next());
                    assertEquals(1, result.getInt(1));
                }
            }
        }
    }
}
