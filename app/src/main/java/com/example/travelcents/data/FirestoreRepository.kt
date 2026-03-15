package com.example.travelcents.data

import com.example.travelcents.ui.main.chats.chat.DirectChatPreview
import com.example.travelcents.ui.main.chats.friends.Friend
import com.example.travelcents.ui.main.chats.chat.Group
import com.example.travelcents.ui.main.chats.chat.Message
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore

class FirestoreRepository {

    private val db = Firebase.firestore

    // ── User ─────────────────────────────────────────────────────────────────

    fun fetchUser(uid: String, onResult: (String) -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val first = doc.getString("firstName") ?: ""
                val last  = doc.getString("lastName")  ?: ""
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
                            val last  = doc.getString("lastName")  ?: ""
                            Friend(
                                uid             = doc.id,
                                displayName     = "$first $last".trim().ifBlank { "Unknown" },
                                email           = doc.getString("email") ?: "",
                                profileImageUrl = doc.getString("profileImageUrl") ?: "",
                                isOnline        = doc.getBoolean("isOnline") ?: false,
                                lastSeenLabel   = "Offline"
                            )
                        }
                        onResult(friends)
                    }
            }
    }

    // ── Groups ────────────────────────────────────────────────────────────────

    fun listenToGroups(uid: String, onUpdate: (List<Group>) -> Unit): ListenerRegistration {
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
            "name"            to name,
            "members"         to members,
            "lastMessage"     to "",
            "lastMessageTime" to Timestamp.now(),
            "groupImageUrl"   to destinationEmoji
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

    // ── Group Messages ────────────────────────────────────────────────────────

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
            "text"       to text,
            "senderId"   to senderId,
            "senderName" to senderName,
            "timestamp"  to FieldValue.serverTimestamp()
        )
        val groupRef = db.collection("groups").document(groupId)
        db.runBatch { batch ->
            batch.set(groupRef.collection("messages").document(), message)
            batch.update(groupRef, mapOf(
                "lastMessage"     to text,
                "lastMessageTime" to FieldValue.serverTimestamp()
            ))
        }
    }

    // ── Direct Chats ──────────────────────────────────────────────────────────

    fun getOrCreateDirectChat(myUid: String, theirUid: String, onResult: (String) -> Unit) {
        db.collection("directChats")
            .whereArrayContains("members", myUid)
            .get()
            .addOnSuccessListener { snapshot ->
                val existing = snapshot.documents.firstOrNull { doc ->
                    val members = doc.get("members") as? List<*>
                    members != null && members.contains(theirUid)
                }
                if (existing != null) {
                    onResult(existing.id)
                } else {
                    val data = hashMapOf(
                        "members"         to listOf(myUid, theirUid),
                        "lastMessage"     to "",
                        "lastMessageTime" to Timestamp.now()
                    )
                    db.collection("directChats").add(data)
                        .addOnSuccessListener { onResult(it.id) }
                }
            }
    }

    // Listen to direct chats and resolve the other user's name for preview
    fun listenToDirectChatPreviews(
        myUid: String,
        onUpdate: (List<DirectChatPreview>) -> Unit
    ): ListenerRegistration {
        return db.collection("directChats")
            .whereArrayContains("members", myUid)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val docs = snapshot.documents
                if (docs.isEmpty()) { onUpdate(emptyList()); return@addSnapshotListener }

                val previews = mutableListOf<DirectChatPreview>()
                var remaining = docs.size

                docs.forEach { doc ->
                    val members    = (doc.get("members") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    val otherUid   = members.firstOrNull { it != myUid } ?: ""
                    val lastMsg    = doc.getString("lastMessage") ?: ""
                    val lastTime   = doc.getTimestamp("lastMessageTime")
                    val chatId     = doc.id

                    if (otherUid.isEmpty()) {
                        remaining--
                        if (remaining == 0) onUpdate(previews.sortedByDescending { it.lastMessageTime })
                        return@forEach
                    }

                    // Fetch other user's name
                    db.collection("users").document(otherUid).get()
                        .addOnSuccessListener { userDoc ->
                            val first = userDoc.getString("firstName") ?: ""
                            val last  = userDoc.getString("lastName")  ?: ""
                            previews.add(
                                DirectChatPreview(
                                    id              = chatId,
                                    otherUid        = otherUid,
                                    otherUserName   = "$first $last".trim().ifBlank { "Unknown" },
                                    lastMessage     = lastMsg,
                                    lastMessageTime = lastTime
                                )
                            )
                            remaining--
                            if (remaining == 0) onUpdate(previews.sortedByDescending { it.lastMessageTime })
                        }
                        .addOnFailureListener {
                            remaining--
                            if (remaining == 0) onUpdate(previews.sortedByDescending { it.lastMessageTime })
                        }
                }
            }
    }

    fun listenToDirectMessages(chatId: String, onUpdate: (List<Message>) -> Unit): ListenerRegistration {
        return db.collection("directChats")
            .document(chatId)
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

    fun sendDirectMessage(chatId: String, text: String, senderId: String, senderName: String) {
        val message = hashMapOf(
            "text"       to text,
            "senderId"   to senderId,
            "senderName" to senderName,
            "timestamp"  to FieldValue.serverTimestamp()
        )
        val chatRef = db.collection("directChats").document(chatId)
        db.runBatch { batch ->
            batch.set(chatRef.collection("messages").document(), message)
            batch.update(chatRef, mapOf(
                "lastMessage"     to text,
                "lastMessageTime" to FieldValue.serverTimestamp()
            ))
        }
    }
}