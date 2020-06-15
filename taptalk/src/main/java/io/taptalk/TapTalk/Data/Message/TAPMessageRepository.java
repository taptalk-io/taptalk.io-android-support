package io.taptalk.TapTalk.Data.Message;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.taptalk.TapTalk.Data.TapTalkDatabase;
import io.taptalk.TapTalk.Helper.TAPUtils;
import io.taptalk.TapTalk.Listener.TAPDatabaseListener;
import io.taptalk.TapTalk.Manager.TAPChatManager;
import io.taptalk.TapTalk.Model.TAPImageURL;
import io.taptalk.TapTalk.Model.TAPRoomModel;
import io.taptalk.TapTalk.Model.TAPUserModel;

import static io.taptalk.TapTalk.Const.TAPDefaultConstant.RoomType.TYPE_PERSONAL;

public class TAPMessageRepository {

    private static final String TAG = TAPMessageRepository.class.getSimpleName();
    private TAPMessageDao messageDao;
    private LiveData<List<TAPMessageEntity>> allMessages;
    private List<TAPMessageEntity> allMessageList = new ArrayList<>();

    public TAPMessageRepository(Application application) {
        TapTalkDatabase db = TapTalkDatabase.getDatabase(application);
        messageDao = db.messageDao();
        allMessages = messageDao.getAllMessageLiveData();
    }

    public void delete(List<TAPMessageEntity> messageEntities, TAPDatabaseListener listener) {
        new Thread(() -> {
            messageDao.delete(messageEntities);
            listener.onDeleteFinished();
        }).start();
    }

    public void deleteRoomMessageBeforeTimestamp(String roomID, long minimumTimestamp, TAPDatabaseListener listener) {
        new Thread(() -> {
            messageDao.deleteRoomMessageBeforeTimestamp(roomID, minimumTimestamp);
            listener.onDeleteFinished();
        }).start();
    }

    public void insert(TAPMessageEntity message) {
        new InsertAsyncTask(messageDao).execute(message);
    }

    private static class InsertAsyncTask extends AsyncTask<TAPMessageEntity, Void, Void> {
        private TAPMessageDao asyncTaskDao;

        InsertAsyncTask(TAPMessageDao chatDao) {
            asyncTaskDao = chatDao;
        }

        @Override
        protected Void doInBackground(TAPMessageEntity... messages) {
            asyncTaskDao.insert(messages[0]);
            return null;
        }
    }

    public void insert(List<TAPMessageEntity> messageEntities, boolean isClearSaveMessages) {
        new Thread(() -> {
            messageEntities.removeAll(Collections.singleton(null)); // Remove null objects from list
            if (messageEntities.isEmpty()) {
                return;
            }
            messageDao.insert(messageEntities);
            if (0 < TAPChatManager.getInstance().getSaveMessages().size() && isClearSaveMessages) {
                TAPChatManager.getInstance().clearSaveMessages();
            }
        }).start();
    }

    public void insert(List<TAPMessageEntity> messageEntities, boolean isClearSaveMessages, TAPDatabaseListener listener) {
        new Thread(() -> {
            messageEntities.removeAll(Collections.singleton(null)); // Remove null objects from list
            if (messageEntities.isEmpty()) {
                listener.onInsertFailed("Could not save messages, inserted list is either empty or null.");
            } else {
                messageDao.insert(messageEntities);
                if (0 < TAPChatManager.getInstance().getSaveMessages().size() && isClearSaveMessages) {
                    TAPChatManager.getInstance().clearSaveMessages();
                }
                listener.onInsertFinished();
            }
        }).start();
    }

    public LiveData<List<TAPMessageEntity>> getAllMessagesLiveData() {
        return allMessages;
    }

    public void getAllMessagesInRoom(String roomID, final TAPDatabaseListener<TAPMessageEntity> listener) {
        new Thread(() -> {
            List<TAPMessageEntity> allMessages = messageDao.getAllMessagesInRoom(roomID);
            listener.onSelectFinished(allMessages);
        }).start();
    }

    public void getMessageListDesc(final String roomID, final TAPDatabaseListener<TAPMessageEntity> listener) {
        new Thread(() -> {
            allMessageList = messageDao.getAllMessageListDesc(roomID);
            listener.onSelectFinished(allMessageList);
        }).start();
    }

