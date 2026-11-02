package com.sqisoft.reservation.domain;

public enum GroupErrorCode implements ErrorCode {

    GROUP_NOT_FOUND("그룹을 찾을 수 없습니다."),
    INVALID_GROUP_NAME("그룹 이름은 1~30자로 입력해주세요."),
    NOT_GROUP_OWNER("그룹장만 할 수 있는 작업입니다."),
    NOT_GROUP_MEMBER("승인된 그룹원만 할 수 있는 작업입니다."),
    ALREADY_INVITED("이미 초대했거나 참여 중인 사용자입니다."),
    SELF_INVITATION("자기 자신은 초대할 수 없습니다."),
    INVITATION_NOT_FOUND("초대를 찾을 수 없습니다."),
    NOT_INVITEE("초대받은 본인만 응답할 수 있습니다."),
    INVITATION_ALREADY_RESPONDED("이미 응답한 초대입니다.");

    private final String message;

    GroupErrorCode(String message) {
        this.message = message;
    }

    @Override
    public String message() {
        return message;
    }
}
