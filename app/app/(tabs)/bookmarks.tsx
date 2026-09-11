import { View, Text, ScrollView, ActivityIndicator, Pressable, StyleSheet } from 'react-native';
import { useEffect, useState } from 'react';
import { useRouter } from 'expo-router';
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

  useEffect(() => {
    if (authLoading) return;

    if (!isLoggedIn) {
      setItems([]);
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);
    favoriteApi
      .list()
      .then((resources) => setItems(resources.map((r) => toResourceCardItem(r))))
      .catch((e) => setError(e instanceof ApiError ? e.message : '즐겨찾기를 불러오지 못했어요.'))
      .finally(() => setLoading(false));
    // bookmarked가 바뀔 때마다(토글할 때마다) 목록을 다시 불러와서 화면을 최신 상태로 맞춘다.
  }, [isLoggedIn, authLoading, bookmarked]);

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

        {isLoggedIn && !loading && !error && items.length === 0 && (
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

        {isLoggedIn && !loading && !error && items.map((item) => (
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