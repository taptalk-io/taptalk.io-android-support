package io.taptalk.TapTalk.Helper;

import android.app.Application;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.arch.lifecycle.ProcessLifecycleOwner;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.facebook.stetho.Stetho;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.android.libraries.places.api.Places;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.RemoteMessage;
import com.orhanobut.hawk.Hawk;
import com.orhanobut.hawk.NoEncryption;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.taptalk.TapTalk.API.Api.TAPApiManager;
import io.taptalk.TapTalk.API.View.TAPDefaultDataView;
import io.taptalk.TapTalk.Listener.TAPChatListener;
import io.taptalk.TapTalk.Listener.TapCommonListener;
import io.taptalk.TapTalk.Listener.TapCoreProjectConfigsListener;
import io.taptalk.TapTalk.Listener.TapListener;
import io.taptalk.TapTalk.Manager.AnalyticsManager;
import io.taptalk.TapTalk.Manager.TAPCacheManager;
import io.taptalk.TapTalk.Manager.TAPChatManager;
import io.taptalk.TapTalk.Manager.TAPConnectionManager;
import io.taptalk.TapTalk.Manager.TAPContactManager;
import io.taptalk.TapTalk.Manager.TAPDataManager;
import io.taptalk.TapTalk.Manager.TAPEncryptorManager;
import io.taptalk.TapTalk.Manager.TAPFileDownloadManager;
import io.taptalk.TapTalk.Manager.TAPGroupManager;
import io.taptalk.TapTalk.Manager.TAPMessageStatusManager;
import io.taptalk.TapTalk.Manager.TAPNetworkStateManager;
import io.taptalk.TapTalk.Manager.TAPNotificationManager;
import io.taptalk.TapTalk.Manager.TAPOldDataManager;
import io.taptalk.TapTalk.Manager.TapCoreProjectConfigsManager;
import io.taptalk.TapTalk.Manager.TapCoreRoomListManager;
import io.taptalk.TapTalk.Manager.TapLocaleManager;
import io.taptalk.TapTalk.Model.ResponseModel.TAPCommonResponse;
import io.taptalk.TapTalk.Model.ResponseModel.TAPContactResponse;
import io.taptalk.TapTalk.Model.ResponseModel.TAPGetAccessTokenResponse;
import io.taptalk.TapTalk.Model.ResponseModel.TAPGetUserResponse;
import io.taptalk.TapTalk.Model.TAPContactModel;
import io.taptalk.TapTalk.Model.TAPErrorModel;
import io.taptalk.TapTalk.Model.TAPMessageModel;
import io.taptalk.TapTalk.Model.TAPUserModel;
import io.taptalk.TapTalk.Model.TapConfigs;
import io.taptalk.TapTalk.View.Activity.TapUIChatActivity;
import io.taptalk.TapTalk.ViewModel.TAPRoomListViewModel;
import io.taptalk.Taptalk.BuildConfig;
import io.taptalk.Taptalk.R;

