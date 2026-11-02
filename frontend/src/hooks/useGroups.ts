import { useCallback, useEffect, useRef, useState } from 'react';
import type { GroupDto, GroupMemberDto, InvitationDto } from '../types';
import type { GroupApi } from '../api/types';
import { ApiError } from '../api/client';

function toErrorCode(e: unknown): string {
  return e instanceof ApiError ? e.code : 'UNKNOWN';
}

export type GroupBoard = {
  groups: GroupDto[];
  invitations: InvitationDto[];
  members: GroupMemberDto[];
  selectedGroupId: number | null;
  loading: boolean;
  errorCode: string | null;
  selectGroup: (groupId: number) => Promise<void>;
  createGroup: (name: string) => Promise<void>;
  invite: (email: string) => Promise<void>;
  acceptInvitation: (membershipId: number) => Promise<void>;
  rejectInvitation: (membershipId: number) => Promise<void>;
  clearError: () => void;
};

/**
 * 그룹·초대·그룹원의 단일 출처. 예약 화면의 그룹 선택과 그룹 화면이 같은 선택을 공유해야 하므로
 * 두 화면 위(App)에서 한 번만 돌린다.
 */
export function useGroups(options: { api: GroupApi }): GroupBoard {
  const { api } = options;
  const [groups, setGroups] = useState<GroupDto[]>([]);
  const [invitations, setInvitations] = useState<InvitationDto[]>([]);
  const [members, setMembers] = useState<GroupMemberDto[]>([]);
  const [selectedGroupId, setSelectedGroupId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [errorCode, setErrorCode] = useState<string | null>(null);

  // 선택된 그룹을 콜백 의존성에 넣지 않으려고 ref 로도 들고 있는다 —
  // 의존성에 넣으면 선택이 바뀔 때마다 초대 폼의 핸들러가 새로 만들어진다.
  const selectedRef = useRef<number | null>(null);
  selectedRef.current = selectedGroupId;

  const loadMembers = useCallback(
    async (groupId: number) => {
      setMembers(await api.fetchMembers(groupId));
    },
    [api],
  );

  const selectGroup = useCallback(
    async (groupId: number) => {
      setSelectedGroupId(groupId);
      try {
        await loadMembers(groupId);
        setErrorCode(null);
      } catch (e) {
        setErrorCode(toErrorCode(e));
      }
    },
    [loadMembers],
  );

  /** 그룹·초대 목록을 다시 받고, 선택이 비어 있으면 첫 그룹으로 채운다. */
  const reload = useCallback(
    async (preferGroupId?: number) => {
      const [nextGroups, nextInvitations] = await Promise.all([
        api.fetchGroups(),
        api.fetchInvitations(),
      ]);
      setGroups(nextGroups);
      setInvitations(nextInvitations);

      const stillThere = nextGroups.some((group) => group.id === selectedRef.current);
      const target =
        nextGroups.find((group) => group.id === preferGroupId)?.id ??
        (stillThere ? selectedRef.current : (nextGroups[0]?.id ?? null));

      setSelectedGroupId(target);
      selectedRef.current = target;
      if (target !== null) {
        await loadMembers(target);
      } else {
        setMembers([]);
      }
    },
    [api, loadMembers],
  );

  useEffect(() => {
    reload()
      .catch((e: unknown) => setErrorCode(toErrorCode(e)))
      .finally(() => setLoading(false));
    // 마운트 시 1회만 조회한다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const createGroup = useCallback(
    async (name: string) => {
      try {
        const created = await api.createGroup(name);
        await reload(created.id);
        setErrorCode(null);
      } catch (e) {
        setErrorCode(toErrorCode(e));
      }
    },
    [api, reload],
  );

  const invite = useCallback(
    async (email: string) => {
      const groupId = selectedRef.current;
      if (groupId === null) {
        return;
      }
      try {
        await api.invite(groupId, email);
        await loadMembers(groupId);
        setErrorCode(null);
      } catch (e) {
        setErrorCode(toErrorCode(e));
      }
    },
    [api, loadMembers],
  );

  const acceptInvitation = useCallback(
    async (membershipId: number) => {
      try {
        const accepted = await api.acceptInvitation(membershipId);
        // 방금 합류한 그룹으로 옮겨 준다 — 수락의 결과를 바로 보게 한다.
        await reload(accepted.groupId);
        setErrorCode(null);
      } catch (e) {
        setErrorCode(toErrorCode(e));
      }
    },
    [api, reload],
  );

  const rejectInvitation = useCallback(
    async (membershipId: number) => {
      try {
        await api.rejectInvitation(membershipId);
        await reload();
        setErrorCode(null);
      } catch (e) {
        setErrorCode(toErrorCode(e));
      }
    },
    [api, reload],
  );

  const clearError = useCallback(() => setErrorCode(null), []);

  return {
    groups,
    invitations,
    members,
    selectedGroupId,
    loading,
    errorCode,
    selectGroup,
    createGroup,
    invite,
    acceptInvitation,
    rejectInvitation,
    clearError,
  };
}
