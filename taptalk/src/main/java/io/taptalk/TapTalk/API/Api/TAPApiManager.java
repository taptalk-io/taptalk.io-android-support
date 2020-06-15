package io.taptalk.TapTalk.API.Api;

import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.util.List;

import javax.annotation.Nullable;

import io.taptalk.TapTalk.API.RequestBody.ProgressRequestBody;
import io.taptalk.TapTalk.API.Service.TAPTalkApiService;
import io.taptalk.TapTalk.API.Service.TAPTalkDownloadApiService;
import io.taptalk.TapTalk.API.Service.TAPTalkMultipartApiService;
import io.taptalk.TapTalk.API.Service.TAPTalkRefreshTokenService;
import io.taptalk.TapTalk.API.Service.TAPTalkSocketService;
import io.taptalk.TapTalk.Exception.TAPApiRefreshTokenRunningException;
import io.taptalk.TapTalk.Exception.TAPApiSessionExpiredException;
import io.taptalk.TapTalk.Exception.TAPAuthException;
import io.taptalk.TapTalk.Helper.TapTalk;
import io.taptalk.TapTalk.Listener.TapListener;
import io.taptalk.TapTalk.Manager.AnalyticsManager;
import io.taptalk.TapTalk.Manager.TAPDataManager;
import io.taptalk.TapTalk.Manager.TAPEncryptorManager;
import io.taptalk.TapTalk.Model.RequestModel.TAPAddContactByPhoneRequest;
import io.taptalk.TapTalk.Model.RequestModel.TAPAddRoomParticipantRequest;
import io.taptalk.TapTalk.Model.RequestModel.TAPAuthTicketRequest;
import io.taptalk.TapTalk.Model.RequestModel.TAPCommonRequest;
import io.taptalk.TapTalk.Model.RequestModel.TAPCreateRoomRequest;
import io.taptalk.TapTalk.Model.RequestModel.TAPDeleteMessageRequest;
import io.taptalk.TapTalk.Model.RequestModel.TAPDeleteRoomRequest;
import io.taptalk.TapTalk.Model.RequestModel.TAPFileDownloadRequest;
import io.taptalk.TapTalk.Model.RequestModel.TAPGetMessageListByRoomAfterRequest;
import io.taptalk.TapTalk.Model.RequestModel.TAPGetMessageListByRoomBeforeRequest;
import io.taptalk.TapTalk.Model.RequestModel.TAPGetMultipleUserByIdRequest;
import io.taptalk.TapTalk.Model.RequestModel.TAPGetRoomByXcRoomIDRequest;
import io.taptalk.TapTalk.Model.RequestModel.TAPGetUserByIdRequest;
import io.taptalk.TapTalk.Model.RequestModel.TAPGetUserByUsernameRequest;
import io.taptalk.TapTalk.Model.RequestModel.TAPGetUserByXcUserIdRequest;
import io.taptalk.TapTalk.Model.RequestModel.TAPLoginOTPRequest;
import io.taptalk.TapTalk.Model.RequestModel.TAPLoginOTPVerifyRequest;
import io.taptalk.TapTalk.Model.RequestModel.TAPPushNotificationRequest;
import io.taptalk.TapTalk.Model.RequestModel.TAPRegisterRequest;
import io.taptalk.TapTalk.Model.RequestModel.TAPSendCustomMessageRequest;
import io.taptalk.TapTalk.Model.RequestModel.TAPUpdateMessageStatusRequest;
import io.taptalk.TapTalk.Model.RequestModel.TAPUpdateRoomRequest;
import io.taptalk.TapTalk.Model.RequestModel.TAPUserIdRequest;
import io.taptalk.TapTalk.Model.ResponseModel.TAPAddContactByPhoneResponse;
import io.taptalk.TapTalk.Model.ResponseModel.TAPAddContactResponse;
import io.taptalk.TapTalk.Model.ResponseModel.TAPAuthTicketResponse;
import io.taptalk.TapTalk.Model.ResponseModel.TAPBaseResponse;
import io.taptalk.TapTalk.Model.ResponseModel.TAPCheckUsernameResponse;
import io.taptalk.TapTalk.Model.ResponseModel.TAPCommonResponse;
import io.taptalk.TapTalk.Model.ResponseModel.TAPContactResponse;
import io.taptalk.TapTalk.Model.ResponseModel.TAPCountryListResponse;
import io.taptalk.TapTalk.Model.ResponseModel.TAPCreateRoomResponse;
import io.taptalk.TapTalk.Model.ResponseModel.TAPDeleteMessageResponse;
import io.taptalk.TapTalk.Model.ResponseModel.TAPGetAccessTokenResponse;
import io.taptalk.TapTalk.Model.ResponseModel.TAPGetMessageListByRoomResponse;
import io.taptalk.TapTalk.Model.ResponseModel.TAPGetMultipleUserResponse;
import io.taptalk.TapTalk.Model.ResponseModel.TAPGetRoomListResponse;
import io.taptalk.TapTalk.Model.ResponseModel.TAPGetUserResponse;
import io.taptalk.TapTalk.Model.ResponseModel.TAPLoginOTPResponse;
import io.taptalk.TapTalk.Model.ResponseModel.TAPLoginOTPVerifyResponse;
import io.taptalk.TapTalk.Model.ResponseModel.TAPRegisterResponse;
import io.taptalk.TapTalk.Model.ResponseModel.TAPSendCustomMessageResponse;
import io.taptalk.TapTalk.Model.ResponseModel.TAPUpdateMessageStatusResponse;
import io.taptalk.TapTalk.Model.ResponseModel.TAPUpdateRoomResponse;
import io.taptalk.TapTalk.Model.ResponseModel.TAPUploadFileResponse;
import io.taptalk.TapTalk.Model.TAPErrorModel;
import io.taptalk.TapTalk.Model.TAPRoomModel;
import io.taptalk.TapTalk.Model.TapConfigs;
import io.taptalk.Taptalk.BuildConfig;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static io.taptalk.TapTalk.Const.TAPDefaultConstant.HttpResponseStatusCode.RESPONSE_SUCCESS;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.HttpResponseStatusCode.UNAUTHORIZED;