    public void getMessageListDesc(final String roomID, final TAPDatabaseListener listener, final long lastTimestamp) {
        new Thread(() -> {
            List<TAPMessageEntity> entities = messageDao.getAllMessageTimeStamp(lastTimestamp, roomID);
            listener.onSelectFinished(entities);
        }).start();
    }

    public void getMessageListAsc(final String roomID, final TAPDatabaseListener listener) {
        new Thread(() -> {
            try {
                allMessageList = messageDao.getAllMessageListAsc(roomID);
                listener.onSelectFinished(allMessageList);
            } catch (Exception e) {
                listener.onSelectFailed(e.getMessage());
            }
        }).start();
    }

    public void searchAllMessages(String keyword, final TAPDatabaseListener listener) {
        new Thread(() -> {
            String queryKeyword = '%' + keyword
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_") + '%';
            List<TAPMessageEntity> entities = messageDao.searchAllMessages(queryKeyword);
            listener.onSelectFinished(entities);
        }).start();
    }

    public void getRoomList(String myID, List<TAPMessageEntity> saveMessages, boolean isCheckUnreadFirst, final TAPDatabaseListener listener) {
        new Thread(() -> {
            try {
                if (0 < saveMessages.size()) {
                    messageDao.insert(saveMessages);
                    TAPChatManager.getInstance().clearSaveMessages();
                }
                List<TAPMessageEntity> entities = messageDao.getAllRoomList();

                if (isCheckUnreadFirst && entities.size() > 0) {
                    Map<String, Integer> unreadMap = new LinkedHashMap<>();
                    for (TAPMessageEntity entity : entities) {
                        unreadMap.put(entity.getRoomID(), messageDao.getUnreadCount(myID, entity.getRoomID()));
                    }
                    listener.onSelectedRoomList(entities, unreadMap);
                } else listener.onSelectFinished(entities);
            } catch (Exception e) {
                listener.onSelectFailed(e.getMessage());
            }
        }).start();
    }

    public void getAllUnreadMessagesFromRoom(String myID, String roomID, TAPDatabaseListener<TAPMessageEntity> listener) {
        new Thread(() -> {
            List<TAPMessageEntity> messageEntities = messageDao.getAllUnreadMessagesFromRoom(myID, roomID);
            listener.onSelectFinished(messageEntities);
        }).start();
    }

    public void searchAllChatRooms(String myID, String keyword, final TAPDatabaseListener listener) {
        new Thread(() -> {
            String queryKeyword = '%' + keyword
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_") + '%';
            List<TAPMessageEntity> entities = messageDao.searchAllChatRooms(queryKeyword);
            Map<String, Integer> unreadMap = new LinkedHashMap<>();
            for (TAPMessageEntity entity : entities) {
                unreadMap.put(entity.getRoomID(), messageDao.getUnreadCount(myID, entity.getRoomID()));
            }
            listener.onSelectedRoomList(entities, unreadMap);
        }).start();
    }

    public void getRoomMedias(Long lastTimestamp, String roomID, final TAPDatabaseListener listener) {
        new Thread(() -> {
            List<TAPMessageEntity> roomMedias;
            if (lastTimestamp == 0L) {
                roomMedias = messageDao.getRoomMedias(roomID);
            } else {
                roomMedias = messageDao.getRoomMedias(lastTimestamp, roomID);
            }
            listener.onSelectFinished(roomMedias);
        }).start();
    }

    public void getRoomMediaMessageBeforeTimestamp(String roomID, long minimumTimestamp, final TAPDatabaseListener<TAPMessageEntity> listener) {
        new Thread(() -> {
            List<TAPMessageEntity> messages = messageDao.getRoomMediaMessageBeforeTimestamp(roomID, minimumTimestamp);
            listener.onSelectFinished(messages);
        }).start();
    }

    public void getRoomMediaMessage(String roomID, final TAPDatabaseListener<TAPMessageEntity> listener) {
        new Thread(() -> {
            List<TAPMessageEntity> messages = messageDao.getRoomMediaMessage(roomID);
            listener.onSelectFinished(messages);
        }).start();
    }

