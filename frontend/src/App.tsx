import { useState } from 'react';
import type { AuthApi, GroupApi, ReservationApi } from './api/types';
import { authApi as realAuthApi } from './api/auth';
import { groupApi as realGroupApi } from './api/groups';
import { reservationApi as realReservationApi } from './api/reservations';
import { useAuth } from './hooks/useAuth';
import { useGroups } from './hooks/useGroups';
import LoginPage from './pages/LoginPage';
import GroupsPage from './pages/GroupsPage';
import ReservationPage from './pages/ReservationPage';
import { today } from './today';

export type AppProps = {
  authApi?: AuthApi;
  groupApi?: GroupApi;
  reservationApi?: ReservationApi;
};

type Tab = 'reservation' | 'groups';

export default function App({
  authApi = realAuthApi,
  groupApi = realGroupApi,
  reservationApi = realReservationApi,
}: AppProps = {}) {
  const auth = useAuth({ api: authApi });

  if (auth.status === 'loading') {
    return (
      <div className="page page--center">
        <p className="page__state" role="status">
          불러오는 중입니다.
        </p>
      </div>
    );
  }

  if (auth.status === 'anonymous' || auth.member === null) {
    return <LoginPage auth={auth} />;
  }

  // 로그인한 사람이 바뀌면 그룹 상태도 처음부터 다시 쌓는다 —
  // key 로 갈아 끼워 앞 사용자의 그룹이 한 프레임이라도 비치지 않게 한다.
  return (
    <SignedInShell
      key={auth.member.id}
      auth={auth}
      groupApi={groupApi}
      reservationApi={reservationApi}
    />
  );
}

type SignedInShellProps = {
  auth: ReturnType<typeof useAuth>;
  groupApi: GroupApi;
  reservationApi: ReservationApi;
};

function SignedInShell({ auth, groupApi, reservationApi }: SignedInShellProps) {
  const [tab, setTab] = useState<Tab>('reservation');
  const board = useGroups({ api: groupApi });
  const member = auth.member;

  if (member === null) {
    return null;
  }

  const hasGroup = board.groups.length > 0;

  return (
    <div className="app">
      <header className="topbar">
        <span className="topbar__brand">회의실 예약</span>

        <nav className="tabs tabs--top" role="tablist" aria-label="화면 전환">
          <button
            type="button"
            role="tab"
            className="tab"
            aria-selected={tab === 'reservation'}
            onClick={() => setTab('reservation')}
          >
            예약
          </button>
          <button
            type="button"
            role="tab"
            className="tab"
            aria-selected={tab === 'groups'}
            onClick={() => setTab('groups')}
          >
            그룹
          </button>
        </nav>

        <div className="topbar__account">
          <span className="topbar__name">{member.name}</span>
          <button type="button" className="btn btn--quiet" onClick={() => void auth.logout()}>
            로그아웃
          </button>
        </div>
      </header>

      {tab === 'groups' ? (
        <main className="shell">
          <GroupsPage board={board} />
        </main>
      ) : hasGroup ? (
        <ReservationPage
          initialDate={today()}
          groupId={board.selectedGroupId}
          groups={board.groups}
          member={member}
          onSelectGroup={(groupId) => void board.selectGroup(groupId)}
          api={reservationApi}
        />
      ) : (
        // 예약할 회의실은 그룹에만 있다. 빈 격자를 보여 주는 대신 다음에 할 일을 준다.
        <main className="shell">
          <div className="empty">
            <h1 className="empty__title">그룹을 만들거나 초대를 수락하세요.</h1>
            <p className="empty__lead">
              회의실은 그룹 단위로 관리합니다. 그룹에 들어가야 예약 격자가 열립니다.
            </p>
            <button type="button" className="btn btn--primary" onClick={() => setTab('groups')}>
              그룹 화면으로 가기
            </button>
          </div>
        </main>
      )}
    </div>
  );
}