import static io.taptalk.TapTalk.Const.TAPDefaultConstant.ClientErrorCodes.ERROR_CODE_ACCESS_TOKEN_UNAVAILABLE;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.ClientErrorCodes.ERROR_CODE_ACTIVE_USER_NOT_FOUND;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.ClientErrorCodes.ERROR_CODE_INVALID_AUTH_TICKET;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.ClientErrorCodes.ERROR_CODE_OTHERS;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.ClientErrorMessages.ERROR_MESSAGE_ACCESS_TOKEN_UNAVAILABLE;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.ClientErrorMessages.ERROR_MESSAGE_ACTIVE_USER_NOT_FOUND;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.ClientErrorMessages.ERROR_MESSAGE_INVALID_AUTH_TICKET;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.ClientSuccessMessages.SUCCESS_MESSAGE_AUTHENTICATE;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.ClientSuccessMessages.SUCCESS_MESSAGE_REFRESH_ACTIVE_USER;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.ClientSuccessMessages.SUCCESS_MESSAGE_REFRESH_CONFIG;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.DEFAULT_CHANNEL_MAX_PARTICIPANTS;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.DEFAULT_CHAT_MEDIA_MAX_FILE_SIZE;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.DEFAULT_GROUP_MAX_PARTICIPANTS;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.DEFAULT_ROOM_PHOTO_MAX_FILE_SIZE;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.DEFAULT_USER_PHOTO_MAX_FILE_SIZE;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.DatabaseType.MESSAGE_DB;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.DatabaseType.MY_CONTACT_DB;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.DatabaseType.SEARCH_DB;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.ProjectConfigKeys.CHANNEL_MAX_PARTICIPANTS;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.ProjectConfigKeys.CHAT_MEDIA_MAX_FILE_SIZE;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.ProjectConfigKeys.GROUP_MAX_PARTICIPANTS;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.ProjectConfigKeys.ROOM_PHOTO_MAX_FILE_SIZE;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.ProjectConfigKeys.USERNAME_IGNORE_CASE;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.ProjectConfigKeys.USER_PHOTO_MAX_FILE_SIZE;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.REFRESH_TOKEN_RENEWED;
import static io.taptalk.TapTalk.Manager.TAPConnectionManager.ConnectionStatus.CONNECTED;
import static io.taptalk.TapTalk.Manager.TAPConnectionManager.ConnectionStatus.NOT_CONNECTED;

public class TapTalk implements LifecycleObserver {
    private static final String TAG = TapTalk.class.getSimpleName();
    public static TapTalk tapTalk;
    public static Context appContext;
    public static boolean isForeground;
    public static boolean isLoggingEnabled = false;
    private static TapTalkScreenOrientation screenOrientation = TapTalkScreenOrientation.TapTalkOrientationDefault;
    //    public static boolean isOpenDefaultProfileEnabled = true;
    private static String clientAppName = "";
    private static int clientAppIcon = R.drawable.tap_ic_taptalk_logo;
    private static boolean isRefreshTokenExpired, isAutoConnectDisabled, isAutoContactSyncDisabled;
    private Intent intent;
    private static boolean listenerInit = false;

    //    private static Thread.UncaughtExceptionHandler defaultUEH;
    private List<TapListener> tapListeners = new ArrayList<>();

    private static Map<String, String> coreConfigs;
    private static Map<String, String> projectConfigs;
    private static Map<String, String> customConfigs;
    public static TapTalkImplementationType implementationType;
    private static TAPChatListener chatListener;

    public static String mixpanelToken = "";

    public enum TapTalkEnvironment {
        TapTalkEnvironmentProduction,
        TapTalkEnvironmentStaging,
        TapTalkEnvironmentDevelopment;
    }

    public enum TapTalkImplementationType {
        TapTalkImplementationTypeCore,
        TapTalkImplementationTypeUI,
        TapTalkImplementationTypeCombine
    }

    public enum TapTalkScreenOrientation {
        TapTalkOrientationDefault,
        TapTalkOrientationPortrait,
        TapTalkOrientationLandscape // FIXME: 6 February 2019 Activity loads portrait by default then changes to landscape after onCreate
    }

//    private static Thread.UncaughtExceptionHandler uncaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {
//        @Override
//        public void uncaughtException(Thread thread, Throwable throwable) {
//            TAPChatManager.getInstance().saveIncomingMessageAndDisconnect();
//            TAPContactManager.getInstance().saveUserDataMapToDatabase();
//            TAPFileDownloadManager.getInstance().saveFileProviderPathToPreference();
//            TAPFileDownloadManager.getInstance().saveFileMessageUriToPreference();
//            defaultUEH.uncaughtException(thread, throwable);
//        }
//    };

    public TapTalk(
            @NonNull final Context appContext,
            @NonNull String appID,
            @NonNull String appSecret,
            @NonNull String userAgent,
            int clientAppIcon,
            String clientAppName,
            String appBaseURL,
            TapTalkImplementationType type,
            @NonNull TapListener tapListener) {

        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);

