# 🪐 AliienCore

**AliienCore** is a lightweight, highly optimized utility library designed for Bukkit/Paper Minecraft plugins (Java 21). It eliminates boilerplate code by providing robust, chainable builders and utilities for modern, Folia-compatible plugin development.

## 📚 Documentation
For complete documentation of all my plugins (that use this library), visit the [Aliien Docs](https://aliien.gitbook.io/aliien-docs/).

## ✨ Features

* **Strict Async & Folia Compatibility:** Built with absolute Folia compatibility in mind. Global and entity tasks are handled natively via schedulers without `BukkitRunnable`, and all File I/O and database operations are strictly non-blocking.
* **GUIs (Menu System):** Create robust menus using `AliienGUI`. Features an internal, raw-slot-based master lock that handles all click-routing, shift-click blocking, dupe-protection, and zero need for manual `InventoryClickEvent` listeners.
* **ItemBuilder:** A chainable builder for creating standard and custom items, featuring native support for PersistentDataContainers (PDC), custom model data, and glowing effects. Seamlessly integrates with GUIs via `.buildClickable()`.
* **Database Manager:** Centralized database handling using HikariCP for connection pooling. Supports both SQLite (with forced safe file-locking) and MySQL. All reads/writes are completely asynchronous and protected with `PreparedStatement` placeholders.
* **ColorUtils:** A bulletproof text formatter that flawlessly bridges modern Adventure `MiniMessage` tags (`<red>`) with legacy ampersand codes (`&c`) and hex codes (`&#ffffff`). It also globally injects `<!italic>` to override default vanilla text behavior.
* **MessageUtils:** Cleanly send messages with native support for vararg placeholder replacements and MiniMessage parsing.
* **SoundUtils:** Parse config strings (e.g., `SOUND_KEY:VOLUME:PITCH`) into `CustomSound` records. Natively supports both Bukkit Vanilla Enums and Custom Resource Pack strings, handling all null-safety and playback routing.
* **Configs:** Generate and manage advanced configuration files easily with native `BoostedYAML` integration.
* **bStats Integration:** Built-in metrics handling with the `org.bstats` package relocated to prevent conflicts, making it easy to attach custom `SimplePie` charts.
* **UpdateChecker:** An asynchronous, generic GitHub Gist update checker to easily notify admins of new plugin releases.
* **Player Utilities:** Safe inventory management (drop-on-full).

## 🤝 Contributing
Pull requests are welcome! If you have a highly reusable utility class that you think belongs in the core library, feel free to open a PR.