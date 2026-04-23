package com.example.travelcents.data.trip

import android.os.SystemClock
import android.util.Log
import java.util.concurrent.atomic.AtomicInteger

object TripPerformanceLogger {
    private const val TAG = "TripPerformance"
    private const val METRIC_TRIP_QUERY = "trip_query"
    private const val METRIC_EVENT_QUERY = "event_query"
    private const val METRIC_OPTION_QUERY = "option_query"
    private const val METRIC_YELP_ATTEMPT = "yelp_enrichment_attempt"
    private const val METRIC_HOME_FIRST_RENDER = "home_first_render"

    private data class TripSession(
        val id: Int,
        val trigger: String,
        val requestedTripId: String?,
        val startedAtMs: Long,
        var resolvedTripId: String? = null,
        var tripQueries: Int = 0,
        var eventQueries: Int = 0,
        var optionQueries: Int = 0,
        var yelpAttempts: Int = 0,
        var firstRenderLogged: Boolean = false
    )

    private data class HomeSession(
        val id: Int,
        val trigger: String,
        val viewerUid: String?,
        val startedAtMs: Long,
        var tripQueries: Int = 0,
        var firstRenderLogged: Boolean = false
    )

    private val nextSessionId = AtomicInteger(1)
    private val totalTripQueries = AtomicInteger()
    private val totalEventQueries = AtomicInteger()
    private val totalOptionQueries = AtomicInteger()
    private val totalYelpAttempts = AtomicInteger()

    @Volatile
    private var activeTripSession: TripSession? = null

    @Volatile
    private var activeHomeSession: HomeSession? = null

    @Volatile
    private var appStartedAtMs: Long? = null

    fun markAppStart() {
        if (appStartedAtMs == null) {
            appStartedAtMs = SystemClock.elapsedRealtime()
        }
    }

    @Synchronized
    fun beginTripLoad(trigger: String, requestedTripId: String?) {
        activeTripSession = TripSession(
            id = nextSessionId.getAndIncrement(),
            trigger = trigger,
            requestedTripId = requestedTripId,
            startedAtMs = SystemClock.elapsedRealtime()
        )
        Log.d(
            TAG,
            "trip_session_started id=${activeTripSession?.id} trigger=$trigger requestedTripId=${requestedTripId.orEmpty()}"
        )
    }

    @Synchronized
    fun bindTrip(tripId: String) {
        activeTripSession?.resolvedTripId = tripId
        Log.d(TAG, "trip_session_bound id=${activeTripSession?.id} tripId=$tripId")
    }

    @Synchronized
    fun beginHomeLoad(trigger: String, viewerUid: String?) {
        activeHomeSession = HomeSession(
            id = nextSessionId.getAndIncrement(),
            trigger = trigger,
            viewerUid = viewerUid,
            startedAtMs = SystemClock.elapsedRealtime()
        )
        Log.d(
            TAG,
            "home_session_started id=${activeHomeSession?.id} trigger=$trigger viewerUid=${viewerUid.orEmpty()}"
        )
    }

    @Synchronized
    fun recordHomeFirstRender(source: String, tripCount: Int) {
        val session = activeHomeSession ?: return
        if (session.firstRenderLogged) return

        session.firstRenderLogged = true
        val now = SystemClock.elapsedRealtime()
        val coldStartDurationMs = appStartedAtMs?.let { now - it }

        Log.d(
            TAG,
            "$METRIC_HOME_FIRST_RENDER id=${session.id} source=$source trigger=${session.trigger} " +
                "viewerUid=${session.viewerUid.orEmpty()} tripCount=$tripCount durationMs=${now - session.startedAtMs} " +
                "coldStartDurationMs=${coldStartDurationMs ?: -1} tripQueries=${session.tripQueries}"
        )

        activeHomeSession = null
    }

    fun recordTripQuery(source: String, detail: String) {
        recordMetric(
            metric = METRIC_TRIP_QUERY,
            totalCounter = totalTripQueries,
            source = source,
            detail = detail
        )
    }

