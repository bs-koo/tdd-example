import { useId, useRef, useState } from 'react';
import { flushSync } from 'react-dom';
import type { GroupMemberDto, GroupRole, MembershipStatus } from '../types';
import type { GroupBoard } from '../hooks/useGroups';
import ErrorBanner from '../components/ErrorBanner';

export type GroupsPageProps = {
  board: GroupBoard;
};

const GROUP_NAME_LIMIT = 30;
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const ROLE_LABEL: Record<GroupRole, string> = {
  OWNER: '그룹장',
  MEMBER: '그룹원',
};

// 서버의 멤버십 상태를 사람이 읽는 말로 옮긴다. "APPROVED" 는 그룹 안에서 "참여 중"이라는 뜻이다.
const STATUS_LABEL: Record<MembershipStatus, string> = {
  APPROVED: '참여 중',
  PENDING: '수락 대기',
  REJECTED: '거절함',
};

function RosterRow({ member }: { member: GroupMemberDto }) {
  return (
    <li className="roster__item">
      <span className="roster__name">{member.name}</span>
      <span className="roster__email">{member.email}</span>
      <span className="badge" data-role={member.role}>
        {ROLE_LABEL[member.role]}
      </span>
      <span className="badge" data-status={member.status}>
        {STATUS_LABEL[member.status]}
      </span>
    </li>
  );
}

