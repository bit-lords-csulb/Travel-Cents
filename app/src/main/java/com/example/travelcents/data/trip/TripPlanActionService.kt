package com.example.travelcents.data.trip

import com.example.travelcents.data.sync.TripSyncRemoteDataSource
import com.example.travelcents.data.trip.model.EventOption
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.withSelectedOption

data class TripPlanActionResult(
    val event: TravelEvent? = null,
    val options: List<EventOption> = emptyList(),
    val confirmationMessage: String
)

interface TripPlanActionRemoteGateway {
    suspend fun upsertEvent(
        tripKey: TripKey,
        event: TravelEvent
    )

    suspend fun deleteEvent(
        tripKey: TripKey,
        eventId: String
    )

    suspend fun persistEventAndOptions(
        tripKey: TripKey,
        eventId: String,
        event: TravelEvent,
        options: List<EventOption>,
        updatedOptionIds: Set<String>? = null
    )
}

class TripPlanActionRemoteDataSource(
    private val remoteDataSource: TripSyncRemoteDataSource = TripSyncRemoteDataSource()
) : TripPlanActionRemoteGateway {
    override suspend fun upsertEvent(
        tripKey: TripKey,
        event: TravelEvent
    ) {
        remoteDataSource.upsertEvent(tripKey = tripKey, event = event)
    }

    override suspend fun deleteEvent(
        tripKey: TripKey,
        eventId: String
    ) {
        remoteDataSource.deleteEvent(tripKey = tripKey, eventId = eventId)
    }

    override suspend fun persistEventAndOptions(
        tripKey: TripKey,
        eventId: String,
        event: TravelEvent,
        options: List<EventOption>,
        updatedOptionIds: Set<String>?
    ) {
        remoteDataSource.persistEventAndOptions(
            tripKey = tripKey,
            eventId = eventId,
            event = event,
            options = options,
            updatedOptionIds = updatedOptionIds
        )
    }
}

class TripPlanActionService(
    private val remoteGateway: TripPlanActionRemoteGateway = TripPlanActionRemoteDataSource()
) {
    suspend fun addEvent(
        tripKey: TripKey,
        event: TravelEvent
    ): TripPlanActionResult {
        remoteGateway.upsertEvent(tripKey = tripKey, event = event)
        return TripPlanActionResult(
            event = event,
            confirmationMessage = "Plan added to your trip."
        )
    }

    suspend fun updateEvent(
        tripKey: TripKey,
        event: TravelEvent
    ): TripPlanActionResult {
        remoteGateway.upsertEvent(tripKey = tripKey, event = event)
        return TripPlanActionResult(
            event = event,
            confirmationMessage = "Plan updated."
        )
    }

    suspend fun deleteEvent(
        tripKey: TripKey,
        eventId: String
    ): TripPlanActionResult {
        remoteGateway.deleteEvent(tripKey = tripKey, eventId = eventId)
        return TripPlanActionResult(
            confirmationMessage = "Plan deleted from your trip."
        )
    }

    suspend fun replaceSelectedOption(
        tripKey: TripKey,
        event: TravelEvent,
        existingOptions: List<EventOption>,
        optionId: String,
        persistOptions: Boolean
    ): TripPlanActionResult {
        val selectedOption = existingOptions.firstOrNull { option -> option.optionId == optionId }
            ?: throw IllegalArgumentException("Option '$optionId' was not found for event '${event.eventId}'.")

        val updatedOptions = existingOptions.map { option ->
            option.copy(selected = option.optionId == optionId)
                .scopedTo(
                    ownerUid = tripKey.ownerUid,
                    tripId = tripKey.tripId,
                    eventId = event.eventId
                )
        }
        val updatedEvent = event.withSelectedOption(
            updatedOptions.first { option -> option.optionId == optionId }
        )

        if (persistOptions) {
            remoteGateway.persistEventAndOptions(
                tripKey = tripKey,
                eventId = event.eventId,
                event = updatedEvent,
                options = updatedOptions
            )
        } else {
            remoteGateway.upsertEvent(tripKey = tripKey, event = updatedEvent)
        }

        return TripPlanActionResult(
            event = updatedEvent,
            options = updatedOptions,
            confirmationMessage = "Updated this trip option."
        )
    }

    suspend fun saveOption(
        tripKey: TripKey,
        event: TravelEvent,
        existingOptions: List<EventOption>,
        option: EventOption
    ): TripPlanActionResult {
        val savedOption = option.copy(selected = false)
            .scopedTo(
                ownerUid = tripKey.ownerUid,
                tripId = tripKey.tripId,
                eventId = event.eventId
            )
        val mergedOptions = (existingOptions.filterNot { existing ->
            existing.optionId == savedOption.optionId
        } + savedOption).distinctBy(EventOption::optionId)

        remoteGateway.persistEventAndOptions(
            tripKey = tripKey,
            eventId = event.eventId,
            event = event,
            options = mergedOptions,
            updatedOptionIds = setOf(savedOption.optionId)
        )

        return TripPlanActionResult(
            event = event,
            options = mergedOptions,
            confirmationMessage = "Saved as an option for this slot."
        )
    }
}