        TapTalk.appContext = appContext;
        TapTalk.clientAppIcon = clientAppIcon;
        TapTalk.clientAppName = clientAppName;

        // Init Base URL
        TAPApiManager.setBaseUrlApi(generateApiBaseURL(appBaseURL));
        TAPApiManager.setBaseUrlSocket(generateSocketBaseURL(appBaseURL));
        TAPConnectionManager.getInstance().setWebSocketEndpoint(generateWSSBaseURL(appBaseURL));

        // Init Hawk for preference
        if (BuildConfig.BUILD_TYPE.equals("dev")) {
            // No encryption for dev build
            Hawk.init(appContext).setEncryption(new NoEncryption()).build();
        } else {
            Hawk.init(appContext).build();
        }

        implementationType = type;

        TAPCacheManager.getInstance(appContext).initAllCache();

        // Init database
        TAPDataManager.getInstance().initDatabaseManager(MESSAGE_DB, (Application) appContext);
        TAPDataManager.getInstance().initDatabaseManager(SEARCH_DB, (Application) appContext);
        TAPDataManager.getInstance().initDatabaseManager(MY_CONTACT_DB, (Application) appContext);
        // Update here when adding database table

        // Save header requirement
        TAPDataManager.getInstance().saveApplicationID(appID);
        TAPDataManager.getInstance().saveApplicationSecret(appSecret);
        TAPDataManager.getInstance().saveUserAgent(userAgent);

        // Init configs
        presetConfigs();
        refreshRemoteConfigs(new TapCommonListener() {
        });

        if (TAPDataManager.getInstance().checkAccessTokenAvailable()) {
            //TAPConnectionManager.getInstance().connect();
            TAPContactManager.getInstance().setMyCountryCode(TAPDataManager.getInstance().getMyCountryCode());

            TAPFileDownloadManager.getInstance().getFileProviderPathFromPreference();
            TAPFileDownloadManager.getInstance().getFileMessageUriFromPreference();
            TAPOldDataManager.getInstance().startAutoCleanProcess();
        }

        TAPDataManager.getInstance().updateSendingMessageToFailed();
        TAPContactManager.getInstance().setContactSyncPermissionAsked(TAPDataManager.getInstance().isContactSyncPermissionAsked());
        TAPContactManager.getInstance().setContactSyncAllowedByUser(TAPDataManager.getInstance().isContactSyncAllowedByUser());

        // Init Stetho for debug build
        if (BuildConfig.DEBUG) {
            Stetho.initialize(
                    Stetho.newInitializerBuilder(appContext)
                            .enableDumpapp(Stetho.defaultDumperPluginsProvider(appContext))
                            .enableWebKitInspector(Stetho.defaultInspectorModulesProvider(appContext))
                            .build()
            );
        }

        if (!tapListeners.contains(tapListener)) {
            tapListeners.add(tapListener);
        }

        TAPContactManager.getInstance().loadAllUserDataFromDatabase();

        if (null != TAPDataManager.getInstance().checkAccessTokenAvailable() &&
                TAPDataManager.getInstance().checkAccessTokenAvailable()) {
            initListener();
        }

