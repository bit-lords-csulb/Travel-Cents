package com.example.travelcents.data.social.repository

import com.example.travelcents.data.social.model.Friend
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class FriendsRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun searchUsersByUsername(query: String, excludeUid: String, onResult: (List<Friend>) -> Unit) {
        db.collection("users")
            .whereEqualTo("username", query.trim().lowercase())
            .get()
            .addOnSuccessListener { snap ->
                onResult(snap.documents.filter { it.id != excludeUid }.map { it.toFriend() })
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    fun searchUsersByEmail(query: String, excludeUid: String, onResult: (List<Friend>) -> Unit) {
        db.collection("users")
            .whereEqualTo("email", query.trim().lowercase())
            .get()
            .addOnSuccessListener { snap ->
                onResult(snap.documents.filter { it.id != excludeUid }.map { it.toFriend() })
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
                onResult(snap.documents.filter { it.id != excludeUid }.map { it.toFriend() })
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
                if (senderUids.isEmpty()) {
                    onUpdate(emptyList())
                    return@addSnapshotListener
                }
                db.collection("users")
                    .whereIn(FieldPath.documentId(), senderUids)
                    .get()
                    .addOnSuccessListener { userSnap ->
                        onUpdate(userSnap.documents.map { it.toFriend() })
                    }
            }
    }

    fun removeFriend(myUid: String, theirUid: String, onSuccess: () -> Unit = {}, onFailure: () -> Unit = {}) {
        val batch = db.batch()
        batch.delete(db.collection("users").document(myUid).collection("friends").document(theirUid))
        batch.delete(db.collection("users").document(theirUid).collection("friends").document(myUid))
        batch.commit()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure() }
    }

    fun sendFriendRequest(myUid: String, theirUid: String, onSuccess: () -> Unit) {
        val batch = db.batch()
        batch.set(
            db.collection("users").document(myUid).collection("friends").document(theirUid),
            mapOf("status" to "pending", "direction" to "sent")
        )
        batch.set(
            db.collection("users").document(theirUid).collection("friends").document(myUid),
            mapOf("status" to "pending", "direction" to "received")
        )
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
        batch.update(
            db.collection("users").document(myUid).collection("friends").document(senderUid),
            mapOf("status" to "accepted")
        )
        batch.update(
            db.collection("users").document(senderUid).collection("friends").document(myUid),
            mapOf("status" to "accepted")
        )
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
                if (friendUids.isEmpty()) {
                    onResult(emptyList())
                    return@addOnSuccessListener
                }
                db.collection("users")
                    .whereIn(FieldPath.documentId(), friendUids)
                    .get()
                    .addOnSuccessListener { userSnapshot ->
                        onResult(userSnapshot.documents.map { it.toFriend() })
                    }
            }
    }
}
