# Trip Generation Pipeline Enhancement Plan

## Scope

This audit is based on:

- the current Android generation pipeline in `NewTripViewModel`, `GroqRepository`, `SerpRepository`, and `YelpRepository`
- the successful debug run captured in `debug_pipeline_runs/20260402_132757/`

Primary goals:

- identify what is already optimized
- identify what certainly needs improvement
- determine what data can be generated locally instead of remotely
- propose ways to speed up trip generation and the debug/reporting workflow

## Executive Summary

The current pipeline is partly optimized, but it is not stable for longer trips.

What is working:

- Groq is only used once, for itinerary metadata and IATA extraction.
- Serp flight and hotel search already run in parallel.
- Yelp detail calls are deferred until card expansion.
- Serp flight and hotel results are cached.

What is clearly broken or inefficient:

- Yelp fan-out is too aggressive and hits rate limits on long trips.
- day-by-day Yelp primaries repeat the same restaurant and activity across multiple days.
- most Yelp day calls fail silently and are dropped.
- the flight parser is reading the wrong Serp fields for departure/arrival times.
- hotel image downloading is the dominant latency cost.
- Groq is currently used for data that is mostly deterministic and local.

## Evidence From The Latest Successful Debug Run

Run analyzed:

- `debug_pipeline_runs/20260402_132757/`

Observed facts:

- Total outbound API requests: 36
- Groq requests: 1
- Serp requests: 2
- Yelp requests: 33
- Trip length: 16 days
- Restaurant day calls attempted: 16
- Activity day calls attempted: 16
- Yelp events calls attempted: 1
- Restaurant days with usable output: 5
- Activity days with usable output: 5
- Yelp events usable output: 0
- Hotel image download attempts: 310
- Hotel image downloads completed: 308
- Hotel image stage time: about 117 seconds
- Total observed timeline span: about 127 seconds

Hard failures and quality issues:

- many Yelp restaurant calls returned HTTP 429
- many Yelp activity calls returned HTTP 429
- Yelp events returned HTTP 403 in this run
- the same restaurant primary was selected across multiple days
- the same activity primary was selected across multiple days
- Serp flight parsing produced empty departure and arrival time fields despite a valid flight result

## What Is Already Optimized

### 1. Groq usage is already minimal

The app no longer uses Groq to generate day-level restaurants and activities during trip creation. That removes one expensive and potentially hallucinatory stage.

This is good because:

- it limits hallucination risk
- it reduces model latency and token cost
- it narrows Groq’s responsibility to metadata normalization

### 2. Serp flight and hotel requests are parallelized

Flights and hotels start together. That is the correct shape for this stage because they do not depend on each other.

### 3. Yelp business detail and review fetches are lazy

The detailed Yelp fetches are not in the critical generation path. That is the right tradeoff.

### 4. Serp results are cached

`SerpCache` already avoids re-fetching the same flight and hotel search repeatedly. This is one of the better pieces of the pipeline.

### 5. Image downloading deduplicates URLs

The image cache layer already avoids downloading exact duplicate URLs more than once.

## What Certainly Needs Improvement

## A. Reliability

### 1. Yelp request strategy is not viable for long trips

Current strategy:

- 1 Yelp restaurant search per day
- 1 Yelp activity search per day
- 1 Yelp events search per trip

For a 16-day trip that becomes 33 Yelp requests in a short burst. The debug run shows that this routinely produces HTTP 429 responses.

Impact:

- many days are missing restaurants
- many days are missing activities
- the pipeline gives the appearance of success while silently degrading output quality

Priority:

- highest

Required change:

- stop doing per-day Yelp searches
- fetch pools once per trip or once per neighborhood cluster, then distribute locally

### 2. API failures are swallowed too aggressively

The Android repositories mostly return `null` or `emptyList()` on failure. That keeps the app alive, but it hides root cause and allows incomplete trips to look valid.

Impact:

- missing data is hard to distinguish from legitimate “no results”
- debugging depends on external tools instead of in-app diagnostics

Required change:

- preserve structured error state per stage
- store failure reason, HTTP code, and affected dates
- surface degraded generation to debug builds and optionally to the user

## B. Data Quality

### 3. Yelp primary assignment is wrong

The app currently picks `response.businesses.first()` for each day independently. If Yelp returns the same top business each time, the app repeats it across the trip.

This is confirmed by the report:

- one restaurant primary repeated across multiple days
- one activity primary repeated across multiple days

Required change:

- fetch a pool
- deduplicate by business id
- assign primaries round-robin without reuse
- keep reused items only as alternatives if the pool is exhausted

### 4. Flight time parsing is broken

The live Serp response shape places times under:

- `departure_airport.time`
- `arrival_airport.time`

Current code expects:

- `departureTime`
- `arrivalTime`

That mismatch explains why the debug report showed empty departure and arrival times even though the flight itself was valid.

Required change:

- update the model mapping to the actual response shape
- add parser tests against captured Serp payloads

### 5. The selected hotel strategy is too naive

The pipeline sorts by highest rated and then takes the first hotel within budget. In the debug run, that produced a low-end Harlem property for a 16-day comfort trip to New York.

Issues:

- no neighborhood preference
- no minimum hotel quality floor for `comfort` or `luxury`
- no ranking that balances review count, hotel class, location, and rate

Required change:

- replace “first result wins” with a local ranking function
- use travel style to enforce minimum acceptable hotel class and review volume

## C. Latency

### 6. Hotel image download is dominating total runtime

The report shows:

- about 117 seconds spent in the hotel image stage
- about 127 seconds total pipeline span

This means hotel image downloading is the main bottleneck.

Root causes:

- downloading both thumbnails and originals for all hotel options
- downloading many photos for options that may never be viewed
- sequential download behavior in `ImageCacheManager`

Required change:

- do not download all hotel images during generation
- only prefetch a small hero image set for selected options
- defer gallery images until expansion
- parallelize downloads with a small bounded concurrency if prefetch remains necessary

### 7. Yelp over-parallelization is self-defeating

Launching many Yelp calls at once looks fast on paper, but it causes rate limits and degraded output.

Required change:

- reduce request count first
- then add bounded concurrency
- do not burst 30+ Yelp requests for a single trip

## What Can Be Generated Locally

## 1. Trip metadata except IATA lookup

These do not need Groq:

- `itinerary_id`
- `user_id`
- `date_from`
- `date_to`
- `duration_days`
- `currency`
- `travel_style`
- `travelers`
- `created_at`
- `status`

These are fully deterministic from local input.

### Recommendation

- build the itinerary object locally
- reserve remote work only for fields that truly require inference

## 2. Trip title can be generated locally

Current example:

- `New York Getaway`

That is not worth a model call.

Local patterns are enough:

- `<Destination> Trip`
- `<Destination> Getaway`
- `<Destination> Family Trip`
- `<Destination> Weekend`

### Recommendation

- generate a deterministic local title first
- optionally let the user rename it later

## 3. IATA lookup should ideally be local or semi-local

The only Groq output that currently appears materially useful is:

- `origin_iata`
- `destination_iata`

Even that should not come from an LLM long-term.

Better alternatives:

- local airport database for top cities and metros
- bundled city-to-airport mapping for common destinations
- deterministic geocoding/aviation lookup service if local coverage is insufficient

### Recommendation

- replace Groq IATA inference with local lookup plus metro fallback
- keep Groq out of the critical path entirely if possible

## 4. Event times and slot structure can be local

Current Yelp event creation hardcodes:

- restaurants: `19:00-21:00`
- activities: `10:00-12:00`

That means the pipeline is already not using remote intelligence for scheduling.

### Recommendation

- generate day slots locally
- fill those slots from Yelp/Serp pools
- only use an LLM later if you want optional “polish” suggestions

## 5. Budget slicing is already local and should stay local

The budget split logic is local today, but the result is not driving downstream selection in a meaningful way.

### Recommendation

- keep budget logic local
- use it directly in ranking and filtering instead of passing it to a model

## Information That May Not Be Needed At All

### 1. Full hotel photo galleries at generation time

