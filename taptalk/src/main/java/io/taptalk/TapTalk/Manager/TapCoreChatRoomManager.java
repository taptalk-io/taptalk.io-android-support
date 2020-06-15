package io.taptalk.TapTalk.Manager;

import android.net.Uri;
import android.support.annotation.Keep;

import java.util.ArrayList;
import java.util.List;

import io.taptalk.TapTalk.API.View.TAPDefaultDataView;
import io.taptalk.TapTalk.Helper.TapTalk;
import io.taptalk.TapTalk.Listener.TAPChatListener;
import io.taptalk.TapTalk.Listener.TAPDatabaseListener;
import io.taptalk.TapTalk.Listener.TapCommonListener;
import io.taptalk.TapTalk.Listener.TapCoreCreateGroupWithPictureListener;
import io.taptalk.TapTalk.Listener.TapCoreGetRoomListener;
import io.taptalk.TapTalk.Listener.TapCoreChatRoomListener;
import io.taptalk.TapTalk.Model.ResponseModel.TAPCommonResponse;
import io.taptalk.TapTalk.Model.ResponseModel.TAPCreateRoomResponse;
import io.taptalk.TapTalk.Model.ResponseModel.TAPGetUserResponse;
import io.taptalk.TapTalk.Model.ResponseModel.TAPUpdateRoomResponse;
import io.taptalk.TapTalk.Model.TAPErrorModel;
import io.taptalk.TapTalk.Model.TAPOnlineStatusModel;
import io.taptalk.TapTalk.Model.TAPRoomModel;
import io.taptalk.TapTalk.Model.TAPTypingModel;
import io.taptalk.TapTalk.Model.TAPUserModel;

import static io.taptalk.TapTalk.Const.TAPDefaultConstant.ClientErrorCodes.ERROR_CODE_ACTIVE_USER_NOT_FOUND;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.ClientErrorCodes.ERROR_CODE_ADMIN_REQUIRED;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.ClientErrorCodes.ERROR_CODE_GROUP_DELETED;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.ClientErrorCodes.ERROR_CODE_OTHERS;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.ClientErrorMessages.ERROR_MESSAGE_ACTIVE_USER_NOT_FOUND;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.ClientErrorMessages.ERROR_MESSAGE_ADMIN_REQUIRED;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.ClientErrorMessages.ERROR_MESSAGE_GROUP_DELETED;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.ClientSuccessMessages.SUCCESS_MESSAGE_DELETE_GROUP;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.ClientSuccessMessages.SUCCESS_MESSAGE_LEAVE_GROUP;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.RoomType.TYPE_PERSONAL;

@Keep
public class TapCoreChatRoomManager {

    private static TapCoreChatRoomManager instance;

    private List<TapCoreChatRoomListener> coreChatRoomListeners;
    private TAPChatListener chatListener;

    public static TapCoreChatRoomManager getInstance() {
        return null == instance ? instance = new TapCoreChatRoomManager() : instance;
    }

    private List<TapCoreChatRoomListener> getCoreChatRoomListeners() {
        return null == coreChatRoomListeners ? coreChatRoomListeners = new ArrayList<>() : coreChatRoomListeners;
    }

    public void addChatRoomListener(TapCoreChatRoomListener listener) {
        if (!TapTalk.checkTapTalkInitialized()) {
            return;
        }
        if (getCoreChatRoomListeners().isEmpty()) {
            if (null == chatListener) {
                chatListener = new TAPChatListener() {
                    @Override
                    public void onReceiveStartTyping(TAPTypingModel typingModel) {
                        for (TapCoreChatRoomListener listener : getCoreChatRoomListeners()) {
                            if (null != listener) {
                                listener.onReceiveStartTyping(typingModel.getRoomID(), typingModel.getUser());
                            }
                        }
                    }

                    @Override
                    public void onReceiveStopTyping(TAPTypingModel typingModel) {
                        for (TapCoreChatRoomListener listener : getCoreChatRoomListeners()) {
                            if (null != listener) {
                                listener.onReceiveStopTyping(typingModel.getRoomID(), typingModel.getUser());
                            }
                        }
                    }

                    @Override
                    public void onUserOnlineStatusUpdate(TAPOnlineStatusModel onlineStatus) {
                        for (TapCoreChatRoomListener listener : getCoreChatRoomListeners()) {
                            if (null != listener) {
                                listener.onReceiveOnlineStatus(onlineStatus.getUser(), onlineStatus.getOnline(), onlineStatus.getLastActive());
                            }
                        }
                    }
                };
            }
            TAPChatManager.getInstance().addChatListener(chatListener);
        }
        getCoreChatRoomListeners().add(listener);
    }