export default function GroupsPage({ board }: GroupsPageProps) {
  const [groupName, setGroupName] = useState('');
  const [inviteEmail, setInviteEmail] = useState('');
  const [groupNameError, setGroupNameError] = useState<string | null>(null);
  const [inviteEmailError, setInviteEmailError] = useState<string | null>(null);

  const groupNameRef = useRef<HTMLInputElement>(null);
  const inviteEmailRef = useRef<HTMLInputElement>(null);

  const fieldId = useId();
  const groupNameId = `${fieldId}-group-name`;
  const inviteEmailId = `${fieldId}-invite-email`;

  const selected = board.groups.find((group) => group.id === board.selectedGroupId) ?? null;
  const isOwner = selected?.role === 'OWNER';

  const handleCreateGroup = async (event: React.FormEvent) => {
    event.preventDefault();
    const trimmed = groupName.trim();
    const error =
      trimmed.length === 0
        ? '그룹 이름을 입력해 주세요.'
        : trimmed.length > GROUP_NAME_LIMIT
          ? `그룹 이름은 ${GROUP_NAME_LIMIT}자 이내로 입력해 주세요.`
          : null;
    if (error !== null) {
      flushSync(() => setGroupNameError(error));
      groupNameRef.current?.focus();
      return;
    }
    setGroupNameError(null);
    await board.createGroup(trimmed);
    // 같은 이름을 두 번 만들지 않도록 비운다. 실패하면 오류 배너가 사유를 말한다.
    setGroupName('');
  };

  const handleInvite = async (event: React.FormEvent) => {
    event.preventDefault();
    const trimmed = inviteEmail.trim();
    const error =
      trimmed.length === 0
        ? '초대할 이메일을 입력해 주세요.'
        : !EMAIL_PATTERN.test(trimmed)
          ? '이메일 주소 형식으로 입력해 주세요. 예: name@sqisoft.com'
          : null;
    if (error !== null) {
      flushSync(() => setInviteEmailError(error));
      inviteEmailRef.current?.focus();
      return;
    }
    setInviteEmailError(null);
    await board.invite(trimmed);
    setInviteEmail('');
  };

  return (
    <div className="groups">
      <div className="notice">
        <ErrorBanner code={board.errorCode} />
        {board.errorCode !== null && (
          <div className="notice__actions">
            <button
              type="button"
              className="btn btn--quiet"
              aria-label="오류 알림 닫기"
              onClick={board.clearError}
            >
              알림 닫기
            </button>
          </div>
        )}
      </div>

      <div className="groups__layout">
        <section className="panel" aria-label="내 그룹">
          <h2 className="panel__title">내 그룹</h2>

          {board.groups.length > 0 ? (
            <ul className="grouplist">
              {board.groups.map((group) => (
                <li key={group.id}>
                  {/* 이름이 이 버튼의 이름이고, 역할은 설명으로 딸려 읽힌다 —
                      "SQI 개발팀 그룹장"이 한 덩어리 이름으로 들리지 않게 한다. */}
                  <button
                    type="button"
                    className="grouplist__item"
                    aria-label={group.name}
                    aria-describedby={`${fieldId}-role-${group.id}`}
                    aria-current={group.id === board.selectedGroupId}
                    onClick={() => void board.selectGroup(group.id)}
                  >
                    <span className="grouplist__name">{group.name}</span>
                    <span className="grouplist__role" id={`${fieldId}-role-${group.id}`}>
                      {ROLE_LABEL[group.role]}
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          ) : (
            <p className="panel__state">아직 속한 그룹이 없습니다.</p>
          )}

          <form className="panel__form" onSubmit={handleCreateGroup} noValidate>
            <div className="field">
              <div className="field__head">
                <label className="field__label" htmlFor={groupNameId}>
                  그룹 이름
                </label>
                <span className="field__count">
                  {groupName.length} / {GROUP_NAME_LIMIT}
                </span>
              </div>
              <input
                id={groupNameId}
                ref={groupNameRef}
                className="field__input"
                aria-label="그룹 이름"
                maxLength={GROUP_NAME_LIMIT}
                placeholder="예: SQI 개발팀"
                value={groupName}
                aria-invalid={groupNameError !== null}
                aria-describedby={groupNameError !== null ? `${groupNameId}-error` : undefined}
                onChange={(e) => setGroupName(e.target.value)}
              />
              {groupNameError !== null && (
                <span className="field__error" id={`${groupNameId}-error`}>
                  {groupNameError}
                </span>
              )}
            </div>
            <button type="submit" className="btn btn--primary btn--block">
              그룹 만들기
            </button>
          </form>
        </section>

        <section className="panel" aria-label="그룹원">
          <h2 className="panel__title">
            그룹원{selected !== null && <span className="panel__subject">{selected.name}</span>}
          </h2>

          {selected === null ? (
            <p className="panel__state">그룹을 선택하면 그룹원이 보입니다.</p>
          ) : board.members.length > 0 ? (
            <ul className="roster" aria-label="그룹원">
              {board.members.map((member) => (
                <RosterRow key={member.membershipId} member={member} />
              ))}
            </ul>
          ) : (
            <p className="panel__state">그룹원을 불러오는 중입니다.</p>
          )}

          {/* 초대는 그룹장만 보낼 수 있다 — 보낼 수 없는 폼을 보여 주지 않는다. */}
          {isOwner && (
            <form className="panel__form" onSubmit={handleInvite} noValidate>
              <div className="field">
                <div className="field__head">
                  <label className="field__label" htmlFor={inviteEmailId}>
                    초대할 이메일
                  </label>
                </div>
                <input
                  id={inviteEmailId}
                  ref={inviteEmailRef}
                  type="email"
                  className="field__input"
                  aria-label="초대할 이메일"
                  placeholder="name@sqisoft.com"
                  value={inviteEmail}
                  aria-invalid={inviteEmailError !== null}
                  aria-describedby={
                    inviteEmailError !== null ? `${inviteEmailId}-error` : undefined
                  }
                  onChange={(e) => setInviteEmail(e.target.value)}
                />
                {inviteEmailError !== null ? (
                  <span className="field__error" id={`${inviteEmailId}-error`}>
                    {inviteEmailError}
                  </span>
                ) : (
                  <span className="field__hint">이미 가입한 사람만 초대할 수 있습니다.</span>
                )}
              </div>
              <button type="submit" className="btn btn--block">
                초대 보내기
              </button>
            </form>
          )}
        </section>

        <section className="panel" aria-label="받은 초대">
          <h2 className="panel__title">받은 초대</h2>

          {board.invitations.length > 0 ? (
            <ul className="invites" aria-label="받은 초대">
              {board.invitations.map((invitation) => (
                <li key={invitation.id} className="invites__item">
                  <span className="invites__group">{invitation.groupName}</span>
                  <span className="invites__by">{invitation.invitedBy}님이 초대했습니다</span>
                  <div className="invites__actions">
                    <button
                      type="button"
                      className="btn btn--primary"
                      aria-label={`${invitation.groupName} 초대 수락`}
                      onClick={() => void board.acceptInvitation(invitation.id)}
                    >
                      수락
                    </button>
                    <button
                      type="button"
                      className="btn"
                      aria-label={`${invitation.groupName} 초대 거절`}
                      onClick={() => void board.rejectInvitation(invitation.id)}
                    >
                      거절
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          ) : (
            <p className="panel__state">받은 초대가 없습니다.</p>
          )}
        </section>
      </div>
    </div>
  );
}