public class TAPApiManager {
    private static final String TAG = TAPApiManager.class.getSimpleName();

    //ini url buat api disimpen sesuai environment
    @NonNull private static String BaseUrlApi = "";
    @NonNull private static String BaseUrlSocket = "";

    private TAPTalkApiService homingPigeon;
    private TAPTalkSocketService hpSocket;
    private TAPTalkRefreshTokenService hpRefresh;
    private static TAPApiManager instance;
    private int isShouldRefreshToken = 0;
    //ini flagging jadi kalau logout (refresh token expired) dy ga akan ngulang2 manggil api krna 401
    private boolean isLogout = false;

    public static TAPApiManager getInstance() {
        return instance == null ? instance = new TAPApiManager() : instance;
    }

    private TAPApiManager() {
        TAPApiConnection connection = TAPApiConnection.getInstance();
        this.homingPigeon = connection.getHomingPigeon();
        this.hpSocket = connection.getHpValidate();
        this.hpRefresh = connection.getHpRefresh();
    }

    private long calculateTimeOutTimeWithFileSize(long fileSize) {
        return fileSize / 10 + 60000;
    }

    public boolean isLogout() {
        return isLogout;
    }

    public void setLogout(boolean logout) {
        isLogout = logout;
    }

    @NonNull
    public static String getBaseUrlApi() {
        return BaseUrlApi;
    }

    public static void setBaseUrlApi(@NonNull String baseUrlApi) {
        BaseUrlApi = baseUrlApi;
    }

    @NonNull
    public static String getBaseUrlSocket() {
        return BaseUrlSocket;
    }

    public static void setBaseUrlSocket(@NonNull String baseUrlSocket) {
        BaseUrlSocket = baseUrlSocket;
    }

    private Observable.Transformer ioToMainThreadSchedulerTransformer
            = createIOMainThreadScheduler();