        if (!listenerInit) {
            handleAppToForeground();
        }
    }

    public static void initializeAnalyticsForSampleApps(String analyticsKey) {
        mixpanelToken = analyticsKey;
    }

    private static void initListener() {
        chatListener = new TAPChatListener() {
            @Override
            public void onReceiveMessageInOtherRoom(TAPMessageModel message) {
                updateApplicationBadgeCount();
            }

            @Override
            public void onReceiveMessageInActiveRoom(TAPMessageModel message) {
                updateApplicationBadgeCount();
            }

            @Override
            public void onUpdateMessageInOtherRoom(TAPMessageModel message) {
                updateApplicationBadgeCount();
            }

            @Override
            public void onUpdateMessageInActiveRoom(TAPMessageModel message) {
                updateApplicationBadgeCount();
            }

            @Override
            public void onDeleteMessageInOtherRoom(TAPMessageModel message) {
                updateApplicationBadgeCount();
            }

            @Override
            public void onDeleteMessageInActiveRoom(TAPMessageModel message) {
                updateApplicationBadgeCount();
            }
        };
        TAPChatManager.getInstance().addChatListener(chatListener);
    }

    public static void putGlobalChatListener() {
        TAPChatManager.getInstance().addChatListener(chatListener);
    }

    public static void removeGlobalChatListener() {
        TAPChatManager.getInstance().removeChatListener(chatListener);
    }

    /**
     * =============================================================================================
     * INITIALIZATION
     * =============================================================================================
     */

    public static TapTalk init(Context context, String appKeyID, String appKeySecret, String userAgent, int clientAppIcon, String clientAppName, String appBaseURL, TapTalkImplementationType type, TapListener listener) {
        return tapTalk == null ? (tapTalk = new TapTalk(context, appKeyID, appKeySecret, userAgent, clientAppIcon, clientAppName, appBaseURL, type, listener)) : tapTalk;
    }

    public static TapTalk init(Context context, String appKeyID, String appKeySecret, int clientAppIcon, String clientAppName, String appBaseURL, TapTalkImplementationType type, TapListener listener) {
        return tapTalk == null ? (tapTalk = new TapTalk(context, appKeyID, appKeySecret, "android", clientAppIcon, clientAppName, appBaseURL, type, listener)) : tapTalk;
    }

    public static void initializeGooglePlacesApiKey(String apiKey) {
        if (!checkTapTalkInitialized()) {
            return;
        }
        Places.initialize(appContext, apiKey);
    }

    public static boolean checkTapTalkInitialized() {
        if (null == tapTalk) {
            Log.e(TAG, "Please initialize TapTalk library, read documentation for detailed information.");
            return false;
        }
        return true;
    }

    public static void setLoggingEnabled(boolean enabled) {
        isLoggingEnabled = enabled;
    }

    private String generateSocketBaseURL(String baseURL) {
        return baseURL + "/";
    }

    private String generateWSSBaseURL(String baseURL) {
        return (baseURL + "/connect").replace("https", "wss");
    }

    private String generateApiBaseURL(String baseURL) {
        return baseURL + "/v1/";
    }

    /**
     * =============================================================================================
     * AUTHENTICATION
     * =============================================================================================
     */

    public static void authenticateWithAuthTicket(String authTicket, boolean connectOnSuccess, TapCommonListener listener) {
        if (!checkTapTalkInitialized()) {
            return;
        }
        if (null == authTicket || "".equals(authTicket)) {
            listener.onError(ERROR_CODE_INVALID_AUTH_TICKET, ERROR_MESSAGE_INVALID_AUTH_TICKET);
        } else {
            TAPDataManager.getInstance().saveAuthTicket(authTicket);
            TAPDataManager.getInstance().getAccessTokenFromApi(new TAPDefaultDataView<TAPGetAccessTokenResponse>() {
                @Override
                public void onSuccess(TAPGetAccessTokenResponse response) {
                    TAPDataManager.getInstance().removeAuthTicket();
                    TAPDataManager.getInstance().saveAccessToken(response.getAccessToken());
                    TAPDataManager.getInstance().saveRefreshToken(response.getRefreshToken());
                    TAPDataManager.getInstance().saveRefreshTokenExpiry(response.getRefreshTokenExpiry());
                    TAPDataManager.getInstance().saveAccessTokenExpiry(response.getAccessTokenExpiry());

                    new Thread(() -> {
                        if (!TAPDataManager.getInstance().checkFirebaseToken()) {
                            try {
                                FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(task -> {
                                    if (null != task.getResult()) {
                                        String fcmToken = task.getResult().getToken();
                                        TAPDataManager.getInstance().registerFcmTokenToServer(fcmToken, new TAPDefaultDataView<TAPCommonResponse>() {
                                        });
                                        TAPDataManager.getInstance().saveFirebaseToken(fcmToken);
                                    }
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            TAPDataManager.getInstance().registerFcmTokenToServer(TAPDataManager.getInstance().getFirebaseToken(), new TAPDefaultDataView<TAPCommonResponse>() {
                            });
                        }
                    }).start();

                    new Thread(() -> TAPDataManager.getInstance().getMyContactListFromAPI(new TAPDefaultDataView<TAPContactResponse>() {
                        @Override
                        public void onSuccess(TAPContactResponse response) {
                            List<TAPUserModel> userModels = new ArrayList<>();
                            for (TAPContactModel contact : response.getContacts()) {
                                userModels.add(contact.getUser().setUserAsContact());
                            }
                            TAPDataManager.getInstance().insertMyContactToDatabase(userModels);
                            TAPContactManager.getInstance().updateUserData(userModels);
                        }
                    })).start();

                    TAPDataManager.getInstance().saveActiveUser(response.getUser());
                    TAPApiManager.getInstance().setLogout(false);
                    if (connectOnSuccess) {
                        TAPConnectionManager.getInstance().connect();
                    }
                    listener.onSuccess(SUCCESS_MESSAGE_AUTHENTICATE);

                    if (isRefreshTokenExpired) {
                        isRefreshTokenExpired = false;
                        Intent intent = new Intent(REFRESH_TOKEN_RENEWED);
                        LocalBroadcastManager.getInstance(appContext).sendBroadcast(intent);
                    }
                }

                @Override
                public void onError(TAPErrorModel error) {
                    listener.onError(error.getCode(), error.getMessage());
                }

                @Override
                public void onError(String errorMessage) {
                    listener.onError(ERROR_CODE_OTHERS, errorMessage);
                }
            });
        }
    }

    public static boolean isAuthenticated() {
        return TAPDataManager.getInstance().checkAccessTokenAvailable();
    }

    public static void logoutAndClearAllTapTalkData(TapCommonListener listener) {
        if (!checkTapTalkInitialized()) {
            return;
        }
        TAPDataManager.getInstance().logout(new TAPDefaultDataView<TAPCommonResponse>() {
            @Override
            public void onSuccess(TAPCommonResponse response) {
                clearAllTapTalkData();
                listener.onSuccess(response.getMessage());
            }

            @Override
            public void onError(TAPErrorModel error) {
                listener.onError(error.getCode(), error.getMessage());
            }

            @Override
            public void onError(String errorMessage) {
                listener.onError(ERROR_CODE_OTHERS, errorMessage);
            }
        });
    }

    public static void clearAllTapTalkData() {
        if (!checkTapTalkInitialized()) {
            return;
        }
        TAPDataManager.getInstance().deleteAllPreference();
        TAPDataManager.getInstance().deleteAllFromDatabase();
        TAPDataManager.getInstance().deleteAllManagerData();
        TAPApiManager.getInstance().setLogout(true);
        TAPRoomListViewModel.setShouldNotLoadFromAPI(false);
        TAPChatManager.getInstance().disconnectAfterRefreshTokenExpired();
        isRefreshTokenExpired = true;
    }

    /**
     * =============================================================================================
     * CONNECTION
     * =============================================================================================
     */

    public static void connect(TapCommonListener listener) {
        if (!checkTapTalkInitialized()) {
            return;

        }
        if (isAuthenticated()) {
            TAPConnectionManager.getInstance().connect(listener);
        } else {
            listener.onError(ERROR_CODE_ACCESS_TOKEN_UNAVAILABLE, ERROR_MESSAGE_ACCESS_TOKEN_UNAVAILABLE);
        }
    }

    public static void disconnect() {
        if (!checkTapTalkInitialized()) {
            return;
        }
        TAPConnectionManager.getInstance().close(NOT_CONNECTED);
    }

    public static boolean isConnected() {
        if (!checkTapTalkInitialized()) {
            return false;
        }
        return TAPConnectionManager.getInstance().getConnectionStatus() == CONNECTED;
    }

    public static void setAutoConnectEnabled(boolean enabled) {
        if (!checkTapTalkInitialized()) {
            return;
        }
        isAutoConnectDisabled = !enabled;
    }

    public static boolean isAutoConnectEnabled() {
        if (!checkTapTalkInitialized()) {
            return false;
        }
        return !isAutoConnectDisabled;
    }

    /**
     * =============================================================================================
     * GENERAL
     * =============================================================================================
     */

    public static int getClientAppIcon() {
        if (!checkTapTalkInitialized()) {
            return -1;
        }
        return clientAppIcon;
    }

    public static String getClientAppName() {
        return clientAppName;
    }

    public static TapTalkImplementationType getTapTalkImplementationType() {
        if (!checkTapTalkInitialized()) {
            return null;
        }
        return implementationType;
    }

    public static void updateApplicationBadgeCount() {
        if (!isAuthenticated()) {
            return;
        }
        TAPNotificationManager.getInstance().updateUnreadCount();
    }

    // TODO: 22 August 2019 CORE MODEL
    public static Map<String, String> getCoreConfigs() {
        if (!checkTapTalkInitialized()) {
            return null;
        }
        return new HashMap<>(coreConfigs);
    }

    public static Map<String, String> getProjectConfigs() {
        if (!checkTapTalkInitialized()) {
            return null;
        }
        return new HashMap<>(projectConfigs);
    }

    public static Map<String, String> getCustomConfigs() {
        if (!checkTapTalkInitialized()) {
            return null;
        }
        return new HashMap<>(customConfigs);
    }

    public static void refreshRemoteConfigs(TapCommonListener listener) {
        TapCoreProjectConfigsManager.getInstance().getProjectConfigs(new TapCoreProjectConfigsListener() {
            @Override
            public void onSuccess(TapConfigs config) {
                coreConfigs = config.getCoreConfigs();
                projectConfigs = config.getProjectConfigs();
                customConfigs = config.getCustomConfigs();
                TAPDataManager.getInstance().saveCoreConfigs(coreConfigs);
                TAPDataManager.getInstance().saveProjectConfigs(projectConfigs);
                TAPDataManager.getInstance().saveCustomConfigs(customConfigs);
                if (null != listener) {
                    listener.onSuccess(SUCCESS_MESSAGE_REFRESH_CONFIG);
                }
            }

            @Override
            public void onError(String errorCode, String errorMessage) {
                if (null != listener) {
                    listener.onError(ERROR_CODE_OTHERS, errorMessage);
                }
            }
        });
    }

    private static void presetConfigs() {
        coreConfigs = TAPDataManager.getInstance().getCoreConfigs();
        projectConfigs = TAPDataManager.getInstance().getProjectConfigs();
        customConfigs = TAPDataManager.getInstance().getCustomConfigs();

        // Set default values if configs are empty
        if (coreConfigs.isEmpty()) {
            coreConfigs.put(CHAT_MEDIA_MAX_FILE_SIZE, DEFAULT_CHAT_MEDIA_MAX_FILE_SIZE);
            coreConfigs.put(ROOM_PHOTO_MAX_FILE_SIZE, DEFAULT_ROOM_PHOTO_MAX_FILE_SIZE);
            coreConfigs.put(USER_PHOTO_MAX_FILE_SIZE, DEFAULT_USER_PHOTO_MAX_FILE_SIZE);
            coreConfigs.put(GROUP_MAX_PARTICIPANTS, DEFAULT_GROUP_MAX_PARTICIPANTS);
            coreConfigs.put(CHANNEL_MAX_PARTICIPANTS, DEFAULT_CHANNEL_MAX_PARTICIPANTS);
        }

        if (projectConfigs.isEmpty()) {
            projectConfigs.put(USERNAME_IGNORE_CASE, "1");
        }
    }

    public static void setAutoContactSyncEnabled(boolean enabled) {
        if (!checkTapTalkInitialized()) {
            return;
        }
        isAutoContactSyncDisabled = !enabled;
    }

    public static boolean isAutoContactSyncEnabled() {
        if (!checkTapTalkInitialized()) {
            return false;
        }
        return !isAutoContactSyncDisabled;
    }

    /**
     * =============================================================================================
     * USER
     * =============================================================================================
     */

    public static TAPUserModel getTaptalkActiveUser() {
        if (!checkTapTalkInitialized()) {
            return null;
        }
        return TAPChatManager.getInstance().getActiveUser();
    }

    public static void refreshActiveUser(TapCommonListener listener) {
        if (!checkTapTalkInitialized()) {
            return;
        }
        new Thread(() -> {
            if (null != TAPChatManager.getInstance().getActiveUser()) {
                TAPDataManager.getInstance().getUserByIdFromApi(TAPChatManager.getInstance().getActiveUser().getUserID(), new TAPDefaultDataView<TAPGetUserResponse>() {
                    @Override
                    public void onSuccess(TAPGetUserResponse response) {
                        TAPDataManager.getInstance().saveActiveUser(response.getUser());
                        listener.onSuccess(SUCCESS_MESSAGE_REFRESH_ACTIVE_USER);
                    }

                    @Override
                    public void onError(TAPErrorModel error) {
                        listener.onError(error.getCode(), error.getMessage());
                    }

                    @Override
                    public void onError(String errorMessage) {
                        listener.onError(ERROR_CODE_OTHERS, errorMessage);
                    }
                });
            } else {
                listener.onError(ERROR_CODE_ACTIVE_USER_NOT_FOUND, ERROR_MESSAGE_ACTIVE_USER_NOT_FOUND);
            }
        }).start();
    }

    /**
     * =============================================================================================
     * NOTIFICATION
     * =============================================================================================
     */

    public static boolean isTapTalkNotification(RemoteMessage remoteMessage) {
        return null != remoteMessage &&
                null != remoteMessage.getData() &&
                null != remoteMessage.getData().get("identifier") &&
                remoteMessage.getData().get("identifier").equals("io.taptalk.TapTalk");
    }

    public static void handleTapTalkPushNotification(RemoteMessage remoteMessage) {
        TAPNotificationManager.getInstance().updateNotificationMessageMapWhenAppKilled();
        HashMap<String, Object> notificationMap = TAPUtils.fromJSON(new TypeReference<HashMap<String, Object>>() {
        }, remoteMessage.getData().get("body"));
        try {
            //Log.e(TAG, "onMessageReceived: " + TAPUtils.toJsonString(remoteMessage));
            TAPNotificationManager.getInstance().createAndShowBackgroundNotification(appContext, TapTalk.getClientAppIcon(),
                    TapUIChatActivity.class,
                    TAPEncryptorManager.getInstance().decryptMessage(notificationMap));
        } catch (Exception e) {
            Log.e(TAG, "onMessageReceived: ", e);
            e.printStackTrace();
        }
    }

    public static void showTapTalkNotification(TAPMessageModel tapMessageModel) {
        new TAPNotificationManager.NotificationBuilder(appContext)
                .setNotificationMessage(tapMessageModel)
                .setSmallIcon(TapTalk.getClientAppIcon())
                .setNeedReply(false)
                .setOnClickAction(TapUIChatActivity.class)
                .show();
    }

    /**
     * =============================================================================================
     * LANGUAGE
     * =============================================================================================
     */

    public enum Language {ENGLISH, INDONESIAN}

    public static void setDefaultLanguage(Language language) {
        String defaultLanguage;
        switch (language) {
            case INDONESIAN:
                defaultLanguage = "in";
                break;
            default:
                defaultLanguage = "en";
                break;
        }
        TapLocaleManager.setLocale((Application) appContext, defaultLanguage);

        // TODO: 27 Feb 2020 RESTART OPEN ACTIVITIES TO APPLY CHANGED RESOURCES
    }

    /**
     * =============================================================================================
     * TEMP
     * =============================================================================================
     */

    public static List<TapListener> getTapTalkListeners() {
        if (!checkTapTalkInitialized()) {
            return null;
        }
        return tapTalk.tapListeners;
    }

    public static void addTapTalkListener(TapListener listener) {
        if (!checkTapTalkInitialized()) {
            return;
        }
        if (!tapTalk.tapListeners.contains(listener)) {
            tapTalk.tapListeners.add(listener);
        }
    }

    public static void removeTapTalkListener(TapListener listener) {
        if (!checkTapTalkInitialized()) {
            return;
        }
        tapTalk.tapListeners.remove(listener);
    }

    private static void setTapTalkScreenOrientation(TapTalkScreenOrientation orientation) {
        if (!checkTapTalkInitialized()) {
            return;
        }
        TapTalk.screenOrientation = orientation;
    }

    private static TapTalkScreenOrientation getTapTalkScreenOrientation() {
        if (!checkTapTalkInitialized()) {
            return null;
        }
        return TapTalk.screenOrientation;
    }

    // Enable/disable in-app notification after chat fragment goes inactive or to background
    private static void setInAppNotificationEnabled(boolean enabled) {
        if (!checkTapTalkInitialized()) {
            return;
        }
        TAPNotificationManager.getInstance().setRoomListAppear(!enabled);
    }

//    private static void setOpenTapTalkUserProfileByDefaultEnabled(boolean enabled) {
//        isOpenDefaultProfileEnabled = enabled;
//    }

    private void createAndShowBackgroundNotification(Context context, int notificationIcon, Class destinationClass, TAPMessageModel newMessageModel) {
        if (!checkTapTalkInitialized()) {
            return;
        }
        TAPNotificationManager.getInstance().createAndShowBackgroundNotification(context, notificationIcon, destinationClass, newMessageModel);
    }

    public static void fetchNewMessageandUpdatedBadgeCount() {
        if (TapTalk.checkTapTalkInitialized() && TapTalk.isAuthenticated()) {
            TapCoreRoomListManager.getInstance().fetchNewMessageToDatabase(new TapCommonListener() {
                @Override
                public void onSuccess(String s) {
                    TapTalk.updateApplicationBadgeCount();
                }

                @Override
                public void onError(String s, String s1) {
                    TapTalk.updateApplicationBadgeCount();
                }
            });
        }
    }


    public static void handleAppToForeground() {
        isForeground = true;
        TAPContactManager.getInstance().loadAllUserDataFromDatabase();
        TAPGroupManager.Companion.getGetInstance().loadAllRoomDataFromPreference();
        TAPChatManager.getInstance().setFinishChatFlow(false);
        TAPNetworkStateManager.getInstance().registerCallback(TapTalk.appContext);
        TAPChatManager.getInstance().triggerSaveNewMessage();
        TAPFileDownloadManager.getInstance().getFileProviderPathFromPreference();
        TAPFileDownloadManager.getInstance().getFileMessageUriFromPreference();
//        defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
//        Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);

//         Start service on first load
//        if (null == intent) {
//            intent = new Intent(TapTalk.appContext, TapTalkEndAppService.class);
//            appContext.startService(intent);
//        }
    }

    public static void handleAppToBackground() {
        isForeground = false;
        TAPRoomListViewModel.setShouldNotLoadFromAPI(false);
        TAPDataManager.getInstance().setNeedToQueryUpdateRoomList(true);
        TAPNetworkStateManager.getInstance().unregisterCallback(TapTalk.appContext);
        TAPChatManager.getInstance().updateMessageWhenEnterBackground();
        TAPMessageStatusManager.getInstance().updateMessageStatusWhenAppToBackground();
        TAPChatManager.getInstance().setNeedToCalledUpdateRoomStatusAPI(true);
        TAPFileDownloadManager.getInstance().saveFileProviderPathToPreference();
        TAPFileDownloadManager.getInstance().saveFileMessageUriToPreference();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onAppBackgrounded() {
        if (null != tapTalk) {
            handleAppToBackground();
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onAppForegrounded() {
        if (null != tapTalk) {
            handleAppToForeground();
        }
    }
}
