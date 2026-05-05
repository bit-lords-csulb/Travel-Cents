package com.example.travelcents.data.social.repository

import android.net.Uri
import android.util.Log
import com.android.identity.util.UUID
import com.example.travelcents.data.social.model.Group
import com.example.travelcents.data.social.model.Message
import com.example.travelcents.ui.main.chats.chat.TripPreview
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class GroupsRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun listenToGroups(uid: String, onUpdate: (List<Group>) -> Unit): ListenerRegistration {
        return db.collection("groups")
            .whereArrayContains("members", uid)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("GroupsRepo", "listenToGroups failed for uid=$uid", error)
                    onUpdate(emptyList())
                    return@addSnapshotListener
                }
                val groups = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Group::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                onUpdate(groups)
            }
    }

    fun observeGroup(groupId: String): Flow<Group?> = callbackFlow {
        val listenerRegistration = db.collection("groups").document(groupId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val group = snapshot?.toObject(Group::class.java)
                trySend(group)
            }
        awaitClose { listenerRegistration.remove() }
    }

    fun observeGroups(): Flow<List<Group>> = callbackFlow {
        val listenerRegistration = db.collection("groups")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val groups = snapshot.toObjects(Group::class.java)
                    trySend(groups)
                }
            }
        awaitClose { listenerRegistration.remove() }
    }


    fun createGroup(
        name: String,
        members: List<String>,
        ownerId: String,
        destinationEmoji: String,
        linkedTripId: String = "",
        linkedTripOwnerId: String = "",
        onSuccess: (String) -> Unit,
        onFailure: () -> Unit
    ) {
        val data = hashMapOf(
            "name" to name,
            "members" to members,
            "ownerId" to ownerId,
            "lastMessage" to "",
            "lastMessageTime" to Timestamp.now(),
            "groupImageUrl" to destinationEmoji,
            "linkedTripId" to linkedTripId,
            "linkedTripOwnerId" to linkedTripOwnerId
        )
        db.collection("groups").add(data)
            .addOnSuccessListener { onSuccess(it.id) }
            .addOnFailureListener { onFailure() }
    }
    fun leaveGroup(groupId: String, userId: String, onComplete: () -> Unit) {
        val groupRef = db.collection("groups").document(groupId)

        groupRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                onComplete()
                return@addOnSuccessListener
            }

            val members = doc.get("members") as? List<*> ?: emptyList<Any>()

            // If this is the last person, delete the group and messages
            if (members.size <= 1 && members.contains(userId)) {
                deleteGroup(groupId, onComplete)
            } else {
                // Just remove the specific user from the members array
                groupRef.update("members", FieldValue.arrayRemove(userId))
                    .addOnSuccessListener { onComplete() }
                    .addOnFailureListener { onComplete() }
            }
        }.addOnFailureListener { onComplete() }
    }

    // Helper function to clean up the group and its messages
    private fun deleteGroup(groupId: String, onComplete: () -> Unit) {
        val groupRef = db.collection("groups").document(groupId)

        // 1. Delete all messages in the sub-collection first
        groupRef.collection("messages").get().addOnSuccessListener { messages ->
            val batch = db.batch()
            messages.documents.forEach { batch.delete(it.reference) }

            // 2. Delete the group document itself
            batch.delete(groupRef)

            batch.commit().addOnSuccessListener { onComplete() }
        }.addOnFailureListener { onComplete() }
    }

    fun fetchGroup(groupId: String, onResult: (Group?) -> Unit) {
        db.collection("groups").document(groupId).get()
            .addOnSuccessListener { doc ->
                onResult(doc.toObject(Group::class.java)?.copy(id = doc.id))
            }
            .addOnFailureListener { onResult(null) }
    }

    fun findGroupByTripId(tripId: String, onResult: (Group?) -> Unit) {
        db.collection("groups")
            .whereEqualTo("linkedTripId", tripId)
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->
                val doc = snap.documents.firstOrNull()
                onResult(doc?.toObject(Group::class.java)?.copy(id = doc.id))
            }
            .addOnFailureListener { onResult(null) }
    }

    fun listenToMessages(groupId: String, onUpdate: (List<Message>) -> Unit): ListenerRegistration {
        return db.collection("groups")
            .document(groupId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                onUpdate(messages)
            }
    }

    /// Trips / Itineraries
    fun fetchUserTrips(uid: String, onResult: (List<TripPreview>) -> Unit) {
        db.collection("users")
            .document(uid)
            .collection("trips")
            .get()
            .addOnSuccessListener { snap ->
                val trips = snap.documents.mapNotNull { doc ->
                    val name = doc.getString("tripName")
                        ?: doc.getString("title")
                        ?: doc.getString("destination")
                        ?: "Unnamed Trip"

                    TripPreview(doc.id, name, uid)
                }
                onResult(trips)
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    // Update Group Details
    fun updateGroup(
        groupId: String,
        updates: Map<String, Any>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("groups").document(groupId)
            .update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    // Upload Group Image to Firebase Storage
    fun uploadGroupImage(uri: Uri, groupId: String, onResult: (String?) -> Unit) {
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("group_images/$groupId/${UUID.randomUUID()}")

        imageRef.putFile(uri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    onResult(downloadUri.toString())
                }.addOnFailureListener { onResult(null) }
            }
            .addOnFailureListener { onResult(null) }
    }

    fun sendMessage(groupId: String, text: String, senderId: String, senderName: String) {
        val message = hashMapOf(
            "text" to text,
            "senderId" to senderId,
            "senderName" to senderName,
            "messageType" to "text",
            "timestamp" to FieldValue.serverTimestamp()
        )
        val groupRef = db.collection("groups").document(groupId)
        db.runBatch { batch ->
            batch.set(groupRef.collection("messages").document(), message)
            batch.update(
                groupRef,
                mapOf(
                    "lastMessage" to text,
                    "lastMessageTime" to FieldValue.serverTimestamp(),
                    "lastSenderId" to senderId,
                    "lastSenderName" to senderName
                )
            )
        }
    }
}
