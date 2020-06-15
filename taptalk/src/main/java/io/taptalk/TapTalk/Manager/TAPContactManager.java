package io.taptalk.TapTalk.Manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.taptalk.TapTalk.Listener.TAPDatabaseListener;
import io.taptalk.TapTalk.Listener.TAPSocketListener;
import io.taptalk.TapTalk.Model.TAPUserModel;

public class TAPContactManager {

    private static final String TAG = TAPContactManager.class.getSimpleName();
    private static TAPContactManager instance;
    private HashMap<String, TAPUserModel> userDataMap;
    private HashMap<String, TAPUserModel> userMapByPhoneNumber;
    private String myCountryCode;
    private boolean isContactSyncPermissionAsked, isContactSyncAllowedByUser;

    private TAPContactManager() {
        //loadAllUserDataFromDatabase();
        TAPConnectionManager.getInstance().addSocketListener(new TAPSocketListener() {
//            @Override
//            public void onSocketConnected() {
//                //loadAllUserDataFromDatabase();
//            }

            @Override
            public void onSocketDisconnected() {
                saveUserDataMapToDatabase();
            }
        });
    }

    public static TAPContactManager getInstance() {
        return null == instance ? instance = new TAPContactManager() : instance;
    }

    public TAPUserModel getUserData(String userID) {
        return getUserDataMap().get(userID);
    }

    public void updateUserData(TAPUserModel user) {
        String myUserId = TAPChatManager.getInstance().getActiveUser().getUserID();
        String incomingUserId = user.getUserID();
        TAPUserModel existingUser = getUserDataMap().get(incomingUserId);
        if (!incomingUserId.equals(myUserId) && null == existingUser) {
            // Add new user to map
            user.checkAndSetContact(0);
            getUserDataMap().put(incomingUserId, user);
            saveUserDataToDatabase(user);
        } else if (!incomingUserId.equals(myUserId) &&
                null != existingUser.getUpdated() &&
                null != user.getUpdated() &&
                existingUser.getUpdated() <= user.getUpdated()) {
            // Update user data in map
            existingUser.updateValue(user);
            saveUserDataToDatabase(user);
        }
        //else if (incomingUserId.equals(myUserId) &&
        //        null != TAPChatManager.getInstance().getActiveUser().getUpdated() &&
        //        null != user.getUpdated() &&
        //        TAPChatManager.getInstance().getActiveUser().getUpdated() <= user.getUpdated()) {
        //    // Update active user
        //    TAPDataManager.getInstance().saveActiveUser(user);
        //}
    }

    public void updateUserData(List<TAPUserModel> users) {
        for (TAPUserModel user : users) {
            updateUserData(user);
        }
    }

    public void removeFromContacts(String userID) {
        getUserData(userID).setIsContact(0);
        TAPDataManager.getInstance().insertMyContactToDatabase(getUserData(userID));
    }

    public void saveUserDataToDatabase(TAPUserModel userModel) {
        if (!userModel.getUserID().equals(TAPChatManager.getInstance().getActiveUser().getUserID())) {
            TAPDataManager.getInstance().checkContactAndInsertToDatabase(userModel);
        }
    }

    public void loadAllUserDataFromDatabase() {
        TAPDataManager.getInstance().getAllUserData(getAllUserDataListener);
    }

    public void saveUserDataMapToDatabase() {
        TAPDataManager.getInstance().insertMyContactToDatabase(convertUserDataToList(getUserDataMap()));
    }

    private HashMap<String, TAPUserModel> getUserDataMap() {
        return null == userDataMap ? userDataMap = new HashMap<>() : userDataMap;
    }

    public void clearUserDataMap() {
        getUserDataMap().clear();
    }

    private List<TAPUserModel> convertUserDataToList(HashMap<String, TAPUserModel> userModelMap) {
        List<TAPUserModel> userModelList = new ArrayList<>();
        for (Map.Entry<String, TAPUserModel> entry : userModelMap.entrySet()) {
            userModelList.add(entry.getValue());
        }
        return userModelList;
    }

