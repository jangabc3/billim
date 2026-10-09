import { View, Text, ScrollView, ActivityIndicator, Pressable, StyleSheet } from 'react-native';
import { useCallback, useRef, useState } from 'react';
import { useRouter, useFocusEffect } from 'expo-router';
import Svg, { Path } from 'react-native-svg';
import TopBar from '../../src/components/TopBar';
import SectionIntro from '../../src/components/SectionIntro';
import ResourceCard from '../../src/components/ResourceCard';
import type { ResourceCardItem } from '../../src/components/ResourceCard';
import { useBookmarks } from '../../src/contexts/BookmarkContext';
import { useAuth } from '../../src/contexts/AuthContext';
import { favoriteApi, ApiError } from '../../src/api/client';
import { toResourceCardItem } from '../../src/utils/mapResource';
import { colors, fonts } from '../../src/theme/tokens';

export default function BookmarksScreen() {
  const router = useRouter();
  const { bookmarked, toggle } = useBookmarks();
  const { isLoggedIn, loading: authLoading } = useAuth();

  const [items, setItems] = useState<ResourceCardItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const loadedOnce = useRef(false);

  // 이 탭에 들어올 때마다 서버 기준 최신 목록을 다시 가져온다.
  // (토글 직후에 바로 다시 불러오면 "저장 요청"과 "목록 요청"이 동시에 나가서
  //  저장이 반영되기 전의 옛 목록을 받을 수 있다. 탭 진입 시점에는 저장이 이미 끝나 있다.)
  useFocusEffect(
    useCallback(() => {
      if (authLoading) return;

      if (!isLoggedIn) {
        setItems([]);
        setLoading(false);
        loadedOnce.current = false;
        return;
      }

      let cancelled = false;
      // 처음 한 번만 스피너를 보여주고, 이후 재진입에서는 기존 목록을 유지한 채 조용히 갱신한다.
      if (!loadedOnce.current) setLoading(true);
      setError(null);

      favoriteApi
        .list()
        .then((resources) => {
          if (cancelled) return;
          setItems(resources.map((r) => toResourceCardItem(r)));
          loadedOnce.current = true;
        })
        .catch((e) => {
          if (cancelled) return;
          setError(e instanceof ApiError ? e.message : '즐겨찾기를 불러오지 못했어요.');
        })
        .finally(() => {
          if (!cancelled) setLoading(false);
        });

      return () => {
        cancelled = true;
      };
    }, [isLoggedIn, authLoading]),
  );

  // 이 화면에서 북마크를 해제한 항목은 서버 응답을 기다리지 않고 바로 목록에서 숨긴다.
  const visibleItems = items.filter((item) => bookmarked.has(item.id));

  return (
    <ScrollView style={{ flex: 1, backgroundColor: colors.surface }} contentContainerStyle={{ paddingBottom: 20 }}>
      <TopBar />
      <SectionIntro eyebrow="Bookmarks" title="저장해둔 물품," highlight="여기서 확인해요." desc="관심 가는 물품은 눌러서 저장해두세요." />

      <View style={{ paddingHorizontal: 20 }}>
        {!authLoading && !isLoggedIn && (
          <View style={styles.empty}>
            <Text style={styles.emptyTitle}>로그인하고 저장해보세요</Text>
            <Text style={styles.emptyDesc}>로그인하면 관심 있는 물품을{'\n'}여기에 모아둘 수 있어요.</Text>
            <Pressable style={styles.loginBtn} onPress={() => router.push('/login')}>
              <Text style={styles.loginBtnText}>로그인하기</Text>
            </Pressable>
          </View>
        )}

        {isLoggedIn && loading && (
          <View style={{ paddingVertical: 40, alignItems: 'center' }}>
            <ActivityIndicator color={colors.brand} />
          </View>
        )}

        {isLoggedIn && !loading && error && (
          <Text style={{ fontSize: 12.5, fontFamily: fonts.regular, color: colors.ink3, textAlign: 'center', paddingTop: 30 }}>{error}</Text>
        )}

        {isLoggedIn && !loading && !error && visibleItems.length === 0 && (
          <View style={styles.empty}>
            <View style={styles.emptyIcon}>
              <Svg width={26} height={26} viewBox="0 0 24 24" fill="none" stroke={colors.ink3} strokeWidth={2}>
                <Path d="M6 4h12a1 1 0 0 1 1 1v15l-7-4-7 4V5a1 1 0 0 1 1-1z" />
              </Svg>
            </View>
            <Text style={styles.emptyTitle}>아직 저장한 물품이 없어요</Text>
            <Text style={styles.emptyDesc}>마음에 드는 물품의 북마크를 눌러{'\n'}여기에 모아보세요.</Text>
          </View>
        )}

        {isLoggedIn && !loading && !error && visibleItems.map((item) => (
          <ResourceCard
            key={item.id}
            item={{ ...item, bookmarked: true }}
            onPress={() => router.push(`/resources/${item.id}`)}
            onToggleBookmark={() => toggle(item.id)}
          />
        ))}
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  empty: { alignItems: 'center', paddingTop: 40, paddingHorizontal: 30 },
  emptyIcon: { width: 60, height: 60, borderRadius: 30, backgroundColor: colors.grayFill, alignItems: 'center', justifyContent: 'center', marginBottom: 16 },
  emptyTitle: { fontSize: 14.5, fontFamily: fonts.bold, marginBottom: 6, color: colors.ink },
  emptyDesc: { fontSize: 12.5, fontFamily: fonts.regular, color: colors.ink3, textAlign: 'center', lineHeight: 18, marginBottom: 16 },
  loginBtn: { paddingHorizontal: 20, paddingVertical: 10, borderRadius: 10, backgroundColor: colors.brand },
  loginBtnText: { fontSize: 13, fontFamily: fonts.semibold, color: '#fff' },
});