    private <T> Observable.Transformer<T, T> createIOMainThreadScheduler() {
        return tObservable -> tObservable.subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @SuppressWarnings("unchecked")
    private <T> Observable.Transformer<T, T> applyIOMainThreadSchedulers() {
        return ioToMainThreadSchedulerTransformer;
    }

    @SuppressWarnings("unchecked")
    private <T> void execute(Observable<? extends T> o, Subscriber<T> s) {
        o.compose((Observable.Transformer<T, T>) applyIOMainThreadSchedulers())
                .flatMap((Func1<T, Observable<T>>) this::validateResponse)
                .retryWhen(o1 -> o1.flatMap((Func1<Throwable, Observable<?>>) this::validateException))
                .subscribe(s);
    }

    @SuppressWarnings("unchecked")
    private <T> void executeWithoutBaseResponse(Observable<? extends T> o, Subscriber<T> s) {
        o.compose((Observable.Transformer<T, T>) applyIOMainThreadSchedulers()).subscribe(s);
    }

    private <T> Observable validateResponse(T t) {
        TAPBaseResponse br = (TAPBaseResponse) t;

        int code = br.getStatus();
        if (BuildConfig.DEBUG && code != RESPONSE_SUCCESS)
            Log.d(TAG, "validateResponse: XX HAS ERROR XX: __error_code:" + code);

        if (code == RESPONSE_SUCCESS && BuildConfig.DEBUG)
            Log.d(TAG, "validateResponse: √√ NO ERROR √√");
        else if (code == UNAUTHORIZED && 0 < isShouldRefreshToken && !isLogout) {
            return raiseApiRefreshTokenRunningException();
        } else if (code == UNAUTHORIZED && !isLogout) {
            isShouldRefreshToken++;
            return raiseApiSessionExpiredException(br);
        }
        isShouldRefreshToken = 0;
        return Observable.just(t);
    }

    private Observable validateException(Throwable t) {
        Log.e(TAG, "call: retryWhen(), cause: " + t.getMessage());
        return (t instanceof TAPApiSessionExpiredException && 1 == isShouldRefreshToken && !isLogout) ? refreshToken() :
                ((t instanceof TAPApiRefreshTokenRunningException || (t instanceof TAPApiSessionExpiredException && 1 < isShouldRefreshToken)) && !isLogout) ?
                        Observable.just(Boolean.TRUE) : Observable.error(t);
    }

    private Observable<Throwable> raiseApiSessionExpiredException(TAPBaseResponse br) {
        return Observable.error(new TAPApiSessionExpiredException(br.getError().getMessage()));
    }

    private Observable<Throwable> raiseApiRefreshTokenRunningException() {
        return Observable.error(new TAPApiRefreshTokenRunningException());
    }

    private void updateSession(TAPBaseResponse<TAPGetAccessTokenResponse> r) {
        TAPDataManager.getInstance().saveAccessToken(r.getData().getAccessToken());
        TAPDataManager.getInstance().saveAccessTokenExpiry(r.getData().getAccessTokenExpiry());
        TAPDataManager.getInstance().saveRefreshToken(r.getData().getRefreshToken());
        TAPDataManager.getInstance().saveRefreshTokenExpiry(r.getData().getRefreshTokenExpiry());

        TAPDataManager.getInstance().saveActiveUser(r.getData().getUser());
    }

    public void getAuthTicket(String ipAddress, String userAgent, String userPlatform, String userDeviceID, String xcUserID
            , String fullname, String email, String phone, String username, Subscriber<TAPBaseResponse<TAPAuthTicketResponse>> subscriber) {
        TAPAuthTicketRequest request = TAPAuthTicketRequest.toBuilder(ipAddress, userAgent, userPlatform, userDeviceID, xcUserID,
                fullname, email, phone, username);
        execute(homingPigeon.getAuthTicket(request), subscriber);
    }

    public void sendCustomMessage(Integer messageType, String body, String filterID, String senderUserID, String recipientUserID, Subscriber<TAPBaseResponse<TAPSendCustomMessageResponse>> subscriber) {
        TAPSendCustomMessageRequest request = new TAPSendCustomMessageRequest(messageType, body, filterID, senderUserID, recipientUserID);
        execute(homingPigeon.sendCustomMessage(request), subscriber);
    }

    public void getAccessToken(Subscriber<TAPBaseResponse<TAPGetAccessTokenResponse>> subscriber) {
        execute(homingPigeon.getAccessToken("Bearer " + TAPDataManager.getInstance().getAuthTicket()), subscriber);
    }

    public void requestOTPLogin(String loginMethod, int countryID, String phone, Subscriber<TAPBaseResponse<TAPLoginOTPResponse>> subscriber) {
        TAPLoginOTPRequest request = new TAPLoginOTPRequest(loginMethod, countryID, phone);
        execute(homingPigeon.requestOTPLogin(request), subscriber);
    }

    public void verifyingOTPLogin(long otpID, String otpKey, String otpCode, Subscriber<TAPBaseResponse<TAPLoginOTPVerifyResponse>> subscriber) {
        TAPLoginOTPVerifyRequest request = new TAPLoginOTPVerifyRequest(otpID, otpKey, otpCode);
        execute(homingPigeon.verifyingOTPLogin(request), subscriber);
    }

    public Observable<TAPBaseResponse<TAPGetAccessTokenResponse>> refreshToken() {
        return hpRefresh.refreshAccessToken("Bearer " + TAPDataManager.getInstance().getRefreshToken())
                .compose(this.applyIOMainThreadSchedulers())
                .doOnNext(response -> {
                    if (RESPONSE_SUCCESS == response.getStatus()) {
                        updateSession(response);
                        Observable.error(new TAPAuthException(response.getError().getMessage()));
                    } else if (UNAUTHORIZED == response.getStatus()) {
                        AnalyticsManager.getInstance().trackErrorEvent("Refresh Token Failed", response.getError().getCode(), response.getError().getMessage());
                        TapTalk.clearAllTapTalkData();
                        for (TapListener listener : TapTalk.getTapTalkListeners()) {
                            listener.onTapTalkRefreshTokenExpired();
                        }
                    } else {
                        Observable.error(new TAPAuthException(response.getError().getMessage()));
                    }
                }).doOnError(throwable -> {

                });
    }

    public void refreshAccessToken(Subscriber<TAPBaseResponse<TAPGetAccessTokenResponse>> subscriber) {
        execute(hpRefresh.refreshAccessToken("Bearer " + TAPDataManager.getInstance().getRefreshToken()), subscriber);
    }

    public void validateAccessToken(Subscriber<TAPBaseResponse<TAPErrorModel>> subscriber) {
        execute(hpSocket.validateAccessToken(), subscriber);
    }

    public void registerFcmTokenToServer(String fcmToken, Subscriber<TAPBaseResponse<TAPCommonResponse>> subscriber) {
        TAPPushNotificationRequest request = TAPPushNotificationRequest.Builder(fcmToken);
        execute(homingPigeon.registerFcmTokenToServer(request), subscriber);
    }

    public void getRoomList(String userID, Subscriber<TAPBaseResponse<TAPGetRoomListResponse>> subscriber) {
        TAPCommonRequest request = TAPCommonRequest.builderWithUserID(userID);
        execute(homingPigeon.getRoomList(request), subscriber);
    }

    public void getPendingAndUpdatedMessage(Subscriber<TAPBaseResponse<TAPGetRoomListResponse>> subscriber) {
        execute(homingPigeon.getPendingAndUpdatedMessage(), subscriber);
    }

    public void getMessageListByRoomAfter(String roomID, Long minCreated, Long lastUpdated, Subscriber<TAPBaseResponse<TAPGetMessageListByRoomResponse>> subscriber) {
        TAPGetMessageListByRoomAfterRequest request = new TAPGetMessageListByRoomAfterRequest(roomID, minCreated, lastUpdated);
        execute(homingPigeon.getMessageListByRoomAfter(request), subscriber);
    }

    public void deleteMessages(String roomID, List<String> messageIds, boolean isForEveryone) {
        TAPDeleteMessageRequest request = new TAPDeleteMessageRequest(roomID, messageIds, isForEveryone);
    }

    public void getMessageListByRoomBefore(String roomID, Long maxCreated, Integer limit, Subscriber<TAPBaseResponse<TAPGetMessageListByRoomResponse>> subscriber) {
        TAPGetMessageListByRoomBeforeRequest request = new TAPGetMessageListByRoomBeforeRequest(roomID, maxCreated, limit);
        execute(homingPigeon.getMessageListByRoomBefore(request), subscriber);
    }

    public void updateMessageStatusAsDelivered(List<String> messageIDs, Subscriber<TAPBaseResponse<TAPUpdateMessageStatusResponse>> subscriber) {
        TAPUpdateMessageStatusRequest request = new TAPUpdateMessageStatusRequest(messageIDs);
        execute(homingPigeon.updateMessageStatusAsDelivered(request), subscriber);
    }

    public void updateMessageStatusAsRead(List<String> messageIDs, Subscriber<TAPBaseResponse<TAPUpdateMessageStatusResponse>> subscriber) {
        TAPUpdateMessageStatusRequest request = new TAPUpdateMessageStatusRequest(messageIDs);
        execute(homingPigeon.updateMessageStatusAsRead(request), subscriber);
    }

    public void deleteMessagesAPI(String roomID, List<String> messageIDs, boolean isForEveryone, Subscriber<TAPBaseResponse<TAPDeleteMessageResponse>> subscriber) {
        TAPDeleteMessageRequest request = new TAPDeleteMessageRequest(roomID, messageIDs, isForEveryone);
        execute(homingPigeon.deleteMessages(request), subscriber);
    }

    public void getMyContactListFromAPI(Subscriber<TAPBaseResponse<TAPContactResponse>> subscriber) {
        execute(homingPigeon.getMyContactListFromAPI(), subscriber);
    }

    public void addContact(String userID, Subscriber<TAPBaseResponse<TAPAddContactResponse>> subscriber) {
        TAPUserIdRequest request = new TAPUserIdRequest(userID);
        execute(homingPigeon.addContact(request), subscriber);
    }

    public void removeContact(String userID, Subscriber<TAPBaseResponse<TAPCommonResponse>> subscriber) {
        TAPUserIdRequest request = new TAPUserIdRequest(userID);
        execute(homingPigeon.removeContact(request), subscriber);
    }

    public void getUserByID(String id, Subscriber<TAPBaseResponse<TAPGetUserResponse>> subscriber) {
        TAPGetUserByIdRequest request = new TAPGetUserByIdRequest(id);
        execute(homingPigeon.getUserByID(request), subscriber);
    }

    public void getUserByXcUserID(String xcUserID, Subscriber<TAPBaseResponse<TAPGetUserResponse>> subscriber) {
        TAPGetUserByXcUserIdRequest request = new TAPGetUserByXcUserIdRequest(xcUserID);
        execute(homingPigeon.getUserByXcUserID(request), subscriber);
    }

    public void getUserByUsername(String username, boolean ignoreCase, Subscriber<TAPBaseResponse<TAPGetUserResponse>> subscriber) {
        TAPGetUserByUsernameRequest request = new TAPGetUserByUsernameRequest(username, ignoreCase);
        execute(homingPigeon.getUserByUsername(request), subscriber);
    }

    public void getMultipleUserByID(List<String> ids, Subscriber<TAPBaseResponse<TAPGetMultipleUserResponse>> subscriber) {
        TAPGetMultipleUserByIdRequest request = new TAPGetMultipleUserByIdRequest(ids);
        execute(homingPigeon.getMultipleUserByID(request), subscriber);
    }

    public void uploadImage(File imageFile, String roomID, String caption, String mimeType,
                            ProgressRequestBody.UploadCallbacks uploadCallback,
                            Subscriber<TAPBaseResponse<TAPUploadFileResponse>> subscriber) {
        //RequestBody reqFile = RequestBody.create(MediaType.parse(mimeType), fileImage);
        TAPTalkMultipartApiService tapMultipart = TAPApiConnection.getInstance().getTapMultipart(calculateTimeOutTimeWithFileSize(imageFile.length()));
        ProgressRequestBody reqFile = new ProgressRequestBody(imageFile, mimeType, uploadCallback);
        String extension = imageFile.getAbsolutePath().substring(imageFile.getAbsolutePath().lastIndexOf("."));

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("roomID", roomID)
                .addFormDataPart("file", System.currentTimeMillis() + extension, reqFile)
                .addFormDataPart("caption", caption)
                .addFormDataPart("fileType", "image")
                .build();
        execute(tapMultipart.uploadFile(requestBody), subscriber);
    }

    public void uploadVideo(File videoFile, String roomID, String caption, String mimeType,
                            ProgressRequestBody.UploadCallbacks uploadCallback,
                            Subscriber<TAPBaseResponse<TAPUploadFileResponse>> subscriber) {
        TAPTalkMultipartApiService tapMultipart = TAPApiConnection.getInstance().getTapMultipart(calculateTimeOutTimeWithFileSize(videoFile.length()));
        ProgressRequestBody reqFile = new ProgressRequestBody(videoFile, mimeType, uploadCallback);
        String extension = videoFile.getAbsolutePath().substring(videoFile.getAbsolutePath().lastIndexOf("."));

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("roomID", roomID)
                .addFormDataPart("file", System.currentTimeMillis() + extension, reqFile)
                .addFormDataPart("caption", caption)
                .addFormDataPart("fileType", "video")
                .build();
        execute(tapMultipart.uploadFile(requestBody), subscriber);
    }

    public void uploadFile(File file, String roomID, String mimeType,
                           ProgressRequestBody.UploadCallbacks uploadCallback,
                           Subscriber<TAPBaseResponse<TAPUploadFileResponse>> subscriber) {
        TAPTalkMultipartApiService tapMultipart = TAPApiConnection.getInstance().getTapMultipart(calculateTimeOutTimeWithFileSize(file.length()));
        ProgressRequestBody reqFile = new ProgressRequestBody(file, mimeType, uploadCallback);
        String extension = file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf("."));

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("roomID", roomID)
                .addFormDataPart("file", System.currentTimeMillis() + extension, reqFile)
                .addFormDataPart("fileType", "file")
                .build();
        execute(tapMultipart.uploadFile(requestBody), subscriber);
    }

