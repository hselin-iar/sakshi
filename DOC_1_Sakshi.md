# DOC 1 — Project Brief & Feature Specification: Sakshi (PS 04)

Oct 6, 2026 · @Purohitji · Team Kleos · PS 04: Strength of Mind in the Age of Distraction

## Table of Contents

- §1.0 — Decision Summary
- §1.1 — Design Thinking Foundation
- §1.2 — Feature Set (MoSCoW)
- §1.3 — Novel Feature Suggestions
- §1.4 — User Flow Overview
- §1.5 — Technical Differentiator
- §1.6 — Landscape and Judging Lens
- §1.7 — Source Integrity and Teaching Cards
- §1.8 — Assumptions, Risks and Open Questions

## §1.0 — Decision Summary

Build **Sakshi**: a local-first mobile web app that trains attention by making the user *watch* their own mind, not by blocking it.

- **The loop.** Declare one idea → run a focus session → log every pull (a tap, an auto-detected app-leave, or a "held") → see a Replay of pulled versus placed attention → guess your own pattern before the app shows it → pick your own counter-habit → the app steps back as you improve.
- **Why it fits PS 04.** The organizer brief, as summarised in the research doc, wants a system that helps people understand their attention rather than control it. The spine of the product is one line from the Complete Works: “Until you know what the mind is doing you cannot control it.” (Raja-Yoga, Pratyahara and Dharana.)
- **What is different (our judgment, §1.6).** Opal and Freedom block, Forest penalises, one sec delays. None makes *noticing* the thing you train.
- **Four hooks.** The catch is the rep, not zero drift. A resisted pull ("held") counts as a win. The app never answers for you: you guess, then it reveals. It fades out as you improve.
- **The demo moment: The Replay.** Hand a judge the phone for 60 seconds. They hold one idea, a staged pull arrives, and the app instantly replays *their own* attention and asks one question. Detail in §1.4.
- **Cut hard.** App blocking, streaks, notifications, any "strength of mind" score, accounts, and any chat with or quote generated as Vivekananda.

**Decisions for you now** (defaults assumed if you say nothing; full list in §1.8): team roster and who can code; the organizer's judging text if you have any; demo format; hosting and connectivity at the venue.

All outside numbers carry a source and an as-of date in §1.6. Anything not sourced is labelled ASSUMPTION, ESTIMATE or JUDGMENT.

## §1.1 — Design Thinking Foundation

The problem is not that young people lack willpower; it is that they cannot see where their attention goes, and existing tools take control away instead of building sight.

**Project name:** Sakshi (साक्षी, "the witness"). This is our product label. The research doc holds the witness teachings but not this word, so it never appears inside a quotation.

**Tagline:** Don't fight your mind. Watch it, train it, then put the app down.

### Problem statement