    public void removeChatRoomListener(TapCoreChatRoomListener listener) {
        if (!TapTalk.checkTapTalkInitialized()) {
            return;
        }
        getCoreChatRoomListeners().remove(listener);
        if (getCoreChatRoomListeners().isEmpty()) {
            TAPChatManager.getInstance().removeChatListener(chatListener);
        }
    }

    public void getPersonalChatRoom(TAPUserModel recipientUser, TapCoreGetRoomListener listener) {
        if (!TapTalk.checkTapTalkInitialized()) {
            return;
        }
        if (null == TAPChatManager.getInstance().getActiveUser()) {
            if (null != listener) {
                listener.onError(ERROR_CODE_ACTIVE_USER_NOT_FOUND, ERROR_MESSAGE_ACTIVE_USER_NOT_FOUND);
            }
        }
        try {
            TAPRoomModel roomModel = TAPRoomModel.Builder(
                    TAPChatManager.getInstance().arrangeRoomId(
                            TAPChatManager.getInstance().getActiveUser().getUserID(),
                            recipientUser.getUserID()),
                    recipientUser.getName(),
                    TYPE_PERSONAL,
                    recipientUser.getAvatarURL(),
                    "");
            if (null != listener) {
                listener.onSuccess(roomModel);
            }
        } catch (Exception e) {
            if (null != listener) {
                listener.onError(ERROR_CODE_OTHERS, e.getMessage());
            }
        }
    }

