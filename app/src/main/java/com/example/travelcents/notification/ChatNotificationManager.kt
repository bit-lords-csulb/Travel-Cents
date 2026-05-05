package com.example.travelcents.notification

import android.content.Context
import android.util.Log
import com.example.travelcents.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class ChatNotificationManager private constructor(@Suppress("UNUSED_PARAMETER") context: Context) {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val notificationHelper = NotificationHelper.getInstance()
    private var groupsListener: ListenerRegistration? = null
    private var directListener: ListenerRegistration? = null
    private var authListenerRegistered = false
    private var sessionStartedAtMs = System.currentTimeMillis()
    private val seenMessageTimeByChat = mutableMapOf<String, Long>()

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        debugLog {
            "auth state changed currentUid=${firebaseAuth.currentUser?.uid ?: "<none>"}"
        }
        if (firebaseAuth.currentUser != null) {
            startListeningInternal()
        } else {
            stopListening()
        }
    }

    companion object {
        private const val TAG = "ChatNotificationManager"

        private var instance: ChatNotificationManager? = null

        fun initialize(context: Context) {
            if (instance == null) {
                instance = ChatNotificationManager(context.applicationContext)
            }
        }

        fun getInstance(): ChatNotificationManager {
            return instance ?: throw IllegalStateException("ChatNotificationManager must be initialized")
        }
    }

    private fun debugLog(message: () -> String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message())
        }
    }

    fun startListening() {
        debugLog {
            "startListening called authListenerRegistered=$authListenerRegistered " +
                "currentUid=${auth.currentUser?.uid ?: "<none>"}"
        }
        if (!authListenerRegistered) {
            auth.addAuthStateListener(authListener)
            authListenerRegistered = true
        }
        if (auth.currentUser != null) {
            startListeningInternal()
        }
    }

    private fun startListeningInternal() {
        if (groupsListener != null || directListener != null) {
            debugLog {
                "listeners already active groupsListener=${groupsListener != null} " +
                    "directListener=${directListener != null}"
            }
            return
        }

        val currentUid = auth.currentUser?.uid ?: run {
            debugLog { "not starting listeners: no authenticated user" }
            return
        }

        // Give a 60-second buffer to make manual Firestore testing easier
        sessionStartedAtMs = System.currentTimeMillis() - 60000
        seenMessageTimeByChat.clear()
        debugLog {
            "starting listeners currentUid=$currentUid sessionStartedAtMs=$sessionStartedAtMs"
        }

        groupsListener = db.collection("groups")
            .whereArrayContains("members", currentUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Group chat notification listener failed", error)
                    return@addSnapshotListener
                }
                val changes = snapshot?.documentChanges.orEmpty()
                debugLog {
                    "group snapshot changes=${changes.size} docs=${snapshot?.size() ?: 0} " +
                        "fromCache=${snapshot?.metadata?.isFromCache ?: "unknown"} " +
                        "pendingWrites=${snapshot?.metadata?.hasPendingWrites() ?: "unknown"}"
                }
                processChanges(
                    changes = changes,
                    currentUid = currentUid,
                    chatType = ChatNotificationTarget.TYPE_GROUP
                )
            }

        directListener = db.collection("directChats")
            .whereArrayContains("members", currentUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Direct chat notification listener failed", error)
                    return@addSnapshotListener
                }
                val changes = snapshot?.documentChanges.orEmpty()
                debugLog {
                    "direct snapshot changes=${changes.size} docs=${snapshot?.size() ?: 0} " +
                        "fromCache=${snapshot?.metadata?.isFromCache ?: "unknown"} " +
                        "pendingWrites=${snapshot?.metadata?.hasPendingWrites() ?: "unknown"}"
                }
                processChanges(
                    changes = changes,
                    currentUid = currentUid,
                    chatType = ChatNotificationTarget.TYPE_DIRECT
                )
            }
    }

    private fun processChanges(
        changes: List<DocumentChange>,
        currentUid: String,
        chatType: String
    ) {
        debugLog {
            "processing $chatType changes=${changes.size} currentUid=$currentUid " +
                "sessionStartedAtMs=$sessionStartedAtMs activeTarget=${NotificationHelper.activeChatTarget}"
        }
        changes.forEach { change ->
            val doc = change.document
            val target = ChatNotificationTarget(chatType = chatType, chatId = doc.id)
            val key = target.notificationKey

            if (change.type != DocumentChange.Type.ADDED &&
                change.type != DocumentChange.Type.MODIFIED
            ) {
                debugLog {
                    "skip $key: unsupported changeType=${change.type}"
                }
                return@forEach
            }

            val lastMessageTime = doc.getTimestamp("lastMessageTime")
            if (lastMessageTime == null) {
                debugLog {
                    "skip $key: missing lastMessageTime changeType=${change.type} " +
                        "fields=${doc.data?.keys?.sorted().orEmpty()}"
                }
                return@forEach
            }

            val lastMessageTimeMs = lastMessageTime.toDate().time
            val previousTimeMs = seenMessageTimeByChat[key]
            val lastSenderId = doc.getString("lastSenderId")
            val lastSenderName = doc.getString("lastSenderName")

            debugLog {
                "change $key type=${change.type} lastMessageTimeMs=$lastMessageTimeMs " +
                    "previousTimeMs=${previousTimeMs ?: "<none>"} " +
                    "sessionStartedAtMs=$sessionStartedAtMs currentUid=$currentUid " +
                    "lastSenderId=${lastSenderId ?: "<missing>"} " +
                    "lastSenderName=${lastSenderName ?: "<missing>"} " +
                    "activeTarget=${NotificationHelper.activeChatTarget}"
            }

            if (lastMessageTimeMs <= sessionStartedAtMs && previousTimeMs == null) {
                seenMessageTimeByChat[key] = maxOf(previousTimeMs ?: 0L, lastMessageTimeMs)
                debugLog {
                    "skip $key: initial snapshot is not newer than listener start " +
                        "lastMessageTimeMs=$lastMessageTimeMs sessionStartedAtMs=$sessionStartedAtMs"
                }
                return@forEach
            }

            if (previousTimeMs != null && lastMessageTimeMs <= previousTimeMs) {
                debugLog {
                    "skip $key: lastMessageTime is not newer than previous seen time " +
                        "lastMessageTimeMs=$lastMessageTimeMs previousTimeMs=$previousTimeMs"
                }
                return@forEach
            }

            if (lastSenderId.isNullOrBlank()) {
                seenMessageTimeByChat[key] = maxOf(previousTimeMs ?: 0L, lastMessageTimeMs)
                debugLog {
                    "fallback $key: parent missing lastSenderId; querying latest message"
                }
                showNotificationFromLatestMessage(
                    target = target,
                    parentDoc = doc,
                    chatType = chatType,
                    currentUid = currentUid,
                    parentLastMessageTimeMs = lastMessageTimeMs,
                    previousTimeMs = previousTimeMs
                )
                return@forEach
            }

            if (lastSenderId == currentUid) {
                seenMessageTimeByChat[key] = maxOf(previousTimeMs ?: 0L, lastMessageTimeMs)
                debugLog {
                    "skip $key: lastSenderId matches current user currentUid=$currentUid"
                }
                return@forEach
            }

            val senderName = lastSenderName
                ?.takeIf { it.isNotBlank() }
                ?: "New message"
            val body = doc.getString("lastMessage")
                ?.takeIf { it.isNotBlank() }
                ?: "New message"
            val title = if (chatType == ChatNotificationTarget.TYPE_GROUP) {
                val groupName = doc.getString("name")?.takeIf { it.isNotBlank() }
                if (groupName != null) "$senderName in $groupName" else senderName
            } else {
                senderName
            }

            seenMessageTimeByChat[key] = maxOf(previousTimeMs ?: 0L, lastMessageTimeMs)
            debugLog {
                "posting notification for $key title=$title lastSenderId=$lastSenderId"
            }
            notificationHelper.showChatNotification(
                target = target,
                title = title,
                body = body
            )
        }
    }

    private fun showNotificationFromLatestMessage(
        target: ChatNotificationTarget,
        parentDoc: DocumentSnapshot,
        chatType: String,
        currentUid: String,
        parentLastMessageTimeMs: Long,
        previousTimeMs: Long?
    ) {
        val key = target.notificationKey
        val collection = when (chatType) {
            ChatNotificationTarget.TYPE_GROUP -> "groups"
            ChatNotificationTarget.TYPE_DIRECT -> "directChats"
            else -> {
                debugLog { "fallback $key: unsupported chatType=$chatType" }
                return
            }
        }

        db.collection(collection)
            .document(target.chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                val messageDoc = snapshot.documents.firstOrNull()
                if (messageDoc == null) {
                    debugLog { "fallback skip $key: no messages found" }
                    return@addOnSuccessListener
                }

                val messageSenderId = messageDoc.getString("senderId")
                val messageSenderName = messageDoc.getString("senderName")
                val messageTimestampMs = messageDoc.getTimestamp("timestamp")?.toDate()?.time
                val candidateTimeMs = messageTimestampMs ?: parentLastMessageTimeMs

                debugLog {
                    "fallback change $key messageId=${messageDoc.id} " +
                        "messageTimestampMs=${messageTimestampMs ?: "<missing>"} " +
                        "parentLastMessageTimeMs=$parentLastMessageTimeMs " +
                        "candidateTimeMs=$candidateTimeMs " +
                        "previousTimeMs=${previousTimeMs ?: "<none>"} " +
                        "currentUid=$currentUid " +
                        "senderId=${messageSenderId ?: "<missing>"} " +
                        "senderName=${messageSenderName ?: "<missing>"}"
                }

                if (candidateTimeMs <= sessionStartedAtMs && previousTimeMs == null) {
                    debugLog {
                        "fallback skip $key: latest message is not newer than listener start " +
                            "candidateTimeMs=$candidateTimeMs sessionStartedAtMs=$sessionStartedAtMs"
                    }
                    return@addOnSuccessListener
                }

                if (previousTimeMs != null && candidateTimeMs <= previousTimeMs) {
                    debugLog {
                        "fallback skip $key: latest message is not newer than previous seen time " +
                            "candidateTimeMs=$candidateTimeMs previousTimeMs=$previousTimeMs"
                    }
                    return@addOnSuccessListener
                }

                if (messageSenderId.isNullOrBlank()) {
                    debugLog { "fallback skip $key: latest message missing senderId" }
                    return@addOnSuccessListener
                }

                if (messageSenderId == currentUid) {
                    debugLog {
                        "fallback skip $key: latest message senderId matches current user currentUid=$currentUid"
                    }
                    return@addOnSuccessListener
                }

                val senderName = messageSenderName
                    ?.takeIf { it.isNotBlank() }
                    ?: "New message"
                val body = messageDoc.getString("text")
                    ?.takeIf { it.isNotBlank() }
                    ?: parentDoc.getString("lastMessage")
                        ?.takeIf { it.isNotBlank() }
                    ?: "New message"
                val title = if (chatType == ChatNotificationTarget.TYPE_GROUP) {
                    val groupName = parentDoc.getString("name")?.takeIf { it.isNotBlank() }
                    if (groupName != null) "$senderName in $groupName" else senderName
                } else {
                    senderName
                }

                seenMessageTimeByChat[key] = maxOf(
                    seenMessageTimeByChat[key] ?: 0L,
                    parentLastMessageTimeMs,
                    candidateTimeMs
                )
                debugLog {
                    "fallback posting notification for $key title=$title senderId=$messageSenderId"
                }
                notificationHelper.showChatNotification(
                    target = target,
                    title = title,
                    body = body
                )
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Fallback latest message lookup failed for $key", error)
            }
    }

    fun stopListening() {
        debugLog {
            "stopListening groupsListener=${groupsListener != null} " +
                "directListener=${directListener != null}"
        }
        groupsListener?.remove()
        directListener?.remove()
        groupsListener = null
        directListener = null
        seenMessageTimeByChat.clear()
    }

}
