# Ask Sakshi (the chat) and the one network use

Decision by the Integration Owner, 2026-10-07: Sakshi may use the `INTERNET` permission, over HTTPS only, for exactly one feature: **Ask Sakshi**, a chat about the person's own numbers. Everything else is unchanged: no account, no cloud storage, no sync, nothing else leaves the phone.

## What is sent
Only text the engine builds (`engine/ask/AskFacts.kt`): the app's own sentences and numbers for the period (stretch, pulls away, return, quiet, Steadiness, patterns with their evidence counts, today's finished windows, the Lake phrase), the instructions in `AskPrompt.kt`, the list of Vivekananda sayings from `sayings.json`, the last six turns of the chat, and the question the person typed. **Never** a package name, an app label, a raw event, a notification or its words. Tests (`AskSakshiTest`) check that no package name or app label leaves the phone. Gentle mode arrives already applied, so a gentle person simply sends fewer facts.

## What comes back, and what is done with it
- The model must answer in JSON: `{"answer": "...", "quote_id": "syNN" | null}`. The quote is printed **from the shelf by id**, so a quote can never be invented or altered. A real saying is always shown.
- Every reply goes through `AskGuard` (the copy rules of AGENTS.md section 7: no `should/must/need to`, no judging words, no `!`). A reply that breaks them, is malformed, times out (20 s) or fails is dropped and the **offline answerer** (`OfflineAnswerer.kt`) answers instead from the same numbers. With no key, or no network, the chat still works and says "Written on your phone. No network used."
- Each reply says where it was written. In real mode the person must see what is sent and tap once before the chat opens; in demo mode only made-up numbers are used.

## Ideas to try
When the question asks for ideas or how to improve, the service may offer one or two small things to try, as invitations tied to a number in the facts ("One small thing you could try: ..."), never orders. They are labelled as written by the language service. The Mirror's own suggestion, which the engine measures over two weeks, stays the only one the app judges.

## The API key
Copy `android/nim.properties.example` to `android/nim.properties` (git-ignored) and put the NVIDIA NIM key after `NIM_API_KEY=`. It is compiled into the APK (`BuildConfig`), so **anyone with the APK can read it out**: use a throwaway key with a spending limit and revoke it after the event. Without the file the chat answers offline. Models and base URL are in the same file: `NIM_MODEL` (default `openai/gpt-oss-20b`, normal answers) and `NIM_IDEAS_MODEL` (default `nvidia/nemotron-3-super-120b-a12b`, a reasoning model used only when the question asks for ideas), at `https://integrate.api.nvidia.com/v1` (OpenAI-compatible). Models are retired over time; if a call returns 410/404 the chat quietly answers offline, and `curl .../v1/models` lists what the key can use. Both defaults were checked against the live service on 2026-10-07. The real service has not been called from this repo's tests (they use a local fake); the first run with a real key is the first real check.

## Gates
`tools/audit_apk.sh` and `ReleaseManifestTest` still refuse every other forbidden permission, and now also require `usesCleartextTraffic="false"`.