    public void uploadProfilePicture(File imageFile, String mimeType,
                                     ProgressRequestBody.UploadCallbacks uploadCallback,
                                     Subscriber<TAPBaseResponse<TAPGetUserResponse>> subscriber) {
        TAPTalkMultipartApiService tapMultipart = TAPApiConnection.getInstance().getTapMultipart(calculateTimeOutTimeWithFileSize(imageFile.length()));
        ProgressRequestBody reqFile = new ProgressRequestBody(imageFile, mimeType, uploadCallback);
        String extension = imageFile.getAbsolutePath().substring(imageFile.getAbsolutePath().lastIndexOf("."));

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", System.currentTimeMillis() + extension, reqFile)
                .addFormDataPart("fileType", "image")
                .build();
        execute(tapMultipart.uploadProfilePicture(requestBody), subscriber);
    }

    public void uploadGroupPicture(File imageFile, String mimeType, String roomID,
                                   Subscriber<TAPBaseResponse<TAPUpdateRoomResponse>> subscriber) {
        TAPTalkMultipartApiService tapMultipart = TAPApiConnection.getInstance().getTapMultipart(calculateTimeOutTimeWithFileSize(imageFile.length()));
        String extension = imageFile.getAbsolutePath().substring(imageFile.getAbsolutePath().lastIndexOf("."));

        ProgressRequestBody reqFile = new ProgressRequestBody(imageFile, mimeType);
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", System.currentTimeMillis() + extension, reqFile)
                .addFormDataPart("roomID", roomID)
                .build();
        execute(tapMultipart.uploadRoomPicture(requestBody), subscriber);
    }

