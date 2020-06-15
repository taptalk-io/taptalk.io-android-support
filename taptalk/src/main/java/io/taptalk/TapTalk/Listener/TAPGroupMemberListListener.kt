package io.taptalk.TapTalk.Listener

import android.support.annotation.Keep
import io.taptalk.TapTalk.Interface.TapTalkGroupMemberListInterface
import io.taptalk.TapTalk.Model.TAPUserModel

@Keep
abstract class TAPGroupMemberListListener : TapTalkGroupMemberListInterface {
    override fun onContactSelected(contact: TAPUserModel?): Boolean {
        return super.onContactSelected(contact)
    }

    override fun onContactDeselected(contact: TAPUserModel?) {
        super.onContactDeselected(contact)
    }

    override fun onContactLongPress(contact: TAPUserModel?) {
        super.onContactLongPress(contact)
    }

    override fun onGroupMemberClicked(member: TAPUserModel?, isAdmin: Boolean) {
        super.onGroupMemberClicked(member, isAdmin)
    }
}