- **Who.** College students and early-career youth in India, about 17 to 25, who study and work on the same phone and laptop they use for entertainment. \[ASSUMPTION: confirm target user\]
- **Pain.** They start a task on purpose and are somewhere else within seconds, then blame themselves. They know *that* focus broke, not *when*, *to what*, or *what happened just before*.
- **Current state.** They block apps (Opal, Freedom), accept a penalty (Forest's tree dies), or add friction (one sec). Each acts on the phone. None trains the noticing. Reviewers report these tools fade ("great for a week, invisible by week three", an anecdotal vendor-blog analysis), and design research names abandonment as the main risk when users react against constraints (§1.6).
- **Root insight.** The unit of progress is the *catch*, not the absence of drift. People interrupt themselves about half the time (Mark, 2024), so blocking outside apps cannot cover it. And the lab method for measuring awareness of a wandering mind, self-caught versus probe-caught reports, has the same shape as the teaching: “Now strengthen the witnessing part and do not waste time in restraining your wanderings.” (class notes, recorded by others.)
- **Scale, with limits.** Average screen attention was about 47 seconds in 2016 to 2020 studies of work screens (Gloria Mark, UC Newsroom, 2 Jan 2024). Indians averaged about five hours a day on phone screens in 2024 (EY via Bloomberg, 27 Mar 2025). Both are population averages, not DTU students.

### Point of view

A distracted college student needs to *witness* their own attention leaving, in their own data, because control without observation fails and tools that control from outside get abandoned.

### How might we

- **HMW-1 (amplify the good):** How might we make the moment of noticing a drift feel like a rep, not a failure?
- **HMW-2 (remove the friction):** How might we capture a pull mid-focus in one tap, so the logging itself never becomes the distraction?
- **HMW-3 (reframe from the goal):** How might we measure attention *skill* (catching, returning, holding) instead of screen time?
- **HMW-4 (question an assumption):** How might we help if the app never blocks, nudges or notifies?
- **HMW-5 (goal behind the goal):** How might we make the user so capable that they stop needing the app?

**Jobs to be done.** Functional: finish the one thing I started. Emotional: stop feeling like a slave to my phone. Social: be someone who can focus when placements and exams arrive.

### Core value proposition

Sakshi turns every focus session into a replayable record of the user's own attention: what pulled, what held, how fast they came back. It then makes them predict, diagnose and choose their own counter-habit. No blocking, no streaks, no notifications, and the app steps back as the skill grows. Other tools control behaviour; this one builds the observer.

### Design principles and where each one comes from

| Principle | Product mechanism | Card in §1.7 |
| --- | --- | --- |
| Observe before you control | Replay and Pattern View | TC-05, TC-07 |
| Pulled versus placed attention | Pull taxonomy, Replay timeline | TC-02 |
| Practise slowly, return without shame | Held count, return time, lapse card, no streaks | TC-01, TC-08, TC-09, TC-11 |
| A counter-habit beats a fight | Own-experiment card | TC-06 |
| Build the muscle, don't outsource it | Guess-then-reveal, typed answers, fade-out | TC-12, TC-13, TC-14 |

The last row is our design principle. The research doc supports "stand on one's own feet" and "hearing is only one part", but states that nothing in it establishes a quotation about "do not outsource" or about screens. On slides, label it *our interpretation*, never his words.

## §1.2 — Feature Set (MoSCoW)

Eight Must-haves, about 21 hours of build effort in total, cover the full loop and the demo; everything else is ranked behind them. Hour figures are ESTIMATES for one developer working with an AI coding agent. Track splits belong in DOC 4.

### Must have (MVP: the project fails without these)

| ID | Feature | What it does and why it is load-bearing | Done when | Est. h |
| --- | --- | --- | --- | --- |
| M1 | One-Idea Session | The user types one task (80 characters at most) and picks a length (90 s for the demo, 10 or 25 min). A full-screen view shows only the task, the elapsed time and three buttons. It is the unit every metric hangs on. From the extra-ideas list (One-Idea Vow): the task is written as a one-sentence vow plus a short why, and the session ends with kept, bent or broken. It reuses the yes / partly / no goal check, so it costs about 30 minutes. | Starts in two taps after typing; nothing else is on screen; ends with a summary; works offline. | 2 |
| M2 | Pull Capture | Three inputs: "I drifted" tap (self-caught); automatic away detection when the page becomes hidden, computed from timestamps; "Held" tap (felt a pull, stayed). On return, one optional label: Notification, Feed, Urge to check, My own thoughts, Other. No data, no PS 04. | Each event saved with time and type; away time within ±1 s on two phones and one laptop; skipping the label never blocks the return. | 3 |
| M3 | Replay | After the session: a timeline of placed stretches, pulls, catches and holds; four numbers (longest unbroken stretch, catches, returns, holds); one teaching card; one question the user answers in their own words (140 characters at most). This is the demo moment. | Renders in under 1 s at 360 px width; every mark traces to a stored event; the question can be skipped. | 4 |
| M4 | Pattern View with Guess-then-Reveal | Before showing data, asks two guesses (when do I lose focus most; what pulls me most). Then reveals real versus guessed, plus three rule-based insights, each with its count and the sessions behind it. Covers the brief's four patterns: concentration, task switching, interruptions, goal completion (a yes / partly / no after each session). | No insight on fewer than 3 events ("not enough yet"); each insight opens the events behind it; the sample week is always labelled "Sample". | 4 |
| M5 | Own-Experiment Card | The user picks or writes one if-then plan ("If I feel the urge to check, then I will turn the phone face-down"). The next session reminds them; afterwards it asks "Did you do it?" and shows pulls per 10 minutes before and after, with the sample size. | One active experiment at most; shows "too early to tell" under 3 sessions; never auto-assigned. | 2 |
| M6 | Teaching Cards with Source Badge | 15 vetted cards (§1.7) appear at fixed moments, each with a source line and a tier badge. A Sources screen lists them all. This is what makes the build "grounded in actual teachings". | Text byte-identical to §1.7; no generated quotes; every card has a source and a tier. | 2 (+1 content) |
| M7 | Local-first data | Everything stays on the device. Export JSON or CSV, delete-all button, and "Load sample week" for the demo. Easier than a backend, and it is the privacy answer to the brief. | Works in airplane mode; the browser network tab shows no attention data leaving the device. | 2 |
| M8 | Mobile PWA shell | Installable, 360 to 430 px layouts, large touch targets, opens from a QR code. | Installs on Android Chrome; loads from a QR code on a judge's phone. | 2 |

### Should have (build in this order if the Musts pass)

Order: S3, S5, S1, S4, S2. S3 goes first because Solo mode is the demo's closing beat. The rest are off the demo's critical path.

| ID | Feature | What it does | Why it is not MVP |
| --- | --- | --- | --- |
| S5 | Lapse-return screen | After 3 or more days away: the lapse card, no streak language, a 60-second restart session. | Needs only a screen and one rule; promote right after S3. |
| S3 | Fade-out (Solo mode) | After several sessions of steady catches the app offers Solo mode: timer only, no prompts, minimal logging, return any time. The threshold is a placeholder to tune. | It is the demo's closing beat, so build it first of the Shoulds. It needs M1 to M5 data to trigger honestly, so the demo phone runs on data labelled Sample. |
| S1 | Probe sampling | One to three random "Where is your mind right now?" prompts in sessions of 10 minutes or more (answers: With my task / Somewhere else). Catch rate = self-caught ÷ (self-caught + probe-caught). Differentiator A, §1.5. | The demo session is 90 s; interpretation needs a clear caveat. |
| S4 | Restless-mind check-in | A 1 to 5 slider and one optional line before and after a session; shows before-to-after. | Good signal, not on the core path. |
| S2 | AI Mirror | After the Replay, one question (20 words at most) built from the user's own events; the user must answer before moving on. The model may choose a card ID, never writes a quote, gives no advice, and falls back to templated questions offline. | Depends on an API and venue wifi, and the core loop does not need it. |

### Could have (backlog)

- Share card: one-tap PNG of the Replay timeline plus one card.
- Body inputs: sleep, movement, breaks as three optional sliders shown beside focus (research doc idea; Passage 133 and 134 support it).
- Hindi or Hinglish interface shell (quotes stay in English).
- Detach drill: a 10-second deliberate "put it down" at the end of a session (TC-10).
- Source Shelf: browse and filter the cards by source type.
- Weekly review page.
- Silence Round (from the extra-ideas list): a 2 to 5 minute phone-down timer with a bell and a closing "what came up?" typed line. About 1 hour. It trains boredom tolerance and fills the research doc's stated gap on silence practice.
- Chitta Pond (from the extra-ideas list): a calm water-ripple background on the session screen that ripples on each logged pull and settles when attention holds. About 2 hours. Only if the Replay is already finished; it must never become a second demo moment.
- Try-first rule for the AI Mirror (S2), from the Effort Receipt idea: the user types their own one-line reading of their week before the Mirror shows its own. No extra build beyond S2.

### Won't have (explicitly cut)

| Cut | Why |
| --- | --- |
| App or site blocking, screen-time capture | Not possible on the web, contradicts "understand, don't control", and Opal and Freedom own it. |
| Streaks, XP, badges, leaderboards | Research doc says success is returning after a lapse. A 2026 vendor-blog review analysis reports streak pressure backfiring (anecdotal). |
| Push notifications and reminders | The brief criticises notification-heavy productivity tools. Quiet by design is a stated stance. |
| A "strength of mind" score | The research doc says do not claim to measure it; say it measures patterns of attention. |
| Chat with, persona of, or generated quotes from Vivekananda | The research doc forbids invented quotes; a persona would invent them constantly. |
| Accounts, cloud sync, social features | Privacy and time. |
| Camera, eye-tracking, EEG, wearables | Privacy and hardware risk. |
| Pranayama audio, "powers" content, clinical claims (ADHD, anxiety) | The research doc cautions on supernatural and medical claims. This is not a medical tool. |
| Native app | A PWA reaches a judge's phone from a QR code in seconds. |
| Substitute Offer, Effort Receipt on external AI or search | A web app cannot see which app or site the user opens, so it cannot intercept. Do not promise what the platform cannot do. |
| Notification Flood Drill, Steadiness Under Load Test | Simulated pings compete with the staged banner in the Replay demo, and return time is already covered by N3. Revisit after the hackathon. |
| Cheerfulness Ledger, Weakness Compass, Samskara Map | Each is a second product: a worry manager, a self-assessment and a path visualisation. They overlap the Own-Experiment card and the Pattern View, which already do the job. Guru Mirror is already S3, the fade-out. |

### Scope-creep flags

- **An AI coach that chats.** It sounds like the obvious use of AI and contradicts "build the muscle, don't outsource it".
- **More teachings.** 303 passages exist; the product uses 15.
- **Analytics beyond three insights.** The Replay is the product, not a dashboard.
- **Intercepting real notifications.** Impossible on the web; do not start.
- **Polishing Hindi before the Replay works.**

## §1.3 — Novel Feature Suggestions

Eight ideas set Sakshi apart from the blockers and timers in §1.6. N1 to N5 add no scope. N6, N7 and N8 are the added differentiators: each is derived from a teaching card rather than from app brainstorming, and together they add about 3 hours of effort (estimate), partly offset because N6 absorbs M5 and replaces the M4 chart.

| # | Idea | What it is | Lens that surfaced it | Build hint | Maps to |
| --- | --- | --- | --- | --- | --- |
| N1 | The Held log | A pull you felt and did not follow is logged as a win and counted, as a count and never a streak. | Worst idea reversed. Worst idea: kill a tree when the user leaves. Reverse: celebrate the resisted pull. | One extra button; show "held N times" on the Replay. Lab support is narrow: incentives for self-catching raised self-catches without raising overall mind wandering in a reading task (Zedelius, Broadway and Schooler, 2015). | M2, M3 |
| N2 | Guess-then-Reveal | The app asks you to predict your pattern before it shows the data; the gap between belief and data is the insight. | SCAMPER, Reverse: the user answers first, the app confirms second. | Two chip questions before the Pattern View; show "you said X, data says Y". | M4 |
| N3 | Return time | Time away is shown as "how fast you came back", measured from timestamps, not as "how long you were gone". | Reframe from the goal: the skill is returning. | Compute from `visibilitychange` timestamps, never from timers (background tabs throttle them). | M2, M3 |
| N4 | Fade-out | After steady catches the app offers Solo mode and steps back. Pitch line: the best version of Sakshi is an empty screen. | SCAMPER, Eliminate. Analogy: coaching and physiotherapy withdraw support as skill grows. | A rule on the last few sessions; Solo mode hides every prompt. | S3 |
| N5 | A card after the rep, never a feed | A teaching appears only after an action, never as a daily quote. | Analogous inspiration: a coach speaks after the set, not instead of it. | A lookup table from event type to card ID; no generation. | M6 |
| N6 | Counter-habit Path | Each logged pull has a trigger label (M2). On return, one tap records what the user did next: followed it, or their own written counter-habit. The Pattern View draws trigger to response as lines whose thickness is the count, so the user watches the old path thin and the counter-habit path thicken. | From the teaching, not from brainstorming: TC-06 says the only remedy for bad habits is counter habits (Raja-Yoga, own published text, Tier A). Prior art for if-then plans on phones exists (Birmingham, "If This, Then Habit"); we have not verified that nobody pairs it with self-logged triggers, so do not claim novelty on stage. | SVG lines drawn from counts M2 already stores. The M5 card becomes the place where the user writes the counter-habit. About 2 h net. | M2, M4, M5 |
| N7 | Forecast vs Reality | Before a session the user guesses how many times their mind will pull away (0 to 10). The Replay shows the guess beside the actual count, and the gap over sessions: "your forecast was off by 4, now off by 1". | TC-07, "until you know what the mind is doing you cannot control it", turned into something measurable: knowing your own mind. It extends N2 from a one-off guess to every session. | One number input in M1 and one comparison line in M3. About 45 minutes. A product choice, not a validated measure; never present it as a strength score. | M1, M3 |
| N8 | In his own hand | The Vivekananda touch, with no persona and no generated words. Cards from his letters (TC-10, TC-15) use a letter frame, ink colour and the label "From a letter". Cards recorded by others use a plain frame and "As recorded by others". The user sees at a glance whose words these are. | Trust as a design feature. The research doc separates his own writing from reported speech, and most apps and quote walls never do. | Two CSS card frames chosen by the tier field in cards.json. About 45 minutes inside M6. Our interpretation stays labelled "Our interpretation" on every screen. | M6 |

One caution on N1, N3, N6 and N7: these are product choices, not validated outcomes. We have not measured whether they improve attention, and we should say so if asked.

## §1.4 — User Flow Overview

Every session ends in a Replay, and the Replay feeds the next session; that loop is the whole product.

&#91;embedded content: user flow · 10 steps, 3 branches, 1 loop\]

The accent box is the demo moment. Three branches change the path: a lapse of three or more days, the first three sessions (no Pattern View yet), and steady catches (Solo mode).

### The demo moment: The Replay

The single moment that should win the room is **The Replay: hand a judge the phone for 60 seconds.** It is the only point in the pitch where the judge's own mind is the data.

1. **Before the room.** The demo phone has Sakshi installed, airplane mode on, and a sample week loaded and labelled "Sample". A demo-only switch shows a generic "New message" banner at about 25 seconds. \[ASSUMPTION: a staged pull is acceptable; say out loud that it was staged.\]
2. **The ask (10 s).** One line of problem with the 47-second figure and its source (§1.6), then: "Hold one idea for sixty seconds."
3. **The session (60 s).** The judge types one idea and taps start. Teammates keep talking normally. At about 25 s the banner appears for 8 s. If they tap it or leave the page, it is logged as a pull with a return time; if they tap Held, or ignore it for the 8 s, it is logged as "held" (the Replay marks it "staged pull").
4. **The Replay (20 s).** Instantly: their timeline, four numbers, one card, and one question, "What pulled you, in your own words?" The card reads “Now strengthen the witnessing part and do not waste time in restraining your wanderings.” and is badged as class notes recorded by others. They type three to five words. The app does not advise.
5. **The flip (15 s).** Switch to the "Week 4" phone: Solo mode, an almost empty screen. Line: "The best version of Sakshi is an empty screen." Close with TC-14, badged as a reported saying, and the label "Our interpretation: the app steps back."
6. **If no judge takes the phone.** The presenter runs it with a teammate staging the pull. Same flow, same 60 seconds. A saved Replay from a rehearsed session opens in one tap if the live run fails; DOC 5 holds the rest.

**Why this wins.** The full loop fits in 60 seconds. It works with no network. It needs no explanation, because the Replay is self-evident. And it enacts the teaching instead of quoting it: observe first, then control.

### Edge cases

- **User never returns.** The session closes after 5 minutes away and is saved as "ended away".
- **Rapid taps.** A second "I drifted" within 2 seconds is ignored.
- **Phone locks or a call arrives.** Treated as away; the label step offers "Phone locked or call".
- **Tab closed mid-session.** Events are saved one by one; on reopening, offer Resume or Discard.
- **Judge will not type.** Two preset ideas as chips: "Read one paragraph", "Write one sentence".

## §1.5 — Technical Differentiator

Take **B** (quote integrity) as a certainty and **A** (catch rate) as the one real bet; **C** is already inside M4's acceptance criteria. These are bets you are choosing, not requirements. Milestone gate for A: start it only after M1 to M5 pass an end-to-end run on a real phone.

### A. Catch rate: self-caught versus probe-caught attention

- **What it is.** During sessions of 10 minutes or more, one to three random prompts ask "Where is your mind right now?" (With my task / Somewhere else). Catch rate = pulls the user caught themselves ÷ (caught by themselves + caught by a probe). A probe-caught "somewhere else" is a drift that escaped awareness.
- **Why it fits.** It turns the witness teaching into a number a technical judge respects. In the lab literature, self-caught reports reflect meta-awareness and probe-caught reports reveal lapses that eluded it (Schooler and colleagues; see PMC5823741). It also shows the brief's "why focus is lost" in a way a timer cannot.
- **Feasibility.** Medium confidence (about 3 to 4 hours: scheduler, two-button prompt, ratio, one chart). Risks: small samples, prompts that themselves interrupt, and nobody having validated this on phones in daily life. \[ASSUMPTION: the lab method carries over; do not claim clinical validity.\]
- **Fallback.** Drop the probes. M2 and M3 already deliver catches and return time; S1 is purely additive.
- **Literature-backed:** yes.

> **Research prompt for a literature tool (opt-in, not a gate):** "What are validated approaches (last \~5 years) for measuring mind-wandering and meta-awareness in everyday life using self-caught versus probe-caught reports, for example smartphone experience sampling, and what are their reported trade-offs and failure modes, including reactivity, probe frequency, response bias and the minimum number of samples for stable estimates?"

If you bring back findings, DOC 2 will cite what the papers actually said, including the reasons to reject alternatives. If you skip it, I proceed on best judgment.

### B. Quote integrity by construction

- **What it is.** The renderer prints cards only from one JSON file of verbatim text. Any model (S2) may select a card ID and nothing else. A unit test fails the build if any rendered quotation is not an exact match to the allowed set.
- **Why it fits.** The brief says ground it in his actual teachings, and the research doc forbids invented quotes. The pitch line is "We cannot hallucinate Vivekananda."
- **Feasibility.** High confidence, about 2 hours.
- **Fallback.** No model at all: cards chosen by a fixed table from event type to card ID (this is already the M6 design).
- **Literature-backed:** no, it is an engineering guarantee.

### C. Local-first and explainable by default

- **What it is.** All analysis runs on the device, and every insight opens the raw events behind it.
- **Why it fits.** The brief asks for understanding, not control, and for privacy-respecting data. Explainable rule-based insights are an honest contrast to opaque "AI insights".
- **Feasibility.** High, because it is folded into M4 and M7.
- **Fallback.** Insights show counts only, without the drill-down.
- **Literature-backed:** no.

**Rejected:** an on-device language model. Venue laptops and phones vary, WebGPU support is uneven, and 24 hours cannot absorb the risk. \[JUDGMENT\]

## §1.6 — Landscape and Judging Lens

The market has blockers, penalties and delays; the open ground is training the noticing itself, and the research on attention tools points to abandonment, not missing features, as the main way they fail. Sources are listed at the end of this section, as of 6 Oct 2026.

&#91;embedded content: positioning map · 5 apps, 1 hypothetical, 2 axes\]

The hypothetical chat coach is the design we reject (§1.2): it reads your attention for you.

### Comparable apps

| App | Mechanism | Gap against PS 04 |
| --- | --- | --- |
| Forest | Grows a virtual tree while you stay in the app; the tree dies if you leave ([Illinois founders blog](https://blog.founders.illinois.edu/?p=11089)). | A penalty for leaving, no view of why; reviewers say gamified novelty fades fastest ([unstar.app](https://unstar.app/blog/opal-forest-freedom-one-sec-jomo-screen-time-apps-ranked-2026), anecdotal). |
| Opal | Blocks chosen apps and sites for set periods, with focus modes and usage statistics ([Illinois founders blog](https://blog.founders.illinois.edu/?p=11089)). | Control sits with the app; it counts time but does not show the moment attention left. |
| Freedom | Blocks apps, sites or the whole internet across devices, with schedules ([Opal's comparison page](https://opalapp.com/blog/opal-vs-freedom)). | Same control model; reviewers report it is easy to bypass ([unstar.app](https://unstar.app/blog/opal-forest-freedom-one-sec-jomo-screen-time-apps-ranked-2026)). |
| one sec | A breathing pause of about 10 s before a chosen app opens, with an option to dismiss ([PNAS abstract](https://doi.org/10.1073/pnas.2213114120)). | Best evidence in the category (below), but it acts at the moment of opening and shows nothing about what happens inside a session. |
| Pomodoro timers | Fixed work and break blocks. | Gloria Mark calls it one-size-fits-all: people's attention rhythms differ ([UC Newsroom, 2 Jan 2024](https://www.universityofcalifornia.edu/news/how-sharpen-your-attention-and-meet-your-goals-2024)). |
| **Sakshi** | Observe, label, guess, run your own experiment, then fade. | Outcomes unmeasured; self-report; small data per user. |

### What the evidence supports for the design

| Finding | Source and date | What we do with it |
| --- | --- | --- |
| Average attention on one screen fell from about 150 s (2003 study, published 2004) to 47 s (2016 to 2020 studies of work screens). Recovering from an interruption can take almost half an hour. About half of interruptions are self-inflicted. | Gloria Mark, [UC Newsroom Q&A](https://www.universityofcalifornia.edu/news/how-sharpen-your-attention-and-meet-your-goals-2024), 2 Jan 2024 | Log self-caused and outside pulls separately; learn the user's own rhythm in the Pattern View; no fixed Pomodoro. |
| Indians averaged about five hours a day on phone screens in 2024, about 70% on social media, gaming and video. | EY via [Bloomberg](https://www.bloomberg.com/news/articles/2025-03-27/indians-spent-1-1-trillion-hours-on-smartphones-in-2024-ey-says), 27 Mar 2025 | Problem framing only. It is a population average, not a student figure. |
| one sec: 280 participants over 6 weeks, actual openings of target apps down 57%. In a preregistered online experiment (N = 500) the time delay cut consumption and the deliberation message did not. | [PNAS abstract](https://doi.org/10.1073/pnas.2213114120), vol. 120 (2023); abstract read via search excerpts, full text not opened | Friction and an easy exit work; a message alone did not. So the teaching is the frame and the catch, label and experiment loop is the mechanism. We must not imply Sakshi has any measured effect. |
| 367 digital self-control apps and extensions reviewed. A major challenge is users reacting against self-imposed constraints and abandoning the tool. | Lyngs et al., [CHI 2019](https://arxiv.org/abs/1902.00157v1); Schwartz, [CHI 2021 Extended Abstracts](https://iris.polito.it/retrieve/e384c433-4722-d4b2-e053-9f05fe0a1d67/chi21c-sub1604-cam-i16.pdf) | No hard locks, no punishment, always an exit. |
| If-then plans: 94 tests, effect d = .65 (medium to large) on goal attainment. | Gollwitzer and Sheeran, [2006](https://kops.uni-konstanz.de/entities/publication/2e749bfb-8533-437c-8203-7e788c910c5f) | The Own-Experiment Card uses the if-then form. The evidence is for goal attainment generally, not attention specifically. |
| Offering incentives for self-catching raised the number of self-catches without raising overall mind wandering, in a lab reading task that also used a bogus-pipeline deception. | Zedelius, Broadway and Schooler, Consciousness and Cognition 36:44–53, 2015 ([abstract](https://ucsb.academia.edu/ClaireZedelius)) | Reward catches, holds and return time, not zero drift. We will not copy the deception, and self-reports can be inflated. |
| `visibilitychange` fires when a tab goes to the background, the window is minimised or the screen is off. Widely available since July 2015. Background timers are throttled. | [MDN Page Visibility API](https://developer.mozilla.org/en-US/docs/Web/API/Page_Visibility_API), page last modified 30 Dec 2025 | M2 auto-detect from timestamps, not timers. Whether switching apps on a phone fires it must be tested on the team's own phones. \[ASSUMPTION\] |

### UX patterns to borrow and avoid

- **Borrow:** friction with a visible exit (one sec); one primary action per screen; the evidence behind every insight one tap away; quiet defaults; progress shown as returning, not streaking (research doc, Principle 5).
- **Avoid:** streak and shame counters (a reviewer quoted in the unstar.app analysis says streak pressure made them more obsessed with the app than the apps it blocked); failures that happen silently; permanent banners.

### What a Vivekananda-themed panel will probably reward

I found no published rubric for this event: searches for its name returned unrelated hackathons (for example VIPS-TC's HackVSIT and DTU's 2021 Innovathon). The table below is therefore \[JUDGMENT\], built from a generic Indian-hackathon rubric (innovation 20 to 25%, technical execution 20 to 25%, impact 20 to 25%, demo 15 to 20%, completeness 10 to 15%; [Reskilll blog, 2026](https://blogs.reskilll.com/what-hackathon-judges-look-for-complete-judging-criteria-breakdown-2026/), unverified for this event) plus theme fidelity.

| Likely criterion | What we show | Proof on the table |
| --- | --- | --- |
| Fidelity to the teachings | 15 verbatim cards, each with a source and a tier badge | The Sources screen; the quote-integrity test (§1.5 B) |
| Innovation | Catch as the rep, Held, Guess-then-Reveal, Fade-out | The Replay |
| Technical execution | Local-first PWA, timestamp-based away detection, explainable insights | Airplane-mode run; open an insight to see its events |
| Impact | Youth focus, honest scale figures, no efficacy claims | One slide with sourced numbers |
| Demo and completeness | The full loop in 60 seconds | The Replay on a judge's phone |
| Mobile-readiness (unwritten) | Many judges check on their own phone, per the same blog | A QR code that opens the installed PWA |

Optional framing for a slide, from the research doc: a press report (Passage 243) says the aim of the old educational system was "man-making" and not cramming. That is a reporter's account, not his own words.

### Sources

Opened in full: UC Newsroom Q\&A; Konstanz record for Gollwitzer and Sheeran; the Zedelius abstract on Academia; MDN. Read only as search excerpts, full text blocked or not opened: the PNAS paper for one sec (PMC and the Heidelberg library page refused access), the Schooler-lab self-caught/probe-caught paper [PMC5823741](https://pmc.ncbi.nlm.nih.gov/articles/PMC5823741), the Bloomberg/EY piece, and the vendor and review blogs ([unstar.app](https://unstar.app/blog/opal-forest-freedom-one-sec-jomo-screen-time-apps-ranked-2026) is a vendor blog and anecdotal). The one sec team's own page, [one-sec.app](https://one-sec.app/max-planck-study), repeats the 57% figure.

## §1.7 — Source Integrity and Teaching Cards

Every quotation in the product comes from one file of 15 verbatim cards, and each card carries a source and a tier that decides how we may attribute it. This is how "grounded in actual teachings" survives a judge's question. All texts below are copied from the research doc; the doc itself says it was checked for fidelity against its own earlier files, not freshly against every original chapter.

### Attribution tiers

| Tier | Applies to | On-screen attribution | Never say |
| --- | --- | --- | --- |
| A | Raja-Yoga published text and his letters | "Raja-Yoga, \[chapter\]" or "From a letter" | "he wrote" for Raja-Yoga lines (the research doc calls them published lectures and commentary) |
| B | Recorded lectures and talks | "From a lecture, Complete Works Vol. N" | "he wrote" |
| C | Reported by others: disciple diaries, class notes, reported sayings, press | "As recorded by others", plus the type | "he said", "he wrote" |
| D | Authorship unresolved (the research doc's "Lecture or writing" or no label) | "Complete Works, Vol. N, \[chapter\]", with no verb | "he said", "he wrote" |

### Rules

1. **One source of truth.** Cards live in one `cards.json`. A test fails the build if any rendered quotation is not an exact match, including the curly apostrophes.
2. **The model never writes, paraphrases or translates a quote.** It may select a card ID. The interface shell may be translated later; the quotes stay in English.
3. **Every card shows its source line and tier badge.** "About this source" shows the research doc's type note.
4. **Never present Belur Math's biography as his words** (BM01 to BM06 in the research doc). Only the quotation inside BM07 is his, and the letter is unnamed.
5. **No voice or audio.** The Belur Math fact-check page says no recording of his voice has been traced, so do not play or imply "original voice" audio.
6. **No supernatural or medical claims.** Skip the "powers" passages; historical health accounts are not medical evidence. The research doc also states that nothing in it is a quotation about screens or modern distraction, so every link from his words to a phone is ours.
7. **Label our interpretation.** On screen and on slides, "His words" and "Our interpretation" are different labels.
8. **Raja-Yoga lines were checked against the Wikisource copy of Vol. 1, not the Advaita Ashrama print edition.** Wording may differ slightly; check against print before anything is printed.
9. **Sample data is always labelled "Sample".**

### The 15 cards

| ID | Moment | Verbatim card text | Source and the research doc's type note | Tier | Ships |
| --- | --- | --- | --- | --- | --- |
| TC-01 | First run: why practice is slow | “The will has to be strengthened by slow, continuous, and persevering drill.” | Complete Works Vol. 5, The Aim of Raja-Yoga. Tagged "lecture or writing" in the research doc, so authorship is unresolved. | D | MVP |
| TC-02 | Explaining pulled versus placed | “We should put our minds on things; they should not draw our minds to them.” | Complete Works Vol. 6, Concentration and Breathing. Tagged "lecture or writing"; the research doc's own Principle 2 cites it under "Lectures and Discourses". | D | MVP |
| TC-03 | Declaring one idea | “Give up, once for all, this nibbling at things. Take up one idea.” | Awakening India, 1.2 (text as published on ereads.rkmm.org; Complete Works page not given). Type not labelled. | D | MVP |
| TC-04 | After an "I drifted" tap | “Let the monkey jump as much as he can; you simply wait and watch.” | Raja-Yoga, Pratyahara and Dharana (Vol. 1). Tagged "lecture or writing", so authorship is unresolved. Other lines in this chapter (RY03, RY05) are labelled his own published text. | D | MVP |
| TC-05 | On the Replay | “Now strengthen the witnessing part and do not waste time in restraining your wanderings.” | Complete Works Vol. 6, Lessons on Raja-Yoga. Class notes, reported speech recorded by others. | C | MVP |
| TC-06 | Choosing an experiment | “The only remedy for bad habits is counter habits; all the bad habits that have left their impressions are to be controlled by good habits.” | Raja-Yoga, Concentration: Its Spiritual Uses (Vol. 1). Own published text (RY01). | A | MVP |
| TC-07 | Before Guess-then-Reveal | “Until you know what the mind is doing you cannot control it.” | Raja-Yoga, Pratyahara and Dharana (Vol. 1). Same type note as TC-04. | D | MVP |
| TC-08 | Week one, when logged drifts rise | “In the first few months you will find that the mind will have a great many thoughts, later you will find that they have somewhat decreased, and in a few more months they will be fewer and fewer, until at last the mind will be under perfect control; but we must patiently practice every day.” | Raja-Yoga, Pratyahara and Dharana (Vol. 1). Own published text (RY05). | A | MVP |
| TC-09 | A plateau in the numbers | “Some days or weeks when you are practising, the mind will be calm and easily concentrated, and you will find yourself progressing fast. All of a sudden the progress will stop one day, and you will find yourself, as it were, stranded.” | Raja-Yoga, Concentration: Its Spiritual Uses (Vol. 1). Own published text (RY07). | A | Later |
| TC-10 | End of session: put it down | “We put all our energies to concentrate and get attached to one thing; but the other part, though equally difficult, we seldom pay any attention to—the faculty of detaching ourselves at a moment’s notice from anything.” | Complete Works Vol. 6, letter to Margot. Letter, his own writing. | A | Could |
| TC-11 | Lapse-return screen | “The remedy for weakness is not brooding over weakness, but thinking of strength.” | Complete Works Vol. 2, Practical Vedanta Part I. Lecture (delivered talk; published transcript, reported speech, not his own writing). | B | Should (S5) |
| TC-12 | Typing your own answer on the Replay | “Hearing is only one part; and the other part is doing.” | Complete Works Vol. 9, Bhakti-Yoga. Lecture or class, recorded text (not his own writing). | B | MVP |
| TC-13 | About screen and pitch | “We want that education by which character is formed, strength of mind is increased, the intellect is expanded, and by which one can stand on one’s own feet.” | Complete Works Vol. 5, Shri Surendra Nath Sen, From Private Diary. Disciple's diary of conversation (reported speech). | C | MVP |
| TC-14 | Solo mode, the fade-out | “How often does a man ruin his disciples by remaining always with them! When men are once trained, it is essential that their leader leaves them; for without his absence they cannot develop themselves!” | Complete Works Vol. 9, Sayings and Utterances. Reported saying (recollection by others, not his own writing). | C | Should (S3) |
| TC-15 | Onboarding: the one card in his own hand | “Arise, ye mighty one, and be strong!” | Complete Works Vol. 6, letter to Sharat and Kripananda (6.4.34). Letter, his own writing. Only this sentence is used; the sentences before it are about someone who blames the place. | A | MVP |

### Cautions on specific cards

- **TC-08** says the mind "will be under perfect control". Frame it as a traditional teaching; Sakshi promises nothing of the kind.
- **TC-14** is about teachers and disciples. Applying it to an app is our interpretation and is labelled so on screen.
- **TC-13** supports "stand on one's own feet". "Do not outsource" is our wording, not the source's.
- **TC-03:** "nibbling" is the source's own word, and the research doc suggests using it with the citation. Its type is unlabelled, so it stays Tier D.
- **TC-02:** confirm its type in the Vol. 6 front matter; if it is a recorded lecture it moves to Tier B. About 30 minutes for the content owner.
- **TC-01, TC-04, TC-07:** the research doc tags all three "lecture or writing", so they are Tier D. Show the source line with no verb. TC-07 is the spine line of the pitch: say "from Raja-Yoga", never "he wrote".
- **Content owner:** this is a good job for the teammate who does not code. Load the cards byte for byte and write each "About this source" note.

## §1.8 — Assumptions, Risks and Open Questions

Four answers would change this plan; everything else below is assumed and flagged so you can overrule it.

### Where I would challenge the plan

- **The research doc is a quote bank, not a product.** Judges score a working product and a pitch. Of 303 passages, the build uses 15. A quote wall on a timer loses to a smaller product that behaves the way the teaching says.
- **"Grounded in his teachings" is a claim you must defend line by line.** The research doc says nothing in it speaks about screens, so every bridge from his words to a phone is yours. Say "our interpretation" out loud.
- **Quotes are not the mechanism.** In the one sec experiment the deliberation message did not work; the delay did. If a judge asks what changes behaviour here, the answer is catch, label, guess and experiment, not the card. Nobody on the team should pitch the quotes as the intervention.
- **Protect the Replay.** The tempting time sinks are a dashboard, an AI chat and Hindi polish. Each one is a way to arrive tomorrow with five half-features and no moment.

### Assumptions

1. Web PWA, local-first, no backend; DOC 2 confirms the stack. \[JUDGMENT\]
2. Target users are Indian college students on Android phones plus laptops. \[ASSUMPTION\]
3. The Musts, about 21 hours \[ESTIMATE\], fit the team, and at least two of the five can code with an AI agent.
4. A QR code to a hosted page works at the venue; if not, the demo runs from the laptop and phones on a local network.
5. `visibilitychange` fires on app switch and screen lock on the target phones. Test on Android Chrome and iOS Safari in the first hour. If it fails, M2 falls back to the manual tap and the Replay says "away detection unavailable".
6. An LLM API may be unavailable or unreliable at the venue; S2 is optional by design.
7. A staged banner in the demo is acceptable if disclosed.
8. English is enough for the interface tomorrow.

### Risks

| Risk | Impact if it hits | Mitigation |
| --- | --- | --- |
| Quote wall, a sticker on a timer | Reads as shallow | Cards appear only after actions. Test: hide the cards; is the product still useful? |
| Wrong or overclaimed attribution | Loses credibility on the theme's own ground | §1.7 tiers, exact-match test, content owner checks TC-02 |
| Overbuilding | A half-working demo | The Won't list. Descope order for DOC 4: cut S2, then S1, then S4 before touching any Must. |
| Live demo fails (judge declines, network, phone quirks) | Lose the moment | Offline-first PWA, a presenter-run version, a saved Replay that opens in one tap |
| Efficacy overclaim | one sec's 57% is theirs; ours is unmeasured | Say: a prototype of a method, outcomes not yet measured |
| Logging breeds guilt | A self-blame loop | Held count, return time, no streaks, no scores, an exit on every screen; not a clinical tool |
| Away detection misses events | Wrong numbers | Timestamps not timers; test on two phones and a laptop; show "unavailable" honestly |
| Sample data mistaken for real | A deceptive demo | A persistent "Sample" label, with a real session shown beside it |
| A false privacy claim | Credibility | No analytics scripts; verify in the browser network tab before saying "on-device" |

### Open questions that change the plan

1. **Team roster.** For each of the five: name, how comfortably they code and in what, what the non-coders are strong at (design, writing, research, speaking), and hours available. This sets the tracks and decides whether S1 is realistic. *Default if you say nothing:* two code, one designs, one owns the cards and Sources screen, one owns the pitch and QA.
2. **The organizer's own words.** Paste any judging criteria, brief text or rules, even a forwarded message. If a rubric exists or AI use is required, I will re-weight; for example, promote the AI Mirror (S2) from Should to Must.
3. **Demo format.** Slot length, projector or table visits, and whether judges will hold your phone. This decides whether a judge or a teammate runs The Replay. *Default:* three minutes, with the Replay as the middle minute.
4. **Hosting and connectivity.** Can you deploy a static page to a free host, get a QR code working on venue wifi or data, and reach an LLM API? *Default:* static PWA on a free host, local fallback, no API dependency.

Not a question: I will carry B and a gated A into DOC 2 unless you cut A.
