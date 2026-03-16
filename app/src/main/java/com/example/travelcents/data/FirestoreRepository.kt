package com.example.travelcents.data

import com.example.travelcents.ui.main.chats.Friend
import com.example.travelcents.ui.main.chats.Group
import com.example.travelcents.ui.main.chats.Message
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()

    // User
    fun fetchUser(uid: String, onResult: (String) -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val first = doc.getString("firstName") ?: ""
                val last = doc.getString("lastName") ?: ""
                onResult("$first $last".trim().ifBlank { "Me" })
            }
    }

    fun fetchFriends(currentUid: String, onResult: (List<Friend>) -> Unit) {
        db.collection("users")
            .document(currentUid)
            .collection("friends")
            .whereEqualTo("status", "accepted")
            .get()
            .addOnSuccessListener { snapshot ->
                val friendUids = snapshot.documents.map { it.id }
                if (friendUids.isEmpty()) { onResult(emptyList()); return@addOnSuccessListener }

                db.collection("users")
                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), friendUids)
                    .get()
                    .addOnSuccessListener { userSnapshot ->
                        val friends = userSnapshot.documents.mapNotNull { doc ->
                            val first = doc.getString("firstName") ?: ""
                            val last = doc.getString("lastName") ?: ""
                            Friend(
                                uid = doc.id,
                                displayName = "$first $last".trim().ifBlank { "Unknown" },
                                email = doc.getString("email") ?: ""
                            )
                        }
                        onResult(friends)
                    }
            }
    }

    // Groups
    fun listenToGroups(
        uid: String,
        onUpdate: (List<Group>) -> Unit
    ): ListenerRegistration {
        return db.collection("groups")
            .whereArrayContains("members", uid)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
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
        onSuccess: (String) -> Unit,
        onFailure: () -> Unit
    ) {
        val data = hashMapOf(
            "name" to name,
            "members" to members,
            "lastMessage" to "",
            "lastMessageTime" to Timestamp.now(),
            "groupImageUrl" to destinationEmoji
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

    // Messages
    fun listenToMessages(
        groupId: String,
        onUpdate: (List<Message>) -> Unit
    ): ListenerRegistration {
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

    fun sendMessage(
        groupId: String,
        text: String,
        senderId: String,
        senderName: String
    ) {
        val message = hashMapOf(
            "text" to text,
            "senderId" to senderId,
            "senderName" to senderName,
            "timestamp" to FieldValue.serverTimestamp()
        )
        val groupRef = db.collection("groups").document(groupId)
        db.runBatch { batch ->
            batch.set(groupRef.collection("messages").document(), message)
            batch.update(groupRef, mapOf(
                "lastMessage" to text,
                "lastMessageTime" to FieldValue.serverTimestamp()
            ))
        }
    }
}