    public void getRoom(String myID, TAPUserModel otherUserModel, final TAPDatabaseListener listener) {
        new Thread(() -> {
            String roomID = TAPChatManager.getInstance().arrangeRoomId(myID, otherUserModel.getUserID());
            TAPMessageEntity room = messageDao.getRoom(roomID);
            if (null != room && null != room.getRoomName() && !room.getRoomName().isEmpty()) {
                // Get room model from saved message
                listener.onSelectFinished(new TAPRoomModel(roomID, room.getRoomName(), room.getRoomType(),
                        TAPUtils.fromJSON(new TypeReference<TAPImageURL>() {
                        }, room.getRoomImage()), room.getRoomColor()));
            } else {
                // Create new room model from user data
                // TODO: 18 December 2018 DEFINE DEFAULT ROOM COLOR
                listener.onSelectFinished(new TAPRoomModel(roomID, otherUserModel.getName(), TYPE_PERSONAL, otherUserModel.getAvatarURL(), ""));
            }
        }).start();
    }

    public void getRoomList(String myID, boolean isCheckUnreadFirst, final TAPDatabaseListener listener) {
        new Thread(() -> {
            List<TAPMessageEntity> entities = messageDao.getAllRoomList();

            if (isCheckUnreadFirst && entities.size() > 0) {
                Map<String, Integer> unreadMap = new LinkedHashMap<>();
                for (TAPMessageEntity entity : entities)
                    unreadMap.put(entity.getRoomID(), messageDao.getUnreadCount(myID, entity.getRoomID()));
                listener.onSelectedRoomList(entities, unreadMap);
            } else {
                listener.onSelectFinished(entities);
            }
        }).start();
    }

    public void getUnreadCountPerRoom(String myID, String roomID, final TAPDatabaseListener listener) {
        new Thread(() -> {
            int unreadCount = messageDao.getUnreadCount(myID, roomID);
            listener.onCountedUnreadCount(roomID, unreadCount);
        }).start();
    }

    public void getUnreadCount(String myID, final TAPDatabaseListener listener) {
        new Thread(() -> {
            int unreadCount = messageDao.getUnreadCount(myID);
            listener.onCountedUnreadCount(unreadCount);
        }).start();
    }

    public void getMinCreatedOfUnreadMessage(String myID, String roomID, final TAPDatabaseListener<Long> listener) {
        new Thread(() -> {
            TAPMessageEntity tempUnreadEntity = messageDao.getMinCreatedOfUnreadMessage(myID, roomID);

            if (null != tempUnreadEntity) {
                Long minCreatedOfUnreadMessage = tempUnreadEntity.getCreated();
                listener.onSelectFinished(minCreatedOfUnreadMessage);
            } else listener.onSelectFinished(0L);
        }).start();
    }

    public void delete(final String localID) {
        new Thread(() -> messageDao.delete(localID)).start();
    }

    public void deleteAllMessage() {
        new Thread(() -> messageDao.deleteAllMessage()).start();
    }

    public void updatePendingStatus() {
        new Thread(() -> messageDao.updatePendingStatus()).start();
    }

    public void updatePendingStatus(final String localID) {
        new Thread(() -> messageDao.updatePendingStatus(localID)).start();
    }

    public void updateFailedStatusToSending(final String localID) {
        new Thread(() -> messageDao.updateFailedStatusToSending(localID)).start();
    }

    public void updateMessageAsRead(final String messageID) {
        new Thread(() -> messageDao.updateMessageAsRead(messageID)).start();
    }

    public void updateMessagesAsRead(final List<String> messageIDs) {
        if (null == messageIDs || messageIDs.isEmpty()) {
            return;
        }
        if (messageIDs.size() == 1) {
            updateMessageAsRead(messageIDs.get(0));
        } else {
            new Thread(() -> {
                // Updated 2020/02/10
                // Split operation if there are more than 500 unread messages
                int unreadCount = messageIDs.size();
                int maxSize = 500;
                if (unreadCount > maxSize) {
                    int separatedCount = 0;
                    while (separatedCount < unreadCount) {
                        int remainingUnreadCount = unreadCount - separatedCount;
                        int endIndex = (remainingUnreadCount > maxSize ? maxSize : remainingUnreadCount) + separatedCount;
                        List<String> separatedMessageIDs = messageIDs.subList(separatedCount, endIndex);
                        messageDao.updateMessagesAsRead(separatedMessageIDs);
                        separatedCount += separatedMessageIDs.size();
                    }
                } else {
                    messageDao.updateMessagesAsRead(messageIDs);
                }
            }).start();
        }
    }

    public void deleteMessageByRoomId(final String roomId, TAPDatabaseListener listener) {
        new Thread(() -> {
            messageDao.deleteMessageByRoomId(roomId);
            listener.onDeleteFinished();
        }).start();
    }
}
