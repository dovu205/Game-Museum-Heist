# Legacy migration fixture

`nightchase-v1.dat` is synthetic test data created by `tools/LegacyFixture.java` using the original pre-SQLite Store implementation. It contains three test accounts (`legacy_alice`, `legacy_bob`, `legacy_idle`) and two test matches. Test passwords are defined in the generator and in `StoreTest.java`; they are not real user credentials.

This fixture is intentionally versioned so migration tests run from a fresh checkout. Real player data lives under `data/` and must never be committed.
