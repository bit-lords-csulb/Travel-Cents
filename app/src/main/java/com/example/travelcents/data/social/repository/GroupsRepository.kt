package com.example.travelcents.data.social.repository

import android.util.Log
import com.example.travelcents.data.social.model.Group
import com.example.travelcents.data.social.model.Message
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

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

    fun createGroup(
        name: String,
        members: List<String>,
        destinationEmoji: String,
        linkedTripId: String = "",
        linkedTripOwnerId: String = "",
        onSuccess: (String) -> Unit,
        onFailure: () -> Unit
    ) {
        val data = hashMapOf(
            "name" to name,
            "members" to members,
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

    fun fetchGroup(groupId: String, onResult: (Group?) -> Unit) {
        db.collection("groups").document(groupId).get()
            .addOnSuccessListener { doc ->
                onResult(doc.toObject(Group::class.java)?.copy(id = doc.id))
            }
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

    fun sendMessage(groupId: String, text: String, senderId: String, senderName: String) {
        val message = hashMapOf(
            "text" to text,
            "senderId" to senderId,
            "senderName" to senderName,
            "timestamp" to FieldValue.serverTimestamp()
        )
        val groupRef = db.collection("groups").document(groupId)
        db.runBatch { batch ->
            batch.set(groupRef.collection("messages").document(), message)
            batch.update(
                groupRef,
                mapOf(
                    "lastMessage" to text,
                    "lastMessageTime" to FieldValue.serverTimestamp()
                )
            )
        }
    }
}
