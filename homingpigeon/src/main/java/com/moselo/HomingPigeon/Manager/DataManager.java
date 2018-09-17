package com.moselo.HomingPigeon.Manager;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.moselo.HomingPigeon.Data.Message.MessageEntity;
import com.moselo.HomingPigeon.Helper.Utils;
import com.moselo.HomingPigeon.Listener.HomingPigeonGetChatListener;
import com.moselo.HomingPigeon.Model.UserModel;

import java.util.List;

import static com.moselo.HomingPigeon.Helper.DefaultConstant.K_RECIPIENT_ID;
import static com.moselo.HomingPigeon.Helper.DefaultConstant.K_USER;

public class DataManager {
    private static DataManager instance;

    public static DataManager getInstance() {
        return instance == null ? (instance = new DataManager()) : instance;
    }

    public UserModel getActiveUser(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return Utils.getInstance().fromJSON(new TypeReference<UserModel>() {
        }, prefs.getString(K_USER, null));
    }

    public boolean checkActiveUser(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (null == Utils.getInstance().fromJSON(new TypeReference<UserModel>() {}, prefs.getString(K_USER, null)))
            return false;
        else return true;
    }

    public void saveActiveUser(Context context, UserModel user) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(K_USER, Utils.getInstance().toJsonString(user)).apply();
        ChatManager.getInstance().setActiveUser(user);
    }

    // TODO: 14/09/18 TEMP
    public String getRecipientID(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(K_RECIPIENT_ID, "0");
    }

    // TODO: 14/09/18 TEMP
    public void saveRecipientID(Context context, String recipientID) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(K_RECIPIENT_ID, recipientID).apply();
    }

    public void initDatabaseManager(String databaseType, Application application) {
        DatabaseManager.getInstance().setRepository(databaseType, application);
    }

    public void insertToDatabase(MessageEntity messageEntity) {
        DatabaseManager.getInstance().insert(messageEntity);
    }

    public void insertToDatabase(List<MessageEntity> messageEntities) {
        DatabaseManager.getInstance().insert(messageEntities);
    }

    public void deleteFromDatabase(String messageLocalID) {
        DatabaseManager.getInstance().delete(messageLocalID);
    }

    public void updateSendingMessageToFailed() {
        DatabaseManager.getInstance().updatePendingStatus();
    }

    public void updateSendingMessageToFailed(String localID) {
        DatabaseManager.getInstance().updatePendingStatus(localID);
    }

    public LiveData<List<MessageEntity>> getMessagesLiveData() {
        return DatabaseManager.getInstance().getMessagesLiveData();
    }

    public void getMessagesFromDatabase(String roomID, HomingPigeonGetChatListener listener) {
        DatabaseManager.getInstance().getMessages(roomID, listener);
    }

    public void getMessagesFromDatabase(String roomID, HomingPigeonGetChatListener listener, long lastTimestamp) {
        DatabaseManager.getInstance().getMessages(roomID, listener, lastTimestamp);
    }
}