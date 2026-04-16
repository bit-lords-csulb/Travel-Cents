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

    private data class Session(
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

    private val nextSessionId = AtomicInteger(1)
    private val totalTripQueries = AtomicInteger()
    private val totalEventQueries = AtomicInteger()
    private val totalOptionQueries = AtomicInteger()
    private val totalYelpAttempts = AtomicInteger()

    @Volatile
    private var activeSession: Session? = null

    @Synchronized
    fun beginTripLoad(trigger: String, requestedTripId: String?) {
        activeSession = Session(
            id = nextSessionId.getAndIncrement(),
            trigger = trigger,
            requestedTripId = requestedTripId,
            startedAtMs = SystemClock.elapsedRealtime()
        )
        Log.d(
            TAG,
            "session_started id=${activeSession?.id} trigger=$trigger requestedTripId=${requestedTripId.orEmpty()}"
        )
    }

    @Synchronized
    fun bindTrip(tripId: String) {
        activeSession?.resolvedTripId = tripId
        Log.d(TAG, "session_bound id=${activeSession?.id} tripId=$tripId")
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
        Log.d(TAG, "listener_attached session=${activeSession?.id ?: -1} source=$source tripId=$tripId")
    }

    @Synchronized
    fun recordListenerDetached(source: String, tripId: String?) {
        Log.d(
            TAG,
            "listener_detached session=${activeSession?.id ?: -1} source=$source tripId=${tripId.orEmpty()}"
        )
    }

    @Synchronized
    fun recordFirstRender(source: String, tripId: String?, eventCount: Int) {
        val session = activeSession ?: return
        if (session.firstRenderLogged) return

        session.firstRenderLogged = true
        session.resolvedTripId = tripId ?: session.resolvedTripId

        Log.d(
            TAG,
            "first_render id=${session.id} source=$source trigger=${session.trigger} " +
                "tripId=${session.resolvedTripId.orEmpty()} requestedTripId=${session.requestedTripId.orEmpty()} " +
                "eventCount=$eventCount durationMs=${SystemClock.elapsedRealtime() - session.startedAtMs} " +
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
        val (sessionId, sessionCount) = incrementSessionCounter(metric)
        Log.d(
            TAG,
            "$metric total=$totalCount session=${sessionId ?: -1} sessionCount=$sessionCount source=$source detail=$detail"
        )
    }

    @Synchronized
    private fun incrementSessionCounter(metric: String): Pair<Int?, Int> {
        val session = activeSession ?: return null to 0
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
}