    public void downloadFile(String roomID, String localID, String fileID, @Nullable Number fileSize, Subscriber<ResponseBody> subscriber) {
        TAPTalkDownloadApiService tapDownload;
        if (null != fileSize) {
            tapDownload = TAPApiConnection.getInstance().getTapDownload(calculateTimeOutTimeWithFileSize(fileSize.longValue()));
        } else tapDownload = TAPApiConnection.getInstance().getTapDownload(30 * 60 * 1000);
        TAPFileDownloadRequest request = new TAPFileDownloadRequest(roomID, fileID);
        executeWithoutBaseResponse(tapDownload.downloadFile(request, request.getRoomID(), localID), subscriber);
    }

    public void getCountryList(Subscriber<TAPBaseResponse<TAPCountryListResponse>> subscriber) {
        execute(homingPigeon.getCountryList(), subscriber);
    }

    public void register(String fullName, String username, Integer countryID, String phone, String email, String password, Subscriber<TAPBaseResponse<TAPRegisterResponse>> subscriber) {
        TAPRegisterRequest request = new TAPRegisterRequest(fullName, username, countryID, phone, email, password);
        execute(homingPigeon.register(request), subscriber);
    }

    public void logout(Subscriber<TAPBaseResponse<TAPCommonResponse>> subscriber) {
        execute(homingPigeon.logout(), subscriber);
    }

