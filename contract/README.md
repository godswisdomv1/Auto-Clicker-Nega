# Contract fixtures

Golden scripts shared by the Kotlin app tests and the Studio tests, so the two codebases cannot drift apart.

- `fixtures/valid/*.json` must decode and validate.
- `fixtures/invalid/*.json` must be refused with the error named in `fixtures/expectations.json`.
- `fixtures/expectations.json` maps each fixture path to `{"ok": true}` or `{"ok": false, "code": "<code>"}`.
  The codes are `bad_json`, `invalid_field`, `too_many_items`, `cycle`, `too_deep`, `too_large`, `bad_bundle`,
  `hash_mismatch` and `schema_too_new`.

A change to the script format means changing these files first, then both implementations.
