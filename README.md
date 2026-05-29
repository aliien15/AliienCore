# 🪐 AliienCore

**AliienCore** is a lightweight, highly optimized utility library designed for Bukkit/Paper Minecraft plugins (Java 21). It eliminates boilerplate code by providing robust, chainable builders and utilities for modern, Folia-compatible plugin development.

## 📚 Documentation
For complete documentation of all my plugins (that use this library), visit the [Aliien Docs](https://aliien.gitbook.io/aliien-docs/).

---

## 🚀 Getting Started

To use AliienCore in your premium plugins, you need to compile it locally and shade it into your project.

### 1. Installation
Clone the repository and install it to your local Maven repository:
```bash
git clone [https://github.com/aliien15/AliienCore](https://github.com/aliien15/AliienCore)
cd AliienCore # (or whatever directory you want to have it in)
mvn clean install
```

Then, add it as a dependency in your plugin's `pom.xml` and shade it:
```xml
<dependency>
    <groupId>com.aliiensmp</groupId>
    <artifactId>AliienCore</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. Initialization
All plugins utilizing this library **must** initialize the core in their `onEnable()` method before using any features:

```java
@Override
public void onEnable() {
    // Initialize the core library
    AliienCore.init(this);
}
```

---

## ✨ Features & Modules

* **Strict Async & Folia Compatibility:** Built with absolute Folia compatibility in mind. Global and entity tasks are handled natively via schedulers without `BukkitRunnable`, and all File I/O and database operations are strictly non-blocking.
* **GUIs (Menu System):** Create robust menus using `AliienGUI`. Features an internal, raw-slot-based master lock that handles all click-routing, shift-click blocking, dupe-protection, and zero need for manual `InventoryClickEvent` listeners.
* **ItemBuilder:** A chainable builder for creating standard and custom items, featuring native support for PersistentDataContainers (PDC), custom model data, and glowing effects. Seamlessly integrates with GUIs via `.buildClickable()`.
* **Database Manager:** Centralized database handling using HikariCP for connection pooling. Supports SQLite (with forced safe file-locking), MySQL, and **H2** (in-memory or file-based). All reads/writes are completely asynchronous and protected with `PreparedStatement` placeholders.
* **Duration & Time Utilities:** Parse strings like `1d2h30m` cleanly into Java `Duration` objects via regex. Format durations back into highly customizable formats (`SHORT`, `LONG`, `CLOCK`), and safely convert durations or strings directly into server ticks for Folia schedulers.
* **ColorUtils:** A bulletproof text formatter that flawlessly bridges modern Adventure `MiniMessage` tags (`<red>`) with legacy ampersand codes (`&c`) and hex codes (`&#ffffff`). It globally injects `<!italic>` to override default vanilla text behavior.
* **MessageUtils:** Cleanly send messages with native support for vararg placeholder replacements and MiniMessage parsing.
* **SoundUtils:** Parse config strings (e.g., `SOUND_KEY:VOLUME:PITCH`) into `CustomSound` records. Natively supports both Bukkit Vanilla Enums and Custom Resource Pack strings, handling all null-safety and playback routing.
* **Configs:** Generate and manage advanced configuration files easily with native `BoostedYAML` integration. Bind static variables directly to config paths using the `@Key` annotation to instantly map raw values.
* **UpdateChecker:** An asynchronous, generic GitHub Gist update checker to easily notify admins of new plugin releases.
* **Player Utilities:** Safe inventory management (drop-on-full).

---

## 💻 Code Examples

### Configuration Binding
Easily map config values directly to public static variables to avoid passing instances around.

```java
import com.aliiensmp.core.config.ConfigManager;
import dev.dejvokep.boostedyaml.YamlDocument;

public class Settings {
    
    @Key("messages.prefix")
    public static String PREFIX;

    @Key("settings.default-sound")
    public static String DEFAULT_SOUND;

    // I recommend putting this in the main class and make it not receive any args, but anywhere works
    public static void load(Plugin plugin) {
        YamlDocument config = ConfigManager.loadConfig(plugin, "config.yml");
        ConfigManager.bindConfig(config, new Settings());
    }
}
```

### Database Operations (Async)
Never write raw JDBC connection logic. Use the centralized manager for completely asynchronous, non-blocking queries.

```java
// Initialization (e.g., in onEnable)
AliienCore.getDatabase().connectH2(this, "database.db"); // Or connectSQLite, connectMySQL

// Asynchronous Write (INSERT/UPDATE/DELETE)
String insertQuery = "INSERT INTO players (uuid, coins) VALUES (?, ?)";
AliienCore.getDatabase().executeAsync(insertQuery, player.getUniqueId().toString(), 100);

// Asynchronous Read (SELECT)
String selectQuery = "SELECT coins FROM players WHERE uuid = ?";
AliienCore.getDatabase().queryAsync(selectQuery, rs -> {
    if (rs.next()) {
        return rs.getInt("coins");
    }
    return 0; // Default if not found
}, player.getUniqueId().toString()).thenAccept(coins -> {
    MessageUtils.send(player, null, "You have " + coins + " coins!");
});
```

### Duration & Time Utilities
Parse complex string formats down to Java `Duration` objects, format remaining times cleanly, or calculate exact server ticks for Folia schedulers.

```java
import com.aliiensmp.core.utils.DurationUtils;
import java.time.Duration;

// Parse input strings directly into standard Java Durations
Duration tempMuteTime = DurationUtils.parse("2d12h30m");

// Format durations into customizable human-readable text layouts
String shortFormat = DurationUtils.format(tempMuteTime, DurationUtils.Style.SHORT); // Output: "2d 12h 30m"
String longFormat  = DurationUtils.format(tempMuteTime, DurationUtils.Style.LONG);  // Output: "2 Days 12 Hours 30 Minutes"

// Format for actionbars, bossbars, or scoreboards
Duration timer = Duration.ofMinutes(4).plusSeconds(12);
String clockFormat = DurationUtils.format(timer, DurationUtils.Style.CLOCK);        // Output: "04:12"

// Safely convert durations or parse strings straight to server ticks (20 ticks = 1 second)
long entityDelayTicks = DurationUtils.toTicks(Duration.ofMinutes(5));
long schedulerTicks   = DurationUtils.toTicks("1h30m");
```

### Creating Interactive GUIs
Zero Bukkit boilerplate. No manual listeners needed.

```java
public void openMenu(Player player) {
    AliienGUI menu = new AliienGUI("My Custom Menu", 3); // Title and rows

    ItemBuilder sword = new ItemBuilder(Material.DIAMOND_SWORD)
            .setName("<red>Epic Sword")
            .setLore("&7Click to receive this sword!")
            .addFlags(ItemFlag.HIDE_ATTRIBUTES);

    // Build as a ClickableItem to automatically handle InventoryClickEvents
    menu.setItem(13, sword.buildClickable(event -> {
        player.getInventory().addItem(new ItemStack(Material.DIAMOND_SWORD));
        player.closeInventory();
        
        // Using SoundUtils and MessageUtils
        CustomSound sound = SoundUtils.parse("ENTITY_EXPERIENCE_ORB_PICKUP:1.0:1.0");
        sound.play(player);
        
        MessageUtils.send(player, Settings.PREFIX, "<green>You claimed your sword!");
    }));

    menu.open(player);
}
```

---

## 🤝 Contributing
Pull requests are welcome! If you have a highly reusable utility class that you think belongs in the core library, feel free to open a PR. Ensure that any contributions adhere to the core philosophy: strict async compliance, Folia compatibility, and zero Bukkit boilerplate.
```