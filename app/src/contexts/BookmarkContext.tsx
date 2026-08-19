import { createContext, useContext, useState, type ReactNode } from 'react';

interface BookmarkContextValue {
  bookmarked: Set<string>;
  toggle: (id: string) => void;
}

const BookmarkContext = createContext<BookmarkContextValue | null>(null);

// TODO: 로그인 붙으면 서버(FavoriteResource API)와 동기화.
export function BookmarkProvider({ children }: { children: ReactNode }) {
  const [bookmarked, setBookmarked] = useState<Set<string>>(new Set(['2']));

  const toggle = (id: string) => {
    setBookmarked((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  };

  return (
    <BookmarkContext.Provider value={{ bookmarked, toggle }}>
      {children}
    </BookmarkContext.Provider>
  );
}

export function useBookmarks() {
  const ctx = useContext(BookmarkContext);
  if (!ctx) throw new Error('useBookmarks는 BookmarkProvider 안에서만 쓸 수 있어요.');
  return ctx;
}