    public void checkUsernameExists(String username, Subscriber<TAPBaseResponse<TAPCheckUsernameResponse>> subscriber) {
        TAPGetUserByUsernameRequest request = new TAPGetUserByUsernameRequest(username, false);
        execute(homingPigeon.checkUsernameExists(request), subscriber);
    }

    public void addContactByPhone(List<String> phones, Subscriber<TAPBaseResponse<TAPAddContactByPhoneResponse>> subscriber) {
        TAPAddContactByPhoneRequest request = new TAPAddContactByPhoneRequest(phones);
        execute(homingPigeon.addContactByPhone(request), subscriber);
    }

    public void createChatRoom(String roomName, int roomType, List<String> participantIDs, Subscriber<TAPBaseResponse<TAPCreateRoomResponse>> subscriber) {
        TAPCreateRoomRequest request = new TAPCreateRoomRequest(roomName, roomType, participantIDs);
        execute(homingPigeon.createChatRoom(request), subscriber);
    }

    public void getChatRoomData(String roomID, Subscriber<TAPBaseResponse<TAPCreateRoomResponse>> subscriber) {
        TAPCommonRequest request = TAPCommonRequest.builderWithRoomID(roomID);
        execute(homingPigeon.getChatRoomData(request), subscriber);
    }

    public void getChatRoomByXcRoomID(String xcRoomID, Subscriber<TAPBaseResponse<TAPCreateRoomResponse>> subscriber) {
        TAPGetRoomByXcRoomIDRequest request = new TAPGetRoomByXcRoomIDRequest(xcRoomID);
        execute(homingPigeon.getChatRoomByXcRoomID(request), subscriber);
    }

