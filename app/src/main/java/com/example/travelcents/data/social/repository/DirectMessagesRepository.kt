package com.example.travelcents.data.social.repository

import android.util.Log
import com.example.travelcents.data.social.model.DirectChatPreview
import com.example.travelcents.data.social.model.Message
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class DirectMessagesRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
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
                        "members" to listOf(myUid, theirUid),
                        "lastMessage" to "",
                        "lastMessageTime" to Timestamp.now(),
                        "lastSenderId" to "",
                        "lastSenderName" to ""
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
        val nameCache = mutableMapOf<String, String>()
        val photoCache = mutableMapOf<String, String>()
        val previewMap = mutableMapOf<String, DirectChatPreview>()

        fun publish() {
            onUpdate(previewMap.values.sortedByDescending { it.lastMessageTime })
        }

        return db.collection("directChats")
            .whereArrayContains("members", myUid)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                Log.d("DirectChats", "snapshot fired, docs: ${snapshot?.documents?.size}, error: $error")
                if (error != null || snapshot == null) return@addSnapshotListener

                if (snapshot.isEmpty) {
                    previewMap.clear()
                    publish()
                    return@addSnapshotListener
                }

                val currentIds = snapshot.documents.map { it.id }.toSet()
                previewMap.keys.retainAll(currentIds)

                snapshot.documents.forEach { doc ->
                    val members = (doc.get("members") as? List<*>)?.filterIsInstance<String>() ?: return@forEach
                    val otherUid = members.firstOrNull { it != myUid } ?: myUid
                    val lastMsg = doc.getString("lastMessage") ?: ""
                    val lastTime = doc.getTimestamp("lastMessageTime")
                    val lastSenderId = doc.getString("lastSenderId") ?: ""
                    val lastSenderName = doc.getString("lastSenderName") ?: ""

                    previewMap[doc.id] = DirectChatPreview(
                        id = doc.id,
                        otherUid = otherUid,
                        otherUserName = nameCache[otherUid] ?: "Loading...",
                        lastMessage = lastMsg,
                        lastMessageTime = lastTime,
                        lastSenderId = lastSenderId,
                        lastSenderName = lastSenderName,
                        otherPhotoUrl = photoCache[otherUid] ?: ""
                    )
                }
                publish()

                val uidsToFetch = snapshot.documents.mapNotNull { doc ->
                    val members = (doc.get("members") as? List<*>)?.filterIsInstance<String>()
                    members?.firstOrNull { it != myUid } ?: members?.firstOrNull()
                }.distinct().filter { it !in nameCache }

                if (uidsToFetch.isNotEmpty()) {
                    uidsToFetch.chunked(30).forEach { batch ->
                        db.collection("users")
                            .whereIn(FieldPath.documentId(), batch)
                            .get()
                            .addOnSuccessListener { userDocs ->
                                userDocs.forEach { userDoc ->
                                    nameCache[userDoc.id] = userDoc.displayName()
                                    photoCache[userDoc.id] = userDoc.getString("profileImageUrl") ?: ""
                                }
                                // Second pass to update names after fetch
                                snapshot.documents.forEach { doc ->
                                    val members = (doc.get("members") as? List<*>)?.filterIsInstance<String>() ?: return@forEach
                                    val otherUid = members.firstOrNull { it != myUid } ?: myUid
                                    
                                    previewMap[doc.id] = DirectChatPreview(
                                        id = doc.id,
                                        otherUid = otherUid,
                                        otherUserName = nameCache[otherUid] ?: "Unknown User",
                                        lastMessage = doc.getString("lastMessage") ?: "",
                                        lastMessageTime = doc.getTimestamp("lastMessageTime"),
                                        lastSenderId = doc.getString("lastSenderId") ?: "",
                                        lastSenderName = doc.getString("lastSenderName") ?: "",
                                        otherPhotoUrl = photoCache[otherUid] ?: ""
                                    )
                                }
                                publish()
                            }
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
            "text" to text,
            "senderId" to senderId,
            "senderName" to senderName,
            "messageType" to "text",
            "timestamp" to FieldValue.serverTimestamp()
        )
        val chatRef = db.collection("directChats").document(chatId)
        db.runBatch { batch ->
            batch.set(chatRef.collection("messages").document(), message)
            batch.update(
                chatRef,
                mapOf(
                    "lastMessage" to text,
                    "lastMessageTime" to FieldValue.serverTimestamp(),
                    "lastSenderId" to senderId,
                    "lastSenderName" to senderName
                )
            )
        }
    }

    fun deleteDirectChat(myUid: String, theirUid: String, onComplete: () -> Unit = {}) {
        db.collection("directChats")
            .whereArrayContains("members", myUid)
            .get()
            .addOnSuccessListener { snapshot ->
                val chatDoc = snapshot.documents.firstOrNull { doc ->
                    val members = doc.get("members") as? List<*>
                    members != null && members.contains(theirUid)
                } ?: run {
                    onComplete()
                    return@addOnSuccessListener
                }

                val chatRef = db.collection("directChats").document(chatDoc.id)
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

    private fun DocumentSnapshot.displayName(): String {
        val first = getString("firstName") ?: ""
        val last = getString("lastName") ?: ""
        val full = "$first $last".trim()
        if (full.isNotEmpty()) return full

        return getString("name") ?: getString("displayName") ?: getString("username") ?: "Unknown User"
    }
}
