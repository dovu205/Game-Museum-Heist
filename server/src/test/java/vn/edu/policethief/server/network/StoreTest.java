package vn.edu.policethief.server.network;

import vn.edu.policethief.server.Store;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.sql.*;
import java.util.*;

public final class StoreTest {
    static int checks;

    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        checks++;
    }

    public static Connection connect(Path directory) throws SQLException {
        Connection c =
                DriverManager.getConnection(
                        "jdbc:sqlite:" + directory.toAbsolutePath().resolve("museumheist.db"));
        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys=ON");
        }
        return c;
    }

    public static void sql(Path directory, String sql) throws SQLException {
        try (Connection c = connect(directory);
                Statement s = c.createStatement()) {
            s.execute(sql);
        }
    }

    static int scalar(Path directory, String sql) throws SQLException {
        try (Connection c = connect(directory);
                Statement s = c.createStatement();
                ResultSet r = s.executeQuery(sql)) {
            r.next();
            return r.getInt(1);
        }
    }

    static void expectFailure(Store store, Store.Match match) throws Exception {
        try {
            store.finish(match);
            throw new AssertionError("Write should fail");
        } catch (IOException expected) {
            checks++;
        }
    }

    static void unchanged(Store store, Store.User live, Path dir, String id) throws Exception {
        check(
                live.points == 0 && live.played == 0 && live.wins == 0,
                "live identity not changed before successful commit");
        check(
                store.login("alice", "secret123").points == 0,
                "same store has no uncommitted points");
        check(
                new Store(dir).login("alice", "secret123").points == 0,
                "reopened store has no uncommitted points");
        check(
                scalar(dir, "SELECT count(*) FROM matches WHERE id='" + id + "'") == 0,
                "failed match not recorded");
        check(
                scalar(dir, "SELECT count(*) FROM match_players") == 0,
                "failed participants rolled back");
    }

    static void migration(Path root) throws Exception {
        Path dir = root.resolve("legacy");
        Files.createDirectories(dir);
        byte[] original;
        try (InputStream fixture =
                StoreTest.class.getResourceAsStream("/fixtures/nightchase-v1.dat")) {
            if (fixture == null) throw new IOException("Missing legacy test fixture");
            original = fixture.readAllBytes();
        }
        Files.write(dir.resolve("nightchase.dat"), original);
        Store store = new Store(dir);
        check(store.ranking().size() == 3, "all legacy users imported");
        Store.User alice = store.login("legacy_alice", "old-alice-pass"),
                bob = store.login("legacy_bob", "old-bob-pass");
        check(
                alice != null && bob != null && store.login("legacy_idle", "old-idle-pass") != null,
                "old passwords work unchanged");
        check(
                store.login("legacy_alice", "wrong-password") == null,
                "wrong password still rejected");
        check(
                alice.points == 3 && alice.wins == 1 && alice.losses == 0 && alice.played == 2,
                "legacy winner statistics preserved");
        check(
                bob.points == 1 && bob.wins == 0 && bob.losses == 1 && bob.played == 2,
                "legacy loser statistics preserved");
        List<Store.Match> history = store.history("legacy_alice");
        check(
                history.size() == 2 && history.get(0).id().equals("legacy-draw"),
                "all old matches retained in chronological order");
        check(
                history.get(1).start() == 100
                        && history.get(1).end() == 200
                        && history.get(1).scoreFirst() == 2
                        && history.get(1).winner().equals("legacy_alice"),
                "match IDs, timestamps, scores and winner retained");
        check(history.get(0).reason().equals("Hòa · dữ liệu cũ"), "Unicode legacy reason retained");
        String digest =
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(original));
        check(
                Arrays.equals(original, Files.readAllBytes(dir.resolve("nightchase.dat"))),
                "original dat untouched");
        check(
                Arrays.equals(
                        original,
                        Files.readAllBytes(
                                dir.resolve("legacy-backups/nightchase-" + digest + ".dat"))),
                "exact legacy backup created");
        store.register("new_user", "new-secret");
        store.finish(
                new Store.Match(
                        "after-migration",
                        "legacy_alice",
                        "new_user",
                        1,
                        0,
                        1,
                        "legacy_alice",
                        500,
                        600,
                        "New game"));
        Store reopened = new Store(dir);
        check(
                reopened.ranking().size() == 4 && reopened.history("legacy_alice").size() == 3,
                "restart neither reimports nor duplicates matches");
        check(
                reopened.login("legacy_alice", "old-alice-pass").points == 4,
                "new statistics survive alongside old statistics");
        check(scalar(dir, "PRAGMA user_version") == 1, "schema version recorded");
        check(
                scalar(dir, "SELECT count(*) FROM pragma_foreign_key_check") == 0,
                "migrated database has no broken foreign keys");
        try (Connection c = connect(dir);
                Statement s = c.createStatement();
                ResultSet r = s.executeQuery("PRAGMA integrity_check")) {
            r.next();
            check(r.getString(1).equals("ok"), "migrated database integrity");
        }
        Path corrupt = root.resolve("corrupt");
        Files.createDirectories(corrupt);
        byte[] broken = {1, 2, 3, 4};
        Files.write(corrupt.resolve("nightchase.dat"), broken);
        try {
            new Store(corrupt);
            throw new AssertionError("Invalid old data accepted");
        } catch (IOException expected) {
            checks++;
        }
        check(
                Arrays.equals(broken, Files.readAllBytes(corrupt.resolve("nightchase.dat"))),
                "corrupt original preserved");
        check(
                scalar(corrupt, "PRAGMA user_version") == 0
                        && scalar(corrupt, "SELECT count(*) FROM sqlite_master WHERE type='table'")
                                == 0,
                "failed migration rolls back schema and import marker");
        Files.write(corrupt.resolve("nightchase.dat"), original);
        check(
                new Store(corrupt).ranking().size() == 3,
                "migration can retry after valid data restored");
        Path badRelation = root.resolve("bad-relation");
        Files.createDirectories(badRelation);
        // This remains a valid legacy stream, but its match points at an unknown user.
        // Rewriting the object graph avoids relying on stream byte offsets.
        Object data;
        try (ObjectInputStream in = Store.legacyInput(new ByteArrayInputStream(original))) {
            data = in.readObject();
        }
        java.lang.reflect.Field matches = data.getClass().getDeclaredField("matches");
        matches.setAccessible(true);
        matches.set(
                data,
                new ArrayList<>(
                        List.of(
                                new Store.Match(
                                        "invalid",
                                        "legacy_alice",
                                        "missingbob",
                                        1,
                                        0,
                                        1,
                                        "",
                                        1,
                                        2,
                                        "invalid relation"))));
        try (ObjectOutputStream out =
                new ObjectOutputStream(
                        Files.newOutputStream(badRelation.resolve("nightchase.dat")))) {
            out.writeObject(data);
        }
        try {
            new Store(badRelation);
            throw new AssertionError("Invalid relation accepted");
        } catch (IOException expected) {
            checks++;
        }
        check(
                scalar(badRelation, "SELECT count(*) FROM sqlite_master WHERE type='table'") == 0,
                "partial legacy users rolled back after match failure");
    }

    static void transactions(Path root) throws Exception {
        Path dir = root.resolve("transactions");
        Store store = new Store(dir);
        check(
                store.register("alice", "secret123") && store.register("bob", "secret456"),
                "register SQL accounts");
        check(!store.register("alice", "different"), "unique account name");
        Store.User live = store.login("alice", "secret123");
        Store.Match match =
                new Store.Match("retry-match", "alice", "bob", 2, 0, 2, "alice", 1, 2, "Victory");
        sql(
                dir,
                "CREATE TRIGGER reject_second_update BEFORE UPDATE ON users WHEN NEW.name='bob'"
                    + " BEGIN SELECT RAISE(ABORT,'injected write failure'); END");
        expectFailure(store, match);
        unchanged(store, live, dir, match.id());
        sql(dir, "DROP TRIGGER reject_second_update");
        sql(
                dir,
                "CREATE TABLE commit_guard (user_name TEXT REFERENCES users(name) DEFERRABLE"
                    + " INITIALLY DEFERRED)");
        sql(
                dir,
                "CREATE TRIGGER reject_commit AFTER INSERT ON matches BEGIN INSERT INTO"
                    + " commit_guard VALUES('absent'); END");
        expectFailure(store, match);
        unchanged(store, live, dir, match.id());
        check(
                scalar(dir, "SELECT count(*) FROM commit_guard") == 0,
                "deferred commit failure rolls back trigger effects");
        sql(dir, "DROP TRIGGER reject_commit");
        store.finish(match);
        check(
                live.points == 2 && live.played == 1 && live.wins == 1,
                "live statistics updated after commit");
        check(
                store.login("bob", "secret456").losses == 1,
                "opponent statistics committed together");
        store.finish(match);
        check(
                live.points == 2 && live.played == 1 && store.history("alice").size() == 1,
                "retry same match committed exactly once");
        check(
                new Store(dir).login("alice", "secret123").points == 2,
                "successful retry survives restart");
        try {
            sql(dir, "INSERT INTO match_players VALUES('no-match','alice',0,0)");
            throw new AssertionError("Foreign key not enforced");
        } catch (SQLException expected) {
            checks++;
        }
        check(
                !new String(
                                Files.readAllBytes(dir.resolve("museumheist.db")),
                                java.nio.charset.StandardCharsets.ISO_8859_1)
                        .contains("secret123"),
                "database contains no plaintext password");
        sql(dir, "PRAGMA user_version=2");
        try {
            new Store(dir);
            throw new AssertionError("Future schema accepted");
        } catch (IOException expected) {
            checks++;
        }
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of("work"));
        Path root = Files.createTempDirectory(Path.of("work"), "sqlite-tests-");
        migration(root);
        transactions(root);
        System.out.println("PASS: " + checks + " SQLite migration and transaction assertions.");
    }
}