    public void updateChatRoom(String roomID, String roomName, Subscriber<TAPBaseResponse<TAPUpdateRoomResponse>> subscriber) {
        TAPUpdateRoomRequest request = new TAPUpdateRoomRequest(roomID, roomName);
        execute(homingPigeon.updateChatRoom(request), subscriber);
    }

    public void addRoomParticipant(String roomID, List<String> userIDs, Subscriber<TAPBaseResponse<TAPCreateRoomResponse>> subscriber) {
        TAPAddRoomParticipantRequest request = new TAPAddRoomParticipantRequest(roomID, userIDs);
        execute(homingPigeon.addRoomParticipant(request), subscriber);
    }

    public void removeRoomParticipant(String roomID, List<String> userIDs, Subscriber<TAPBaseResponse<TAPCreateRoomResponse>> subscriber) {
        TAPAddRoomParticipantRequest request = new TAPAddRoomParticipantRequest(roomID, userIDs);
        execute(homingPigeon.removeRoomParticipant(request), subscriber);
    }

    public void leaveChatRoom(String roomID, Subscriber<TAPBaseResponse<TAPCommonResponse>> subscriber) {
        TAPCommonRequest request = TAPCommonRequest.builderWithRoomID(roomID);
        execute(homingPigeon.leaveChatRoom(request), subscriber);
    }

    public void promoteGroupAdmins(String roomID, List<String> userIDs, Subscriber<TAPBaseResponse<TAPCreateRoomResponse>> subscriber) {
        TAPAddRoomParticipantRequest request = new TAPAddRoomParticipantRequest(roomID, userIDs);
        execute(homingPigeon.promoteGroupAdmins(request), subscriber);
    }

    public void demoteGroupAdmins(String roomID, List<String> userIDs, Subscriber<TAPBaseResponse<TAPCreateRoomResponse>> subscriber) {
        TAPAddRoomParticipantRequest request = new TAPAddRoomParticipantRequest(roomID, userIDs);
        execute(homingPigeon.demoteGroupAdmins(request), subscriber);
    }

    public void deleteChatRoom(TAPRoomModel room, String userID, long accessTokenExpiry, Subscriber<TAPBaseResponse<TAPCommonResponse>> subscriber) {
        String checksum = room.getRoomID() + ":" + room.getRoomType() + ":" + userID + ":" + accessTokenExpiry;
        TAPDeleteRoomRequest request = new TAPDeleteRoomRequest(room.getRoomID(), TAPEncryptorManager.getInstance().md5(checksum));
        execute(homingPigeon.deleteChatRoom(request), subscriber);
    }

    public void getProjectConfig(Subscriber<TAPBaseResponse<TapConfigs>> subscriber) {
        execute(homingPigeon.getProjectConfig(), subscriber);
    }
}
