import { View, Text, ScrollView, StyleSheet } from 'react-native';
import { useRouter } from 'expo-router';
import Svg, { Path } from 'react-native-svg';
import TopBar from '../../src/components/TopBar';
import SectionIntro from '../../src/components/SectionIntro';
import ResourceCard from '../../src/components/ResourceCard';
import { useBookmarks } from '../../src/contexts/BookmarkContext';
import { mockItems } from '../../src/data/mockResources';
import { colors } from '../../src/theme/tokens';

export default function BookmarksScreen() {
  const router = useRouter();
  const { bookmarked, toggle } = useBookmarks();
  const saved = mockItems.filter((item) => bookmarked.has(item.id));

  return (
    <ScrollView style={{ flex: 1, backgroundColor: colors.surface }} contentContainerStyle={{ paddingBottom: 20 }}>
      <TopBar />
      <SectionIntro eyebrow="Bookmarks" title="나중에 빌림" highlight="목록이에요." desc="관심 있는 자원은 여기에 차곡차곡 모아둘게요." />

      <View style={{ paddingHorizontal: 20 }}>
        {saved.length === 0 ? (
          <View style={styles.empty}>
            <View style={styles.emptyIcon}>
              <Svg width={26} height={26} viewBox="0 0 24 24" fill="none" stroke={colors.ink3} strokeWidth={2}>
                <Path d="M6 4h12a1 1 0 0 1 1 1v15l-7-4-7 4V5a1 1 0 0 1 1-1z" />
              </Svg>
            </View>
            <Text style={styles.emptyTitle}>아직 저장한 자원이 없어요</Text>
            <Text style={styles.emptyDesc}>마음에 드는 자원의 북마크를 눌러{'\n'}여기에 모아보세요.</Text>
          </View>
        ) : (
          saved.map((item) => (
            <ResourceCard
              key={item.id}
              item={{ ...item, bookmarked: true }}
              onPress={() => router.push(`/resources/${item.id}`)}
              onToggleBookmark={() => toggle(item.id)}
            />
          ))
        )}
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  empty: { alignItems: 'center', paddingTop: 40, paddingHorizontal: 30 },
  emptyIcon: { width: 60, height: 60, borderRadius: 30, backgroundColor: colors.grayFill, alignItems: 'center', justifyContent: 'center', marginBottom: 16 },
  emptyTitle: { fontSize: 14.5, fontWeight: '800', marginBottom: 6, color: colors.ink },
  emptyDesc: { fontSize: 12.5, color: colors.ink3, textAlign: 'center', lineHeight: 18 },
});
