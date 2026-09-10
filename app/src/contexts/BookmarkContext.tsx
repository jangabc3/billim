import { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import { favoriteApi } from '../api/client';
import { useAuth } from './AuthContext';

interface BookmarkContextValue {
  bookmarked: Set<string>;
  toggle: (id: string) => void;
  loading: boolean;
}

const BookmarkContext = createContext<BookmarkContextValue | null>(null);

export function BookmarkProvider({ children }: { children: ReactNode }) {
  const { isLoggedIn, loading: authLoading } = useAuth();
  const [bookmarked, setBookmarked] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(false);

  // 로그인 상태가 바뀔 때마다(로그인 직후, 로그아웃 직후) 서버 기준으로 다시 맞춘다.
  useEffect(() => {
    if (authLoading) return;

    if (!isLoggedIn) {
      setBookmarked(new Set());
      return;
    }

    setLoading(true);
    favoriteApi
      .list()
      .then((resources) => {
        setBookmarked(new Set(resources.map((r) => String(r.id))));
      })
      .catch(() => {
        // 목록을 못 가져와도 앱이 죽으면 안 되니, 빈 상태로 조용히 둔다.
        setBookmarked(new Set());
      })
      .finally(() => setLoading(false));
  }, [isLoggedIn, authLoading]);

  const toggle = (id: string) => {
    if (!isLoggedIn) {
      // 로그인 안 한 사용자는 서버에 저장할 방법이 없으니 토글 자체를 막는다.
      // 화면(ResourceCard 등)에서 로그인 유도로 이어주는 게 이상적 — 우선은 조용히 무시.
      return;
    }

    const isCurrentlyBookmarked = bookmarked.has(id);
    const resourceId = Number(id);

    // 먼저 화면을 바로 바꾸고(낙관적 업데이트), 서버 요청은 뒤에서 처리한다 —
    // 사용자가 버튼 누를 때마다 로딩을 기다리게 하지 않기 위함.
    setBookmarked((prev) => {
      const next = new Set(prev);
      isCurrentlyBookmarked ? next.delete(id) : next.add(id);
      return next;
    });

    const request = isCurrentlyBookmarked
      ? favoriteApi.remove(resourceId)
      : favoriteApi.add(resourceId);

    request.catch(() => {
      // 서버 요청이 실패하면 화면 상태를 원래대로 되돌린다.
      setBookmarked((prev) => {
        const next = new Set(prev);
        isCurrentlyBookmarked ? next.add(id) : next.delete(id);
        return next;
      });
    });
  };

  return (
    <BookmarkContext.Provider value={{ bookmarked, toggle, loading }}>
      {children}
    </BookmarkContext.Provider>
  );
}

export function useBookmarks() {
  const ctx = useContext(BookmarkContext);
  if (!ctx) throw new Error('useBookmarks는 BookmarkProvider 안에서만 쓸 수 있어요.');
  return ctx;
}