Not needed in the critical path.

Keep:

- one hero image per selected hotel

Defer:

- all alternative images
- original-size gallery images

### 2. Yelp Events trip-wide call

This run produced no usable data and cost one more request in an already rate-limited stage.

### Recommendation

- make Yelp Events optional
- trigger it only for cities and dates where it historically adds value
- or move it behind a “load more local happenings” action

### 3. Groq itinerary metadata call

If IATA lookup and naming move local, the whole Groq call becomes optional or removable.

## Speedup Recommendations

## Highest-impact generation speedups

### 1. Replace per-day Yelp searches with per-trip pools

Current:

- 33 Yelp requests for a 16-day trip

Recommended:

- 1 to 3 restaurant pool calls
- 1 to 3 activity pool calls
- optional 0 to 1 Yelp events calls

Expected outcome:

- far fewer 429s
- more complete trips
- lower latency
- easier deduplication

### 2. Stop downloading every hotel image up front

Recommended preload set:

- selected hotel thumbnail
- selected hotel original hero image
- maybe first image for the top 3 alternatives

Everything else should be lazy.

Expected outcome:

- removes the 117-second bottleneck

### 3. Bound image download concurrency

If image prefetch remains:

- use a small parallel pool, such as 4 to 8 downloads at a time
- keep cancellation support

Current sequential downloading is too slow.

### 4. Remove Groq from the critical path if local IATA lookup is added

Expected outcome:

- saves roughly 0.5 to 1 second on the happy path
- removes one external dependency
- eliminates one category of hallucination

## Debug report speedups

### 1. Separate summary from raw payloads

Current markdown includes large raw JSON blocks inline. That makes the report heavy.

Recommended structure:

- `summary.md` for high-level findings
- `requests/` and `responses/` JSON files per call
- `index.json` for machine-readable metadata

Expected outcome:

- faster report generation
- easier manual inspection
- easier git diffing or artifact upload

### 2. Add report modes

Suggested modes:

- `--summary-only`
- `--include-raw`
- `--skip-images`
- `--skip-hotel-images`
- `--skip-yelp-events`

This lets debugging target the exact stage under investigation.

### 3. Capture compact derived metrics automatically

The report should compute:

- success rate by stage
- calls by provider
- days with missing restaurants
- days with missing activities
- duplicate primary ids
- image stage time

This prevents manual comb-through every run.

## Recommended Future Pipeline Shape

## Phase 1: Build deterministic trip shell locally

Generate locally:

- itinerary id
- title
- duration
- date list
- default event slots

Resolve locally or semi-locally:

- IATA / metro airport ids

## Phase 2: Fetch provider pools, not day-by-day events

Fetch:

- flights once
- hotels once
- restaurant pool once per trip or per area
- activity pool once per trip or per area

## Phase 3: Assemble the trip locally

Locally:

- deduplicate businesses
- assign primary recommendations without repeats
- attach alternatives
- enforce budget
- enforce travel style quality floor

## Phase 4: Defer expensive enrichments

Lazy:

- Yelp detail and reviews
- hotel gallery downloads
- optional local events enrichment

## Priority Fix List

### Immediate

- fix Serp flight time parsing
- replace day-by-day Yelp fan-out with pooled search
- add non-silent failure tracking for missing days
- stop downloading all hotel images at generation time

### Short term

- local ranking for hotel selection
- local deduped distribution of Yelp pools across days
- optional skip of Yelp Events
- report modes and summary metrics

### Medium term

- replace Groq itinerary metadata with deterministic local generation
- move IATA inference to a local or deterministic lookup source
- add parser tests from captured debug payloads

## Bottom Line

The current pipeline is optimized in the wrong places.

It already avoids a second Groq call and parallelizes independent stages, which is good. But it still spends most of its time on non-essential image downloads and wastes Yelp quota on a day-by-day request strategy that collapses under rate limiting.

The most important architectural shift is:

- stop generating the trip by querying Yelp per day
- fetch provider pools once
- build the itinerary locally and deterministically

That change will improve speed, completeness, debuggability, and consistency at the same time.
