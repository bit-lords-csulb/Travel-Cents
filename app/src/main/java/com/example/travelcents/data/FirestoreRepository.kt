package com.example.travelcents.data

import com.example.travelcents.ui.main.chats.Friend
import com.example.travelcents.ui.main.chats.Group
import com.example.travelcents.ui.main.chats.Message
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore

class FirestoreRepository {

    private val db = Firebase.firestore

    // User
    fun fetchUser(uid: String, onResult: (String) -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val first = doc.getString("firstName") ?: ""
                val last = doc.getString("lastName") ?: ""
                onResult("$first $last".trim().ifBlank { "Me" })
            }
    }

    // ── Friends ───────────────────────────────────────────────────────────────

    fun searchUsersByUsername(query: String, excludeUid: String, onResult: (List<Friend>) -> Unit) {
        db.collection("users")
            .whereEqualTo("username", query.trim().lowercase())
            .get()
            .addOnSuccessListener { snap ->
                onResult(snap.documents.filter { it.id != excludeUid }.mapNotNull { doc ->
                    val first = doc.getString("firstName") ?: ""
                    val last = doc.getString("lastName") ?: ""
                    Friend(uid = doc.id, displayName = "$first $last".trim().ifBlank { "Unknown" }, email = doc.getString("email") ?: "")
                })
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    fun searchUsersByEmail(query: String, excludeUid: String, onResult: (List<Friend>) -> Unit) {
        db.collection("users")
            .whereEqualTo("email", query.trim().lowercase())
            .get()
            .addOnSuccessListener { snap ->
                onResult(snap.documents.filter { it.id != excludeUid }.mapNotNull { doc ->
                    val first = doc.getString("firstName") ?: ""
                    val last = doc.getString("lastName") ?: ""
                    Friend(uid = doc.id, displayName = "$first $last".trim().ifBlank { "Unknown" }, email = doc.getString("email") ?: "")
                })
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    fun searchUsersByName(query: String, excludeUid: String, onResult: (List<Friend>) -> Unit) {
        val trimmed = query.trim().lowercase()
        val endStr = trimmed.replaceRange(trimmed.length - 1, trimmed.length, (trimmed.last() + 1).toString())
        db.collection("users")
            .whereGreaterThanOrEqualTo("firstName", trimmed)
            .whereLessThan("firstName", endStr)
            .get()
            .addOnSuccessListener { snap ->
                onResult(snap.documents.filter { it.id != excludeUid }.mapNotNull { doc ->
                    val first = doc.getString("firstName") ?: ""
                    val last = doc.getString("lastName") ?: ""
                    Friend(uid = doc.id, displayName = "$first $last".trim().ifBlank { "Unknown" }, email = doc.getString("email") ?: "")
                })
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    fun listenToSentRequests(currentUid: String, onUpdate: (Set<String>) -> Unit): ListenerRegistration {
        return db.collection("users").document(currentUid).collection("friends")
            .whereEqualTo("direction", "sent")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snap, _ ->
                onUpdate(snap?.documents?.map { it.id }?.toSet() ?: emptySet())
            }
    }

    fun listenToPendingReceivedRequests(currentUid: String, onUpdate: (List<Friend>) -> Unit): ListenerRegistration {
        return db.collection("users").document(currentUid).collection("friends")
            .whereEqualTo("status", "pending")
            .whereEqualTo("direction", "received")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                val senderUids = snapshot.documents.map { it.id }
                if (senderUids.isEmpty()) { onUpdate(emptyList()); return@addSnapshotListener }
                db.collection("users")
                    .whereIn(FieldPath.documentId(), senderUids)
                    .get()
                    .addOnSuccessListener { userSnap ->
                        onUpdate(userSnap.documents.mapNotNull { doc ->
                            val first = doc.getString("firstName") ?: ""
                            val last = doc.getString("lastName") ?: ""
                            Friend(uid = doc.id, displayName = "$first $last".trim().ifBlank { "Unknown" }, email = doc.getString("email") ?: "")
                        })
                    }
            }
    }

    fun removeFriend(myUid: String, theirUid: String) {
        val batch = db.batch()
        batch.delete(db.collection("users").document(myUid).collection("friends").document(theirUid))
        batch.delete(db.collection("users").document(theirUid).collection("friends").document(myUid))
        batch.commit()
    }

    fun sendFriendRequest(myUid: String, theirUid: String, onSuccess: () -> Unit) {
        val batch = db.batch()
        batch.set(db.collection("users").document(myUid).collection("friends").document(theirUid),
            mapOf("status" to "pending", "direction" to "sent"))
        batch.set(db.collection("users").document(theirUid).collection("friends").document(myUid),
            mapOf("status" to "pending", "direction" to "received"))
        batch.commit().addOnSuccessListener { onSuccess() }
    }

    fun cancelFriendRequest(myUid: String, theirUid: String, onSuccess: () -> Unit) {
        val batch = db.batch()
        batch.delete(db.collection("users").document(myUid).collection("friends").document(theirUid))
        batch.delete(db.collection("users").document(theirUid).collection("friends").document(myUid))
        batch.commit().addOnSuccessListener { onSuccess() }
    }

    fun acceptFriendRequest(myUid: String, senderUid: String) {
        val batch = db.batch()
        batch.update(db.collection("users").document(myUid).collection("friends").document(senderUid),
            mapOf("status" to "accepted"))
        batch.update(db.collection("users").document(senderUid).collection("friends").document(myUid),
            mapOf("status" to "accepted"))
        batch.commit()
    }

    fun declineFriendRequest(myUid: String, senderUid: String) {
        val batch = db.batch()
        batch.delete(db.collection("users").document(myUid).collection("friends").document(senderUid))
        batch.delete(db.collection("users").document(senderUid).collection("friends").document(myUid))
        batch.commit()
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