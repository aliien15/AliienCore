# 🪐 AliienCore

**AliienCore** is a lightweight, highly optimized utility library designed for Bukkit/Paper Minecraft plugins. It eliminates boilerplate code by providing robust, chainable builders and utilities for modern plugin development.

## ✨ Features
* 📦 **ItemBuilder:** A chainable builder for creating standard and custom items, featuring native support for PersistentDataContainers (PDC), custom model data, and glowing effects.
* 🎨 **ColorUtils:** A bulletproof text formatter that flawlessly bridges modern Adventure `MiniMessage` tags (`<red>`) with legacy ampersand codes (`&c`) and hex codes (`&#ffffff`).
* 🔄 **UpdateChecker:** An asynchronous, generic GitHub Gist update checker to easily notify admins of new plugin releases.
* 🛠️ **Player:** Safe inventory management (drop-on-full).

## 🚀 Usage (Maven)

To use AliienCore in your project, you must compile and install it to your local Maven repository, and then shade it into your plugin to prevent version conflicts.

**1. Add the Dependency**
```xml
<dependency>
    <groupId>com.aliiensmp</groupId>
    <artifactId>AliienCore</artifactId>
    <version>1.0.0</version>
    <scope>compile</scope>
</dependency>
```

**2. Shade and Relocate (Important!)**
Always relocate the library to your own plugin's package to prevent conflicts with other plugins using AliienCore on the same server.

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.5.1</version>
    <executions>
        <execution>
            <phase>package</phase>
            <goals>
                <goal>shade</goal>
            </goals>
            <configuration>
                <relocations>
                    <relocation>
                        <pattern>com.aliiensmp.core</pattern>
                        <shadedPattern>your.plugin.package.core</shadedPattern> </relocation>
                </relocations>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## 🤝 Contributing
Pull requests are welcome! If you have a highly reusable utility class that you think belongs in the core library, feel free to open a PR.