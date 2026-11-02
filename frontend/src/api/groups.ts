import type { GroupDto, GroupMemberDto, InvitationDto } from '../types';
import type { GroupApi } from './types';
import { authorizedFetch, throwApiError } from './client';

const JSON_HEADERS = { 'Content-Type': 'application/json' };

async function readJson<T>(response: Response): Promise<T> {
  if (!response.ok) {
    await throwApiError(response);
  }
  return (await response.json()) as T;
}

export const groupApi: GroupApi = {
  async fetchGroups(): Promise<GroupDto[]> {
    return readJson<GroupDto[]>(await authorizedFetch('/api/groups'));
  },

  async createGroup(name: string): Promise<GroupDto> {
    const response = await authorizedFetch('/api/groups', {
      method: 'POST',
      headers: JSON_HEADERS,
      body: JSON.stringify({ name }),
    });
    return readJson<GroupDto>(response);
  },

  async fetchMembers(groupId: number): Promise<GroupMemberDto[]> {
    return readJson<GroupMemberDto[]>(await authorizedFetch(`/api/groups/${groupId}/members`));
  },

  async invite(groupId: number, email: string): Promise<GroupMemberDto> {
    const response = await authorizedFetch(`/api/groups/${groupId}/invitations`, {
      method: 'POST',
      headers: JSON_HEADERS,
      body: JSON.stringify({ email }),
    });
    return readJson<GroupMemberDto>(response);
  },

  async fetchInvitations(): Promise<InvitationDto[]> {
    return readJson<InvitationDto[]>(await authorizedFetch('/api/invitations'));
  },

  async acceptInvitation(membershipId: number): Promise<InvitationDto> {
    const response = await authorizedFetch(`/api/invitations/${membershipId}/accept`, {
      method: 'POST',
    });
    return readJson<InvitationDto>(response);
  },

  async rejectInvitation(membershipId: number): Promise<InvitationDto> {
    const response = await authorizedFetch(`/api/invitations/${membershipId}/reject`, {
      method: 'POST',
    });
    return readJson<InvitationDto>(response);
  },
};
