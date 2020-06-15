package io.taptalk.TapTalk.Interface;

import io.taptalk.TapTalk.Model.TAPMessageModel;

public interface TapTalkAttachmentInterface {
    void onDocumentSelected();
    void onCameraSelected();
    void onGallerySelected();
    void onAudioSelected();
    void onLocationSelected();
    void onContactSelected();
    void onCopySelected(String text);
    void onReplySelected(TAPMessageModel message);
    void onForwardSelected(TAPMessageModel message);
    void onOpenLinkSelected(String url);
    void onComposeSelected(String emailRecipient);
    void onPhoneCallSelected(String phoneNumber);
    void onPhoneSmsSelected(String phoneNumber);
    void onSaveImageToGallery(TAPMessageModel message);
    void onSaveVideoToGallery(TAPMessageModel message);
    void onSaveToDownloads(TAPMessageModel message);
    void onDeleteMessage(String roomID, TAPMessageModel message);
}
