package com.example.travelcents.data.trip

import org.junit.Assert.assertEquals
import org.junit.Test

class TripAccessMetadataTest {

    @Test
    fun mergeTripAccessMetadata_preservesOwnerAndAddsNewViewers() {
        val merged = mergeTripAccessMetadata(
            ownerUid = "owner-1",
            existingMemberUids = listOf("owner-1", "viewer-1"),
            existingRoleByUid = mapOf(
                "owner-1" to TripAccessRole.OWNER.wireValue,
                "viewer-1" to TripAccessRole.EDITOR.wireValue
            ),
            additionalMemberUids = listOf("viewer-1", "viewer-2")
        )

        assertEquals(listOf("owner-1", "viewer-1", "viewer-2"), merged.memberUids)
        assertEquals(TripAccessRole.OWNER.wireValue, merged.roleByUid["owner-1"])
        assertEquals(TripAccessRole.EDITOR.wireValue, merged.roleByUid["viewer-1"])
        assertEquals(TripAccessRole.VIEWER.wireValue, merged.roleByUid["viewer-2"])
    }

    @Test
    fun mergeTripAccessMetadata_filtersBlankMembers() {
        val merged = mergeTripAccessMetadata(
            ownerUid = "owner-1",
            existingMemberUids = emptyList(),
            existingRoleByUid = emptyMap(),
            additionalMemberUids = listOf("", "viewer-1", " ")
        )

        assertEquals(listOf("owner-1", "viewer-1"), merged.memberUids)
        assertEquals(
            mapOf(
                "owner-1" to TripAccessRole.OWNER.wireValue,
                "viewer-1" to TripAccessRole.VIEWER.wireValue
            ),
            merged.roleByUid
        )
    }

    @Test
    fun mergeTripAccessMetadata_upgradesViewerToEditorWhenRequested() {
        val merged = mergeTripAccessMetadata(
            ownerUid = "owner-1",
            existingMemberUids = listOf("owner-1", "editor-1"),
            existingRoleByUid = mapOf(
                "owner-1" to TripAccessRole.OWNER.wireValue,
                "editor-1" to TripAccessRole.VIEWER.wireValue
            ),
            additionalMemberUids = listOf("editor-1"),
            defaultRole = TripAccessRole.EDITOR
        )

        assertEquals(TripAccessRole.EDITOR.wireValue, merged.roleByUid["editor-1"])
    }
}
