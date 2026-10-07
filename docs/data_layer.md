# Data layer semantics (T1.4)

The locked ports (LC-2) give signatures only. These are the meanings the Room stores implement; Track 2's in-memory fakes must match them.

| Port call | Meaning |
|---|---|
| `EventStore.range(from, to)`, `NotifStore.range` | half-open `[from, to)`, sorted by time then insertion order |
| `EventStore.append` | duplicates of `(ts, type, pkg)` are dropped silently, including screen events with no package (stored as `""`, because SQLite unique indexes treat NULLs as distinct) |
| `purgeBefore(ts)` | deletes rows with `ts` strictly older; returns the number deleted |
| `Retention.purge(asOf)` | cutoff = `asOf - RAW_RETENTION_DAYS`; an event exactly at the cutoff stays, one millisecond older goes |
| `ListenerCoverage.sessions(from, to)` | sessions touching `[from, to)`: connected before `to` and not ended at or before `from` |
| `coversInterval(from, to)` | the listener was connected for all of `[from, to]`; touching sessions join; an open session runs to the end of the asked interval; an empty interval is covered if a session holds that instant |
| `coverageFraction(from, to)` | connected time ÷ length, 0.0 to 1.0; an empty interval gives 0.0 |
| `openSession(at)` | does nothing if a session is already open (one connection is one session) |
| `closeSession(at)` | closes open sessions; an end before the connect time is raised to the connect time |
| `GapStore.closeOpenPause(at)` | closes PAUSED gaps with no end only; NOT_SEEN gaps are never touched |
| `GapStore.overlapping(from, to)` | half-open; an open gap reaches forever |
| `DerivedStore.windows(from, to)` | windows whose START is in `[from, to)` |
| `DerivedStore.days(from, to)` | inclusive at both ends (study days are whole units) |
| `DerivedStore.replaceDay` | one transaction: deletes that day's windows, stretches, stays and summary, writes the new ones. The engine gives each window a unique non-zero id; stretches and stays point at it through `windowId` |
| `DerivedStore.clearDerived()` | windows, stretches, stays, day and week summaries, patterns. Baseline, settings and other state stay |
| `DerivedStore.clearAll()` | every table, one transaction (Delete everything, F10) |
| `StateStore.baseline()` | the active baseline (latest row with `isActive`) |
| `StateStore.settings()` / `ingest()` / `note()` | plain defaults until first saved. Default `Settings.createdAt` is epoch 0, not a guess at "now"; the host saves real settings at first launch |

Schema notes
- Twenty tables, one per LC-2 entity (`app/schemas/.../1.json`).
- `window` also stores `quietMinutes` and `glances`. Those are `WindowWithDetail` fields (LC-2) that the `Window` entity does not carry; without them a stored window could not read back as the same `WindowWithDetail`.
- `suggestion_state` keys on `(kind, subject)` with `""` for no subject; `pattern` on `(kind, key)`.
- `Pattern.args` is a JSON object of strings; study blocks are `"start-end,start-end"`.
