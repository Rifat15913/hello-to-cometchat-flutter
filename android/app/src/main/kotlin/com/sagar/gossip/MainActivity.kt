package com.sagar.gossip

import android.util.Log
import com.cometchat.pro.constants.CometChatConstants
import com.cometchat.pro.core.AppSettings.AppSettingsBuilder
import com.cometchat.pro.core.CometChat
import com.cometchat.pro.exceptions.CometChatException
import com.cometchat.pro.models.Group
import com.cometchat.pro.models.TextMessage
import com.cometchat.pro.models.User
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugins.GeneratedPluginRegistrant
import org.json.JSONObject


class MainActivity : FlutterActivity(), EventChannel.StreamHandler {

    companion object {
        const val TAG = "MainActivity"
        const val CHANNEL = "com.sagar.gossip/initialize"
        const val appID = "24231cc4ced6c04"
        const val region = "US"
        const val STREAM = "com.sagar.gossip/message"
        const val listenerID = "com.rifat.gossip.superhero"
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        GeneratedPluginRegistrant.registerWith(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
                .setMethodCallHandler { call, result ->
                    when (call.method) {
                        "isCometChatInitialized" -> when (val isInitialized = isCometChatInitialized()) {
                            true -> result.success(isInitialized)
                            false -> result.error("FAILED",
                                    "CometChat sdk failed to initialize",
                                    null)
                        }
                        "loginUser" -> loginUser(call.argument("UID")!!,
                                call.argument("API_KEY")!!,
                                result)
                        "sendMessage" -> sendMessage(call.argument("ROOM_ID")!!,
                                call.argument("MESSAGE")!!,
                                result)
                        "joinGroup" -> joinGroup(result,
                                call.argument("GUID")!!)
                        else -> result.notImplemented()
                    }
                }
        EventChannel(flutterEngine.dartExecutor.binaryMessenger, STREAM).setStreamHandler(this)
    }

    override fun onListen(args: Any?, events: EventChannel.EventSink?) {
        receiveMessage(events)
    }

    override fun onCancel(args: Any?) {
        Log.e(TAG, "event onCancel called")
    }

    override fun onDestroy() {
        CometChat.removeMessageListener(listenerID)
        super.onDestroy()
    }

    private fun isCometChatInitialized(): Boolean {
        var isInitializeComplete = false
        val appSettings = AppSettingsBuilder().subscribePresenceForAllUsers().setRegion(region).build()
        CometChat.init(this, appID, appSettings,
                object : CometChat.CallbackListener<String>() {
                    override fun onSuccess(p0: String?) {
                        isInitializeComplete = true
                    }

                    override fun onError(p0: CometChatException?) {
                        isInitializeComplete = false
                    }
                }
        )
        return isInitializeComplete
    }

    private fun loginUser(uid: String, apiKey: String, result: MethodChannel.Result) {
        CometChat.login(uid, apiKey, object : CometChat.CallbackListener<User>() {
            override fun onSuccess(user: User) {
                Log.e(TAG, user.toString())
                val mapData = hashMapOf(
                        "RESULT" to true,
                        "AVATAR" to user.avatar,
                        "LAST_ACTIVE" to user.lastActiveAt,
                        "NAME" to user.name,
                        "ROLE" to user.role,
                        "STATUS" to user.status,
                        "STATUS_MESSAGE" to user.statusMessage,
                )
                val jsonData = JSONObject(mapData as MutableMap<Any?, Any?>)
                result.success(jsonData.toString())
            }

            override fun onError(p0: CometChatException?) {
                Log.e(TAG, p0?.message!!)
                result.error("FAILED", "Unable to create login", null)
            }
        })
    }

    private fun sendMessage(receiverID: String, message: String, result: MethodChannel.Result) {
        val messageType: String = CometChatConstants.MESSAGE_TYPE_TEXT
        val receiverType: String = CometChatConstants.RECEIVER_TYPE_GROUP

        val textMessage = TextMessage(receiverID, message, receiverType)

        CometChat.sendMessage(textMessage, object : CometChat.CallbackListener<TextMessage>() {
            override fun onSuccess(p0: TextMessage?) {
                Log.d(TAG, "Message sent successfully: " + p0?.toString())
                result.success(true)
            }

            override fun onError(p0: CometChatException?) {
                Log.d(TAG, "Message sending failed with exception: " + p0?.message)
                result.error("FAILED", p0?.message, null)
            }

        })
    }

    private fun joinGroup(result: MethodChannel.Result, guid: String) {
        val GUID = guid
        val groupType: String = CometChatConstants.GROUP_TYPE_PUBLIC
        val password = ""

        CometChat.joinGroup(GUID, groupType, password, object : CometChat.CallbackListener<Group>() {
            override fun onSuccess(p0: Group?) {
                Log.d(TAG, p0.toString())
                result.success(true)
            }

            override fun onError(p0: CometChatException?) {
                Log.d(TAG, "Group joining failed with exception: " + p0?.message)
//        result.error("FAILED",p0?.message,null)
            }
        })
    }

    private fun receiveMessage(result: EventChannel.EventSink?) {
        CometChat.addMessageListener(listenerID, object : CometChat.MessageListener() {
            override fun onTextMessageReceived(p0: TextMessage?) {
                Log.d(TAG, "Message received successfully: ${p0?.text} sender: ${p0?.sender?.uid} receiver: ${p0?.receiverUid}")
                val data = hashMapOf("Message" to p0?.text, "SenderUID" to p0?.sender?.uid)
                val jsonObject = JSONObject(data as MutableMap<Any?, Any?>)
                result?.success(jsonObject.toString())
            }
        })
    }
}
