# SQLite JDBC

Bundled dependency: `org.xerial:sqlite-jdbc:3.53.4.0` (Java 8+ compatible, tested here on Java 17).

Official project and usage: https://github.com/xerial/sqlite-jdbc

Downloaded artifact: https://repo.maven.apache.org/maven2/org/xerial/sqlite-jdbc/3.53.4.0/sqlite-jdbc-3.53.4.0.jar

SHA-256: `bcb1f51e36f940867e83342f9efbf5968ac44a6bef4d397bb4af7b17b45cd2fb`

The matching upstream POM and the upstream `LICENSE` (saved as `SQLITE-JDBC-LICENSE.txt`) are included. The dependency JAR is unmodified. SQLite itself is public domain. No separate database service or SLF4J dependency is required by this game.

MuseumHeist.jar references this file through its manifest Class-Path. Keep the **lib** directory alongside MuseumHeist.jar when distributing the game. Both `java -jar MuseumHeist.jar` and the existing `.bat` launchers use it automatically. Build and tests work offline with this bundled JAR.
