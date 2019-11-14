package com.android.server.mqttservice;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;

import com.android.client.mqtt.IClientCallback;
import com.android.server.mqtt.IMqttService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.Listener;
import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.CallbackConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.Message;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;

import java.net.URISyntaxException;
import java.security.KeyStore;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class MQTTService extends Service {
    private static final String TAG = "MQTTService";

    private static MQTT client;

    private Map<String, List<Object>> callbackMap = new HashMap();
    private ConcurrentHashMap<String, List<String>> cachedMessages = new ConcurrentHashMap<>();

    ExecutorService cachedThreadPool;

    Context context;

    private Listener listener;
    private CallbackConnection callbackConnection;
    private Callback<Void> connectCallback;
    private Callback<byte[]> subscribeCallback;
    private Callback<Void> publishCallback;
    private Callback<Void> disconnectCallback;
    private Callback<Void> killCallback;

    private String host;
    private String username;
    private String password;

    final static short  KEEP_ALIVE   = 255;

    private boolean mIsConnect = false;
	private boolean mIsLostConnect = false;

    private String[] SERVER_TOPICS = {MqttTopics.DISPATCH_FILTER_ITEM+MqttUtils.getDeviceID(),
                                         MqttTopics.DISPATCH_FILTER_LIST+MqttUtils.getDeviceID(),
                                         MqttTopics.SIP_ITEM+MqttUtils.getDeviceID(),
                                         MqttTopics.SIP_ITEM_LIST+MqttUtils.getDeviceID(),
                                         MqttTopics.SIP_CONFIG_INFO+MqttUtils.getDeviceID(),
                                         MqttTopics.DISPATCH_OTP+MqttUtils.getDeviceID(),
                                         MqttTopics.UPGRADE_REQUEST+MqttUtils.getDeviceID(),
                                         MqttTopics.DEVICE_CONTROL_REQUEST+MqttUtils.getDeviceID(),
                                         MqttTopics.FACE_ADD+MqttUtils.getDeviceID(),
                                         MqttTopics.FACE_DELETE+MqttUtils.getDeviceID(),
                                         MqttTopics.TIME_CALIBRATE+MqttUtils.getDeviceID(),
										 MqttTopics.DISPATCH_SIP_ITEM+MqttUtils.getDeviceID(),
										 MqttTopics.DISPATCH_AD_URL+MqttUtils.getDeviceID(),
										 MqttTopics.DISPATCH_DOWNLOAD_URL+MqttUtils.getDeviceID(),
										 MqttTopics.DISPATCH_GROUP_CODES+MqttUtils.getDeviceID(),
										 MqttTopics.DISPATCH_READING_HEAD_PROGRAM_URL+MqttUtils.getDeviceID()
    };

    private int[] SERVER_QOS = {MqttConstants.QOS_QUALITY_1,
                                   MqttConstants.QOS_QUALITY_1,
                                   MqttConstants.QOS_QUALITY_1,
                                   MqttConstants.QOS_QUALITY_1,
                                   MqttConstants.QOS_QUALITY_1,
                                   MqttConstants.QOS_QUALITY_1,
                                   MqttConstants.QOS_QUALITY_1,
                                   MqttConstants.QOS_QUALITY_1,
                                   MqttConstants.QOS_QUALITY_1,
								   MqttConstants.QOS_QUALITY_1,
								   MqttConstants.QOS_QUALITY_1,
								   MqttConstants.QOS_QUALITY_1,
								   MqttConstants.QOS_QUALITY_1,
								   MqttConstants.QOS_QUALITY_1,
								   MqttConstants.QOS_QUALITY_1,
                                   MqttConstants.QOS_QUALITY_1};

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                boolean hasNetwork = MqttUtils.isConnectIsNormal();
                Logger.i(TAG, "BroadcastReceiver action=" + intent.getAction()+" hasNetwork="+hasNetwork+" mIsConnect="+mIsConnect);
				Logger.i(TAG, "BroadcastReceiver mIsConnect="+mIsConnect+" mIsLostConnect="+mIsLostConnect);
                if (hasNetwork && callbackConnection != null && !mIsConnect && !mIsLostConnect) {
                    doClientConnection();
                }
            }
        }
    };

    public MQTTService() {
        context = this;
    }

    @Override
    public IBinder onBind(Intent intent) {

        Logger.i(TAG, "intent packagename="+intent.getPackage());
        return  mMqttBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.i(TAG, "onStartCommand");

        if (client != null && !mIsConnect) {
            doClientConnection();
        }

        return Service.START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.i(TAG, "onCreate");
        Logger.i(TAG, "111");

        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        getApplicationContext().registerReceiver(broadcastReceiver, filter);

        cachedThreadPool = Executors.newCachedThreadPool();

        startMqttClient();
    }

    private void startMqttClient() {
        getConfig();
        if (!TextUtils.isEmpty(host)) {
            init();
        }
    }

    private void stopMqttClient() {
        if (client != null) {
            try {
                if (callbackConnection != null) {
                    callbackConnection.disconnect(disconnectCallback);
                    client = null;
                }
            }catch (Exception e) {
            }
        }
    }

    private void getConfig() {
        host = MqttUtils.getHost();
        username = MqttUtils.getUserName();
        password = MqttUtils.getPassword();
    }

    private void init() {
        Logger.i(TAG, "deviceid="+MqttUtils.getDeviceID());
        try {
            client = new MQTT();
            client.setHost(host);
            client.setVersion("3.1.1");
            client.setKeepAlive(KEEP_ALIVE);
            client.setClientId(MqttUtils.getDeviceID());
            client.setCleanSession(false);

            Logger.i(TAG, "host="+host);
            Logger.i(TAG, "username="+username);
            Logger.i(TAG, "password="+password);

            if (host.startsWith("ssl:")) {
                SocketFactory.SocketFactoryOptions socketFactoryOptions = new SocketFactory.SocketFactoryOptions();
                try {
                    socketFactoryOptions.withCaInputStream(getApplicationContext().getResources().openRawResource(R.raw.ca));
                    SocketFactory socketFactory = new SocketFactory(socketFactoryOptions);
                    client.setSslContext(socketFactory.getSSLContext());
                } catch (IOException | NoSuchAlgorithmException | KeyStoreException | CertificateException | KeyManagementException | UnrecoverableKeyException e) {
                    e.printStackTrace();
                }
            }

//            if (host.startsWith("ssl:")) {
//                SSLContext ctx = SSLContext.getInstance("TLSv1.2");
//                KeyManagerFactory kmf = KeyManagerFactory.getInstance("X509");
//                TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
//                KeyStore ks = KeyStore.getInstance("BKS");
//                KeyStore tks = KeyStore.getInstance("BKS");
//                ks.load(getResources().openRawResource(R.raw.tsl), MqttConstants.BKS_PASSWORD.toCharArray());
//                tks.load(getResources().openRawResource(R.raw.tsl), MqttConstants.BKS_PASSWORD.toCharArray());
//                kmf.init(ks, MqttConstants.BKS_PASSWORD.toCharArray());
//                tmf.init(tks);
//                ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
//                client.setSslContext(ctx);
//                Logger.i(TAG, "setSslContext...");
//            }
            if (!TextUtils.isEmpty(username)) {
                client.setUserName(username);
                client.setPassword(password);
            }

            callbackConnection = client.callbackConnection();
            callbackConnection.listener(listener);

        } catch (Exception e) {
            Logger.i(TAG, "init exception="+e.getMessage());
        }

        doClientConnection();
    }

    private void doClientConnection() {
        if (!mIsConnect && MqttUtils.isConnectIsNormal()) {
            try {
                callbackConnection.connect(connectCallback);
            } catch (Exception e) {
                Logger.i(TAG, "doClientConnection exception="+e.getMessage());
            }
        }
    }

    private void publishWithQuality(String topic, int qos, boolean retained, String msg) {
        try {
            if (client != null && mIsConnect) {
                callbackConnection.publish(topic+MqttUtils.getDeviceID(),msg.getBytes(),QoS.AT_LEAST_ONCE,false,publishCallback);
            } else {
                //TODO
            }
        } catch (Exception e) {
            Logger.i(TAG, "MqttException="+e.getMessage());
        }
    }

    private void dispatchCacheMessage(final String topic, final IClientCallback clientCallback) {
        try {
            List<String> messageList = cachedMessages.get(topic);
            Logger.i(TAG, "dispatchCacheMessage topic="+topic+" messageList.size="+messageList.size());

            if (messageList != null) {
                for (final String message : messageList) {
                    Logger.i(TAG, "dispatchCacheMessage topic="+topic+" message="+message);

                    cachedThreadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                clientCallback.dispatch(topic, message);
                            } catch (Exception e) {
                                if ("android.os.DeadObjectException".equals(e.toString())) {
                                    synchronized (context) {
                                        List<Object> objectList = callbackMap.get(topic);
                                        if (objectList.contains(clientCallback)) {
                                            objectList.remove(clientCallback);
                                        }
                                        callbackMap.put(topic, objectList);
                                    }
                                }
                                Logger.i(TAG, "messageArrived dispatchCacheMessage e="+e.toString());
                            }
                        }
                    });

                }
                cachedMessages.remove(topic);
            }
        } catch (Exception e) {
        }
    }

    private void saveCacheMessage(String topic, String content) {
        Logger.i(TAG, "saveCacheMessage topic="+topic+" content="+content);
        for (String servertopic : SERVER_TOPICS) {
            int index = servertopic.lastIndexOf("/");
            String subTopic = servertopic.substring(0, index+1);
            if (subTopic.equals(topic)) {
                List<String> messageList = cachedMessages.get(topic);
                if (messageList == null) {
                    messageList = new ArrayList<String>();
                }
                messageList.add(content);
                cachedMessages.put(topic, messageList);
            }
        }
    }

    private IBinder mMqttBinder = new IMqttService.Stub() {
        @Override
        public void config(String host, String username, String password) throws RemoteException {
            MqttUtils.setMqttConfig(host, username, password);
			mIsLostConnect = false;
			if (mIsConnect) {
				stopMqttClient();
			} else {
				client = null;
                getConfig();
                if (!TextUtils.isEmpty(host)) {
                    init();
                }
			}
            
        }

        @Override
        public void subscribeTopics(List<String> topics, IClientCallback clientCallback) throws RemoteException {
            synchronized (context) {
                if (topics != null && clientCallback != null) {
                    for (String topic : topics) {
                        List<Object> objectList = callbackMap.get(topic);
                        if (objectList == null) {
                            objectList = new ArrayList<Object>();
                        }
                        objectList.add(clientCallback);
                        callbackMap.put(topic, objectList);
                        dispatchCacheMessage(topic, clientCallback);
						
						if (mIsConnect) {
							try {
								clientCallback.dispatch("/mqtt/connected/", "connected");
							} catch (Exception e) {
							}
						}
                    }
                    if (mIsConnect) {
                        try {
                            clientCallback.dispatch("/mqtt/connected/", "connected");
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }

        @Override
        public void subscribeTopic(String topic, IClientCallback clientCallback) throws RemoteException {
            synchronized (context) {
                Logger.i(TAG, "subscribeTopic topic=" + topic + " clientCallback=" + clientCallback);
                Logger.i(TAG, "threadName=" + Thread.currentThread().getName());
                if (topic != null && clientCallback != null) {
                    List<Object> objectList = callbackMap.get(topic);
                    if (objectList == null) {
                        objectList = new ArrayList<Object>();
                    }
                    objectList.add(clientCallback);
                    callbackMap.put(topic, objectList);
                    dispatchCacheMessage(topic, clientCallback);
					
                    if (mIsConnect) {
                        try {
                            clientCallback.dispatch("/mqtt/connected/", "connected");
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }

        @Override
        public void publish(String topic, String content) throws RemoteException {
            publishWithQuality(topic, MqttConstants.QOS_QUALITY_2, MqttConstants.RETAINED_DISABLE, content);
        }
    };


    @Override
    public void onDestroy() {
        if (callbackConnection != null) {
            callbackConnection.disconnect(disconnectCallback);
            callbackConnection.kill(killCallback);
        }
        cachedThreadPool.shutdown();
        super.onDestroy();
    }

    {
        killCallback = new Callback<Void>(){

            @Override
            public void onSuccess(Void value) {
                Logger.d(TAG, "killCallback : onSuccess");
            }
            @Override
            public void onFailure(Throwable value) {
                value.printStackTrace();
                Logger.d(TAG, "killCallback : failure="+value.getMessage());
            }
        };

        connectCallback = new Callback<Void>(){

            @Override
            public void onSuccess(Void value) {
                Logger.d(TAG, "connectCallback : onSuccess");
                mIsConnect = true;
				mIsLostConnect = false;
                try {
                    Topic topics[] = new Topic[]{new Topic(MqttTopics.DISPATCH_FILTER_ITEM+MqttUtils.getDeviceID(),QoS.AT_LEAST_ONCE),
                            new Topic(MqttTopics.DISPATCH_FILTER_LIST+MqttUtils.getDeviceID(),QoS.AT_LEAST_ONCE),
                            new Topic(MqttTopics.SIP_ITEM+MqttUtils.getDeviceID(),QoS.AT_LEAST_ONCE),
                            new Topic(MqttTopics.SIP_ITEM_LIST+MqttUtils.getDeviceID(),QoS.AT_LEAST_ONCE),
                            new Topic(MqttTopics.SIP_CONFIG_INFO+MqttUtils.getDeviceID(),QoS.AT_LEAST_ONCE),
                            new Topic(MqttTopics.DISPATCH_OTP+MqttUtils.getDeviceID(),QoS.AT_LEAST_ONCE),
                            new Topic(MqttTopics.UPGRADE_REQUEST+MqttUtils.getDeviceID(),QoS.AT_LEAST_ONCE),
                            new Topic(MqttTopics.DEVICE_CONTROL_REQUEST+MqttUtils.getDeviceID(),QoS.AT_LEAST_ONCE),
                            new Topic(MqttTopics.FACE_ADD+MqttUtils.getDeviceID(),QoS.AT_LEAST_ONCE),
                            new Topic(MqttTopics.FACE_DELETE+MqttUtils.getDeviceID(),QoS.AT_LEAST_ONCE),
							new Topic(MqttTopics.DISPATCH_SIP_ITEM+MqttUtils.getDeviceID(),QoS.AT_LEAST_ONCE),
							new Topic(MqttTopics.DISPATCH_AD_URL+MqttUtils.getDeviceID(),QoS.AT_LEAST_ONCE),
							new Topic(MqttTopics.DISPATCH_DOWNLOAD_URL+MqttUtils.getDeviceID(),QoS.AT_LEAST_ONCE),
							new Topic(MqttTopics.DISPATCH_GROUP_CODES+MqttUtils.getDeviceID(),QoS.AT_LEAST_ONCE),
							new Topic(MqttTopics.DISPATCH_READING_HEAD_PROGRAM_URL+MqttUtils.getDeviceID(),QoS.AT_LEAST_ONCE),
                            new Topic(MqttTopics.TIME_CALIBRATE+MqttUtils.getDeviceID(),QoS.AT_LEAST_ONCE)};

                    callbackConnection.subscribe(topics,subscribeCallback);
                } catch (Exception e) {
                    mIsConnect = false;
                }
            }
            @Override
            public void onFailure(Throwable value) {
                mIsConnect = false;
                value.printStackTrace();
                Logger.d(TAG, "connectCallback : failure="+value.getMessage());
                boolean hasNetwork = MqttUtils.isConnectIsNormal();
                if (hasNetwork) {
                    try {
                        Thread.sleep(1000);
                        doClientConnection();
                    } catch (Exception e) {
                    }
                }
            }
        };
        disconnectCallback = new Callback<Void>(){

            public void onSuccess(Void value) {
                Logger.d(TAG, "disconnect success");
                mIsConnect = false;
                getConfig();
                if (!TextUtils.isEmpty(host)) {
                    init();
                }
            }
            public void onFailure(Throwable e) {
                Logger.d(TAG, "disconnect failure");
            }
        };

        listener = new Listener() {

            @Override
            public void onConnected() {
                Logger.d(TAG, "listener onConnected");
                mIsConnect = true;
				mIsLostConnect = false;
				sendConnectedMessage();
            }

            @Override
            public void onDisconnected() {
                Logger.d(TAG, "listener onDisconnected");
                mIsConnect = false;
				mIsLostConnect = true;
            }

            @Override
            public void onPublish(final UTF8Buffer topicBuffer, Buffer msg, Runnable ack) {
                final String body = msg.utf8().toString();
                final String topic = topicBuffer.toString();

                try {
                    final String messagePayload = body;
                    Logger.i(TAG, "topic="+topic+" message="+messagePayload);
                    Logger.i(TAG, "threadName=" + Thread.currentThread().getName());

                    int index = topic.lastIndexOf("/");
                    String tempTopic = topic;
                    if (!tempTopic.startsWith("/")) {
                        tempTopic = "/" + tempTopic;
                    }
                    final String subTopic = tempTopic.substring(0, index + 1);
                    Logger.i(TAG, "subTopic=" + subTopic);

                    synchronized (context) {
                        final List<Object> objectList = callbackMap.get(subTopic);
                        if (objectList != null && objectList.size() != 0) {
                            for (final Object object : objectList) {
                                Logger.i(TAG, "subTopic=" + subTopic + " object=" + object);

                                cachedThreadPool.execute(new Runnable() {
                                    @Override
                                    public void run() {

                                        try {
                                            ((IClientCallback) object).dispatch(subTopic, messagePayload);
                                        } catch (Exception e) {
                                            if ("android.os.DeadObjectException".equals(e.toString())) {
                                                synchronized (context) {
                                                    objectList.remove(object);
                                                    callbackMap.put(subTopic, objectList);
                                                }
                                            }
                                            Logger.i(TAG, "messageArrived dispatch e=" + e.toString());
                                        }

                                    }
                                });

                            }
                            Logger.i(TAG, "process end");
                        } else {
                            saveCacheMessage(subTopic, messagePayload);
                        }

                    }
                } catch (Exception e) {
                    Logger.i(TAG, "messageArrived e="+e.getMessage());
                    //TODO
                }
            }

            @Override
            public void onFailure(Throwable value) {
                Logger.d(TAG, "listener failure");

            }
        };

        subscribeCallback = new Callback<byte[]>() {

            public void onSuccess(byte[] qoses) {
                Logger.d(TAG, "subscribe : success");
            }
            public void onFailure(Throwable value) {
                value.printStackTrace();
                Logger.d(TAG, "subscribe : failure");
            }
        };
        publishCallback = new Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                Logger.d(TAG, "publishCallback onSuccess: ");
            }

            @Override
            public void onFailure(Throwable value) {
                Logger.d(TAG, "publishCallback onFailure: ");
            }
        };
    }
	
    public void sendConnectedMessage() {
        List<Object> callbackList = new ArrayList<Object>();
        Set<String> strings = callbackMap.keySet();
        for (String key : strings) {
            List<Object> objects = callbackMap.get(key);
            for (Object object : objects) {
                if (!callbackList.contains(object)) {
                    try {
                        ((IClientCallback) object).dispatch("/mqtt/connected/", "connected");
                    } catch (Exception e) {
                    }
                    callbackList.add(object);
                }
            }
        }
    }

}
