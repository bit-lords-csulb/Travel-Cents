package com.example.travelcents.data.trip

enum class TripAccessRole(val wireValue: String) {
    OWNER("owner"),
    EDITOR("editor"),
    VIEWER("viewer");

    fun canMutateEvents(): Boolean {
        return this == OWNER || this == EDITOR
    }

    fun canManageTrip(): Boolean {
        return this == OWNER
    }

    companion object {
        fun fromWireValue(value: String?): TripAccessRole {
            return entries.firstOrNull { role ->
                role.wireValue.equals(value, ignoreCase = true)
            } ?: VIEWER
        }
    }
}

data class TripAccessMetadata(
    val memberUids: List<String>,
    val roleByUid: Map<String, String>
)

fun mergeTripAccessMetadata(
    ownerUid: String,
    existingMemberUids: List<String>,
    existingRoleByUid: Map<String, String>,
    additionalMemberUids: List<String>,
    defaultRole: TripAccessRole = TripAccessRole.VIEWER
): TripAccessMetadata {
    val normalizedMembers = buildList {
        add(ownerUid)
        addAll(existingMemberUids)
        addAll(additionalMemberUids)
    }
        .filter { it.isNotBlank() }
        .distinct()

    val normalizedRoles = existingRoleByUid
        .filterKeys { it.isNotBlank() }
        .toMutableMap()
        .apply {
            put(ownerUid, TripAccessRole.OWNER.wireValue)
            normalizedMembers.forEach { memberUid ->
                if (memberUid == ownerUid) return@forEach

                val existingRole = TripAccessRole.fromWireValue(get(memberUid))
                if (existingRole == TripAccessRole.VIEWER && defaultRole == TripAccessRole.EDITOR) {
                    put(memberUid, defaultRole.wireValue)
                } else if (get(memberUid).isNullOrBlank()) {
                    put(memberUid, defaultRole.wireValue)
                }
            }
        }

    return TripAccessMetadata(
        memberUids = normalizedMembers,
        roleByUid = normalizedRoles.toMap()
    )
}