    public void getPersonalChatRoom(String recipientUserID, TapCoreGetRoomListener listener) {
        if (!TapTalk.checkTapTalkInitialized()) {
            return;
        }
        if (null == TAPChatManager.getInstance().getActiveUser()) {
            if (null != listener) {
                listener.onError(ERROR_CODE_ACTIVE_USER_NOT_FOUND, ERROR_MESSAGE_ACTIVE_USER_NOT_FOUND);
            }
        }
        TAPUserModel recipient = TAPContactManager.getInstance().getUserData(recipientUserID);
        if (null == recipient) {
            TAPDataManager.getInstance().getUserByIdFromApi(recipientUserID, new TAPDefaultDataView<TAPGetUserResponse>() {
                @Override
                public void onSuccess(TAPGetUserResponse response) {
                    getPersonalChatRoom(response.getUser(), listener);
                }

                @Override
                public void onError(TAPErrorModel error) {
                    if (null != listener) {
                        listener.onError(error.getCode(), error.getMessage());
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    if (null != listener) {
                        listener.onError(ERROR_CODE_OTHERS, errorMessage);
                    }
                }
            });
        } else {
            getPersonalChatRoom(recipient, listener);
        }
    }

    public void createGroupChatRoom(String groupName, List<String> participantUserIDs, TapCoreGetRoomListener listener) {
        if (!TapTalk.checkTapTalkInitialized()) {
            return;
        }
        TAPDataManager.getInstance().createGroupChatRoom(groupName, participantUserIDs, new TAPDefaultDataView<TAPCreateRoomResponse>() {
            @Override
            public void onSuccess(TAPCreateRoomResponse response) {
                TAPRoomModel room = TAPGroupManager.Companion.getGetInstance().updateGroupDataFromResponse(response);
                if (null != listener) {
                    listener.onSuccess(room);
                }
            }

            @Override
            public void onError(TAPErrorModel error) {
                if (null != listener) {
                    listener.onError(error.getCode(), error.getMessage());
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (null != listener) {
                    listener.onError(ERROR_CODE_OTHERS, errorMessage);
                }
            }
        });
    }

    public void createGroupChatRoomWithPicture(String groupName, List<String> participantUserIDs, Uri groupPictureUri, TapCoreCreateGroupWithPictureListener listener) {
        if (!TapTalk.checkTapTalkInitialized()) {
            return;
        }
        TAPDataManager.getInstance().createGroupChatRoom(groupName, participantUserIDs, new TAPDefaultDataView<TAPCreateRoomResponse>() {
            @Override
            public void onSuccess(TAPCreateRoomResponse response) {
                if (null == response.getRoom()) {
                    if (null != listener) {
                        listener.onSuccess(response.getRoom(), false);
                    }
                } else {
                    TAPGroupManager.Companion.getGetInstance().updateGroupDataFromResponse(response);
                    TAPFileUploadManager.getInstance().uploadRoomPicture(TapTalk.appContext,
                            groupPictureUri, response.getRoom().getRoomID(), new TAPDefaultDataView<TAPUpdateRoomResponse>() {
                                @Override
                                public void onSuccess(TAPUpdateRoomResponse response) {
                                    TAPRoomModel room = TAPGroupManager.Companion.getGetInstance().updateGroupDataFromResponse(response);
                                    if (null != listener) {
                                        listener.onSuccess(room, true);
                                    }
                                }

                                @Override
                                public void onError(TAPErrorModel error) {
                                    if (null != listener) {
                                        listener.onSuccess(response.getRoom(), false);
                                    }
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    if (null != listener) {
                                        listener.onSuccess(response.getRoom(), false);
                                    }
                                }
                            });
                }
            }

            @Override
            public void onError(TAPErrorModel error) {
                if (null != listener) {
                    listener.onError(error.getCode(), error.getMessage());
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (null != listener) {
                    listener.onError(ERROR_CODE_OTHERS, errorMessage);
                }
            }
        });
    }

    public void updateGroupPicture(String groupRoomID, Uri groupPictureUri, TapCoreGetRoomListener listener) {
        if (!TapTalk.checkTapTalkInitialized()) {
            return;
        }
        TAPFileUploadManager.getInstance().uploadRoomPicture(TapTalk.appContext,
                groupPictureUri, groupRoomID, new TAPDefaultDataView<TAPUpdateRoomResponse>() {
                    @Override
                    public void onSuccess(TAPUpdateRoomResponse response) {
                        TAPRoomModel room = TAPGroupManager.Companion.getGetInstance().updateGroupDataFromResponse(response);
                        if (null != listener) {
                            listener.onSuccess(room);
                        }
                    }

                    @Override
                    public void onError(TAPErrorModel error) {
                        if (null != listener) {
                            listener.onError(error.getCode(), error.getMessage());
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (null != listener) {
                            listener.onError(ERROR_CODE_OTHERS, errorMessage);
                        }
                    }
                });
    }

    public void updateGroupChatRoom(String groupRoomID, String groupName, TapCoreGetRoomListener listener) {
        if (!TapTalk.checkTapTalkInitialized()) {
            return;
        }
        TAPDataManager.getInstance().updateChatRoom(groupRoomID, groupName,
                new TAPDefaultDataView<TAPUpdateRoomResponse>() {
                    @Override
                    public void onSuccess(TAPUpdateRoomResponse response) {
                        TAPRoomModel room = TAPGroupManager.Companion.getGetInstance().updateGroupDataFromResponse(response);
                        if (null != listener) {
                            listener.onSuccess(room);
                        }
                    }

                    @Override
                    public void onError(TAPErrorModel error) {
                        if (null != listener) {
                            listener.onError(error.getCode(), error.getMessage());
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (null != listener) {
                            listener.onError(ERROR_CODE_OTHERS, errorMessage);
                        }
                    }
                });
    }

    public void getGroupChatRoom(String groupRoomID, TapCoreGetRoomListener listener) {
        if (!TapTalk.checkTapTalkInitialized()) {
            return;
        }
        TAPRoomModel roomModel = TAPGroupManager.Companion.getGetInstance().getGroupData(groupRoomID);
        if (null == roomModel) {
            TAPDataManager.getInstance().getChatRoomData(groupRoomID, new TAPDefaultDataView<TAPCreateRoomResponse>() {
                @Override
                public void onSuccess(TAPCreateRoomResponse response) {
                    TAPRoomModel room = TAPGroupManager.Companion.getGetInstance().updateGroupDataFromResponse(response);
                    if (null != listener) {
                        listener.onSuccess(room);
                    }
                }

                @Override
                public void onError(TAPErrorModel error) {
                    if (null != listener) {
                        listener.onError(error.getCode(), error.getMessage());
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    if (null != listener) {
                        listener.onError(ERROR_CODE_OTHERS, errorMessage);
                    }
                }
            });
        } else {
            if (null != listener) {
                listener.onSuccess(roomModel);
            }
        }
    }

    public void getChatRoomByXcRoomID(String xcRoomID, TapCoreGetRoomListener listener) {
        if (!TapTalk.checkTapTalkInitialized()) {
            return;
        }
        TAPRoomModel roomModel = TAPGroupManager.Companion.getGetInstance().getGroupData(xcRoomID);
        if (null == roomModel) {
            TAPDataManager.getInstance().getChatRoomByXcRoomID(xcRoomID, new TAPDefaultDataView<TAPCreateRoomResponse>() {
                @Override
                public void onSuccess(TAPCreateRoomResponse response) {
                    TAPRoomModel room = TAPGroupManager.Companion.getGetInstance().updateGroupDataFromResponse(response);
                    if (null != listener) {
                        listener.onSuccess(room);
                    }
                }

                @Override
                public void onError(TAPErrorModel error) {
                    if (null != listener) {
                        listener.onError(error.getCode(), error.getMessage());
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    if (null != listener) {
                        listener.onError(ERROR_CODE_OTHERS, errorMessage);
                    }
                }
            });
        } else {
            if (null != listener) {
                listener.onSuccess(roomModel);
            }
        }
    }

    public void deleteGroupChatRoom(TAPRoomModel groupChatRoomModel, TapCommonListener listener) {
        if (!TapTalk.checkTapTalkInitialized()) {
            return;
        }
        TAPDataManager.getInstance().deleteChatRoom(groupChatRoomModel, new TAPDefaultDataView<TAPCommonResponse>() {
            @Override
            public void onSuccess(TAPCommonResponse tapCommonResponse, String localID) {
                if (tapCommonResponse.getSuccess()) {
                    TAPOldDataManager.getInstance().cleanRoomPhysicalData(groupChatRoomModel.getRoomID(), new TAPDatabaseListener() {
                        @Override
                        public void onDeleteFinished() {
                            TAPDataManager.getInstance().deleteMessageByRoomId(groupChatRoomModel.getRoomID(), new TAPDatabaseListener() {
                                @Override
                                public void onDeleteFinished() {
                                    TAPGroupManager.Companion.getGetInstance().removeGroupData(groupChatRoomModel.getRoomID());
                                    if (null != listener) {
                                        listener.onSuccess(SUCCESS_MESSAGE_DELETE_GROUP);
                                    }
                                }
                            });
                        }
                    });
                } else {
                    if (null != listener) {
                        listener.onError(ERROR_CODE_GROUP_DELETED, ERROR_MESSAGE_GROUP_DELETED);
                    }
                }
            }

            @Override
            public void onError(TAPErrorModel error) {
                if (null != listener) {
                    listener.onError(error.getCode(), error.getMessage());
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (null != listener) {
                    listener.onError(ERROR_CODE_OTHERS, errorMessage);
                }
            }
        });
    }

    public void leaveGroupChatRoom(String groupRoomID, TapCommonListener listener) {
        if (!TapTalk.checkTapTalkInitialized()) {
            return;
        }
        TAPDataManager.getInstance().leaveChatRoom(groupRoomID, new TAPDefaultDataView<TAPCommonResponse>() {
            @Override
            public void onSuccess(TAPCommonResponse response) {
                if (response.getSuccess()) {
                    TAPOldDataManager.getInstance().cleanRoomPhysicalData(groupRoomID, new TAPDatabaseListener() {
                        @Override
                        public void onDeleteFinished() {
                            TAPDataManager.getInstance().deleteMessageByRoomId(groupRoomID, new TAPDatabaseListener() {
                                @Override
                                public void onDeleteFinished() {
                                    TAPGroupManager.Companion.getGetInstance().removeGroupData(groupRoomID);
                                    if (null != listener) {
                                        listener.onSuccess(SUCCESS_MESSAGE_LEAVE_GROUP);
                                    }
                                }
                            });
                        }
                    });
                } else {
                    if (null != listener) {
                        listener.onError(ERROR_CODE_ADMIN_REQUIRED, ERROR_MESSAGE_ADMIN_REQUIRED);
                    }
                }
            }

            @Override
            public void onError(TAPErrorModel error) {
                if (null != listener) {
                    listener.onError(error.getCode(), error.getMessage());
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (null != listener) {
                    listener.onError(ERROR_CODE_OTHERS, errorMessage);
                }
            }
        });
    }

    public void addGroupChatMembers(String groupRoomID, List<String> userIDs, TapCoreGetRoomListener listener) {
        if (!TapTalk.checkTapTalkInitialized()) {
            return;
        }
        TAPDataManager.getInstance().addRoomParticipant(groupRoomID, userIDs, new TAPDefaultDataView<TAPCreateRoomResponse>() {
            @Override
            public void onSuccess(TAPCreateRoomResponse response) {
                TAPRoomModel room = TAPGroupManager.Companion.getGetInstance().updateGroupDataFromResponse(response);
                if (null != listener) {
                    listener.onSuccess(room);
                }
            }

            @Override
            public void onError(TAPErrorModel error) {
                if (null != listener) {
                    listener.onError(error.getCode(), error.getMessage());
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (null != listener) {
                    listener.onError(ERROR_CODE_OTHERS, errorMessage);
                }
            }
        });
    }

    public void removeGroupChatMembers(String groupRoomID, List<String> userIDs, TapCoreGetRoomListener listener) {
        if (!TapTalk.checkTapTalkInitialized()) {
            return;
        }
        TAPDataManager.getInstance().removeRoomParticipant(groupRoomID, userIDs, new TAPDefaultDataView<TAPCreateRoomResponse>() {
            @Override
            public void onSuccess(TAPCreateRoomResponse response) {
                TAPRoomModel room = TAPGroupManager.Companion.getGetInstance().updateGroupDataFromResponse(response);
                if (null != listener) {
                    listener.onSuccess(room);
                }
            }

            @Override
            public void onError(TAPErrorModel error) {
                if (null != listener) {
                    listener.onError(error.getCode(), error.getMessage());
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (null != listener) {
                    listener.onError(ERROR_CODE_OTHERS, errorMessage);
                }
            }
        });
    }

    public void promoteGroupAdmins(String groupRoomID, List<String> userIDs, TapCoreGetRoomListener listener) {
        if (!TapTalk.checkTapTalkInitialized()) {
            return;
        }
        TAPDataManager.getInstance().promoteGroupAdmins(groupRoomID, userIDs, new TAPDefaultDataView<TAPCreateRoomResponse>() {
            @Override
            public void onSuccess(TAPCreateRoomResponse response) {
                TAPRoomModel room = TAPGroupManager.Companion.getGetInstance().updateGroupDataFromResponse(response);
                if (null != listener) {
                    listener.onSuccess(room);
                }
            }

            @Override
            public void onError(TAPErrorModel error) {
                if (null != listener) {
                    listener.onError(error.getCode(), error.getMessage());
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (null != listener) {
                    listener.onError(ERROR_CODE_OTHERS, errorMessage);
                }
            }
        });
    }

    public void demoteGroupAdmins(String groupRoomID, List<String> userIDs, TapCoreGetRoomListener listener) {
        if (!TapTalk.checkTapTalkInitialized()) {
            return;
        }
        TAPDataManager.getInstance().demoteGroupAdmins(groupRoomID, userIDs, new TAPDefaultDataView<TAPCreateRoomResponse>() {
            @Override
            public void onSuccess(TAPCreateRoomResponse response) {
                TAPRoomModel room = TAPGroupManager.Companion.getGetInstance().updateGroupDataFromResponse(response);
                if (null != listener) {
                    listener.onSuccess(room);
                }
            }

            @Override
            public void onError(TAPErrorModel error) {
                if (null != listener) {
                    listener.onError(error.getCode(), error.getMessage());
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (null != listener) {
                    listener.onError(ERROR_CODE_OTHERS, errorMessage);
                }
            }
        });
    }

    public void sendStartTypingEmit(String roomID) {
        if (!TapTalk.checkTapTalkInitialized()) {
            return;
        }
        TAPChatManager.getInstance().sendStartTypingEmit(roomID);
    }

    public void sendStopTypingEmit(String roomID) {
        if (!TapTalk.checkTapTalkInitialized()) {
            return;
        }
        TAPChatManager.getInstance().sendStopTypingEmit(roomID);
    }
}