    private HashMap<String, TAPUserModel> convertUserDataToMap(List<TAPUserModel> userModelList) {
        HashMap<String, TAPUserModel> userModelMap = new HashMap<>();
        for (TAPUserModel userModel : userModelList) {
            userModelMap.put(userModel.getUserID(), userModel);
        }
        return userModelMap;
    }

    private TAPDatabaseListener<TAPUserModel> getAllUserDataListener = new TAPDatabaseListener<TAPUserModel>() {
        @Override
        public void onSelectFinished(List<TAPUserModel> entities) {
            userDataMap = convertUserDataToMap(entities);
        }
    };

    public HashMap<String, TAPUserModel> getUserMapByPhoneNumber() {
        return null == userMapByPhoneNumber ? userMapByPhoneNumber = new HashMap<>() : userMapByPhoneNumber;
    }

    public void setUserMapByPhoneNumber(HashMap<String, TAPUserModel> userMapByPhoneNumber) {
        this.userMapByPhoneNumber = userMapByPhoneNumber;
    }

    public void clearUserMapByPhoneNumber() {
        getUserMapByPhoneNumber().clear();
    }

    public void addUserMapByPhoneNumber(TAPUserModel userModel) {
        if (null != userModel.getPhoneWithCode() && !"".equals(userModel.getPhoneWithCode()))
            getUserMapByPhoneNumber().put(userModel.getPhoneWithCode(), userModel);
    }

    public boolean isUserPhoneNumberAlreadyExist(String phone) {
        return getUserMapByPhoneNumber().containsKey(phone);
    }

    public String convertPhoneNumber(String phone) {
        if (phone.contains("*") || phone.contains("#") || phone.contains(";") || phone.contains(",") || phone.isEmpty()) {
            return "";
        }
        String tempPhone = phone.replaceAll("[^\\d]", "");
        String prefix = tempPhone.substring(0, getMyCountryCode().length());

        if ('0' == tempPhone.charAt(0)) {
            tempPhone = tempPhone.replaceFirst("0", getMyCountryCode());
        } else if (!prefix.equals(getMyCountryCode())) {
            tempPhone = getMyCountryCode() + tempPhone;
        }

        return tempPhone;
    }

    public String getMyCountryCode() {
        return null == myCountryCode ? myCountryCode = "62" : myCountryCode;
    }

    public void resetMyCountryCode() {
        setMyCountryCode(null);
    }

    public void setMyCountryCode(String myCountryCode) {
        this.myCountryCode = myCountryCode;
    }

    public boolean isContactSyncPermissionAsked() {
        return isContactSyncPermissionAsked;
    }

    public void setAndSaveContactSyncPermissionAsked(boolean contactSyncPermissionAsked) {
        TAPDataManager.getInstance().saveContactSyncPermissionAsked(contactSyncPermissionAsked);
        isContactSyncPermissionAsked = contactSyncPermissionAsked;
    }

    public void setContactSyncPermissionAsked(boolean contactSyncPermissionAsked) {
        isContactSyncPermissionAsked = contactSyncPermissionAsked;
    }

    public void resetContactSyncPermissionAsked() {
        setContactSyncPermissionAsked(false);
    }

    public boolean isContactSyncAllowedByUser() {
        return isContactSyncAllowedByUser;
    }

    public void setAndSaveContactSyncAllowedByUser(boolean contactSyncAllowedByUser) {
        TAPDataManager.getInstance().saveContactSyncAllowedByUser(contactSyncAllowedByUser);
        isContactSyncAllowedByUser = contactSyncAllowedByUser;
    }

    public void setContactSyncAllowedByUser(boolean contactSyncAllowedByUser) {
        isContactSyncAllowedByUser = contactSyncAllowedByUser;
    }

    public void resetContactSyncAllowedByUser() {
        setContactSyncAllowedByUser(false);
    }
}
