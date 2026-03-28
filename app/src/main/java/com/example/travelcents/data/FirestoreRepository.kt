package com.example.travelcents.data

import com.example.travelcents.data.model.DirectChatPreview
import com.example.travelcents.ui.main.chats.friends.Friend
import com.example.travelcents.data.model.Group
import com.example.travelcents.data.model.Message
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldPath
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
                val last  = doc.getString("lastName")  ?: ""
                onResult("$first $last".trim().ifBlank { "Me" })
            }
    }

    // Friends
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

    // Groups
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
        linkedTripId: String = "",
        linkedTripOwnerId: String = "",
        onSuccess: (String) -> Unit,
        onFailure: () -> Unit
    ) {
        val data = hashMapOf(
            "name"              to name,
            "members"           to members,
            "lastMessage"       to "",
            "lastMessageTime"   to Timestamp.now(),
            "groupImageUrl"     to destinationEmoji,
            "linkedTripId"      to linkedTripId,
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

    // Group Messages
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

    // Direct Messages
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
    fun listenToDirectChatPreviews(
        myUid: String,
        onUpdate: (List<DirectChatPreview>) -> Unit
    ): ListenerRegistration {
        // name cache so we don't re-fetch on every snapshot
        val nameCache = mutableMapOf<String, String>()
        // previews keyed by chatId so each update replaces the right entry
        val previewMap = mutableMapOf<String, DirectChatPreview>()

        fun publish() {
            onUpdate(previewMap.values.sortedByDescending { it.lastMessageTime })
        }

        return db.collection("directChats")
            .whereArrayContains("members", myUid)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                android.util.Log.d("DirectChats", "snapshot fired, docs: ${snapshot?.documents?.size}, error: $error")
                if (error != null || snapshot == null) return@addSnapshotListener

                if (snapshot.isEmpty) { previewMap.clear(); publish(); return@addSnapshotListener }

                snapshot.documents.forEach { doc ->
                    val members  = (doc.get("members") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    val otherUid = members.firstOrNull { it != myUid } ?: return@forEach
                    val lastMsg  = doc.getString("lastMessage") ?: ""
                    val lastTime = doc.getTimestamp("lastMessageTime")
                    val chatId   = doc.id

                    val cachedName = nameCache[otherUid]
                    if (cachedName != null) {
                        // Already have the name — update preview immediately
                        previewMap[chatId] = DirectChatPreview(chatId, otherUid, cachedName, lastMsg, lastTime)
                        publish()
                    } else {
                        // Fetch name then update
                        db.collection("users").document(otherUid).get()
                            .addOnSuccessListener { userDoc ->
                                val first = userDoc.getString("firstName") ?: ""
                                val last  = userDoc.getString("lastName")  ?: ""
                                val name  = "$first $last".trim().ifBlank { "Unknown" }
                                nameCache[otherUid] = name
                                previewMap[chatId] = DirectChatPreview(chatId, otherUid, name, lastMsg, lastTime)
                                publish()
                            }
                    }
                }

                // Remove any chats that were deleted
                val currentIds = snapshot.documents.map { it.id }.toSet()
                previewMap.keys.retainAll(currentIds)
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

    // Delete a direct chat and all its messages
    fun deleteDirectChat(myUid: String, theirUid: String, onComplete: () -> Unit = {}) {
        db.collection("directChats")
            .whereArrayContains("members", myUid)
            .get()
            .addOnSuccessListener { snapshot ->
                val chatDoc = snapshot.documents.firstOrNull { doc ->
                    val members = doc.get("members") as? List<*>
                    members != null && members.contains(theirUid)
                } ?: run { onComplete(); return@addOnSuccessListener }

                val chatRef = db.collection("directChats").document(chatDoc.id)

                // Delete all messages first then the chat doc
                chatRef.collection("messages").get()
                    .addOnSuccessListener { messages ->
                        val batch = db.batch()
                        messages.documents.forEach { batch.delete(it.reference) }
                        batch.delete(chatRef)
                        batch.commit().addOnSuccessListener { onComplete() }
                    }
                    .addOnFailureListener { onComplete() }
            }
    }
}