    fun recordEventQuery(source: String, detail: String) {
        recordMetric(
            metric = METRIC_EVENT_QUERY,
            totalCounter = totalEventQueries,
            source = source,
            detail = detail
        )
    }

    fun recordOptionQuery(source: String, detail: String) {
        recordMetric(
            metric = METRIC_OPTION_QUERY,
            totalCounter = totalOptionQueries,
            source = source,
            detail = detail
        )
    }

    fun recordYelpEnrichmentAttempt(source: String, detail: String) {
        recordMetric(
            metric = METRIC_YELP_ATTEMPT,
            totalCounter = totalYelpAttempts,
            source = source,
            detail = detail
        )
    }

    @Synchronized
    fun recordListenerAttached(source: String, tripId: String) {
        Log.d(
            TAG,
            "listener_attached tripSession=${activeTripSession?.id ?: -1} source=$source tripId=$tripId"
        )
    }

    @Synchronized
    fun recordListenerDetached(source: String, tripId: String?) {
        Log.d(
            TAG,
            "listener_detached tripSession=${activeTripSession?.id ?: -1} source=$source tripId=${tripId.orEmpty()}"
        )
    }

    @Synchronized
    fun recordFirstRender(source: String, tripId: String?, eventCount: Int) {
        val session = activeTripSession ?: return
        if (session.firstRenderLogged) return

        session.firstRenderLogged = true
        session.resolvedTripId = tripId ?: session.resolvedTripId
        val now = SystemClock.elapsedRealtime()
        val coldStartDurationMs = appStartedAtMs?.let { now - it }

        Log.d(
            TAG,
            "first_render id=${session.id} source=$source trigger=${session.trigger} " +
                "tripId=${session.resolvedTripId.orEmpty()} requestedTripId=${session.requestedTripId.orEmpty()} " +
                "eventCount=$eventCount durationMs=${now - session.startedAtMs} " +
                "coldStartDurationMs=${coldStartDurationMs ?: -1} " +
                "tripQueries=${session.tripQueries} eventQueries=${session.eventQueries} " +
                "optionQueries=${session.optionQueries} yelpAttempts=${session.yelpAttempts}"
        )
    }

    private fun recordMetric(
        metric: String,
        totalCounter: AtomicInteger,
        source: String,
        detail: String
    ) {
        val totalCount = totalCounter.incrementAndGet()
        val (tripSessionId, tripSessionCount) = incrementTripSessionCounter(metric)
        val (homeSessionId, homeSessionCount) = incrementHomeSessionCounter(metric)
        Log.d(
            TAG,
            "$metric total=$totalCount tripSession=${tripSessionId ?: -1} tripSessionCount=$tripSessionCount " +
                "homeSession=${homeSessionId ?: -1} homeSessionCount=$homeSessionCount source=$source detail=$detail"
        )
    }

    @Synchronized
    private fun incrementTripSessionCounter(metric: String): Pair<Int?, Int> {
        val session = activeTripSession ?: return null to 0
        val sessionCount = when (metric) {
            METRIC_TRIP_QUERY -> {
                session.tripQueries += 1
                session.tripQueries
            }
            METRIC_EVENT_QUERY -> {
                session.eventQueries += 1
                session.eventQueries
            }
            METRIC_OPTION_QUERY -> {
                session.optionQueries += 1
                session.optionQueries
            }
            METRIC_YELP_ATTEMPT -> {
                session.yelpAttempts += 1
                session.yelpAttempts
            }
            else -> 0
        }
        return session.id to sessionCount
    }

    @Synchronized
    private fun incrementHomeSessionCounter(metric: String): Pair<Int?, Int> {
        val session = activeHomeSession ?: return null to 0
        if (metric != METRIC_TRIP_QUERY) return session.id to 0

        session.tripQueries += 1
        return session.id to session.tripQueries
    }
}
