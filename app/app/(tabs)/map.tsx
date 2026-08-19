import { View, Text, ScrollView, StyleSheet } from 'react-native';
import { useRouter } from 'expo-router';
import Svg, { Path } from 'react-native-svg';
import TopBar from '../../src/components/TopBar';
import SectionIntro from '../../src/components/SectionIntro';
import ResourceCard from '../../src/components/ResourceCard';
import { useBookmarks } from '../../src/contexts/BookmarkContext';
import { mockItems } from '../../src/data/mockResources';
import { colors, radius } from '../../src/theme/tokens';

export default function MapScreen() {
  const router = useRouter();
  const { bookmarked, toggle } = useBookmarks();
  const closest = mockItems.slice(0, 1);

  return (
    <ScrollView style={{ flex: 1, backgroundColor: colors.surface }} contentContainerStyle={{ paddingBottom: 20 }}>
      <TopBar />
      <SectionIntro eyebrow="Near You" title="가까운 곳부터" highlight="찾아볼까요?" desc="성수동 기준, 오늘 확인된 대여처예요." />

      {/* 지도 플레이스홀더 — TODO: 실제 지도(카카오맵/네이버맵 SDK)로 교체 */}
      <View style={styles.mapBox}>
        <View style={[styles.mapBlob, { left: 24, top: 20, width: 90, height: 60 }]} />
        <View style={[styles.mapBlob, { right: 16, bottom: 14, width: 70, height: 90 }]} />
        <MapPin left={64} top={92} tone="dark"><Path d="M14.7 6.3l3 3-8.4 8.4-4-1 1-4z" /></MapPin>
        <MapPin left={130} top={54} tone="dark"><Path d="M8 4h8l2 4-4 2v12h-4V10L6 8z" /></MapPin>
        <MapPin left={97} top={112} tone="brand" active><Path d="M14.7 6.3l3 3-8.4 8.4-4-1 1-4z" /></MapPin>
        <View style={styles.meDot} />
      </View>

      <View style={{ paddingHorizontal: 20 }}>
        <View style={styles.sectionHead}>
          <View>
            <Text style={styles.sectionEyebrow}>CLOSEST</Text>
            <Text style={styles.sectionTitle}>가까운 순</Text>
          </View>
          <Text style={styles.sectionCount}>{mockItems.length}곳</Text>
        </View>
        {closest.map((item) => (
          <ResourceCard
            key={item.id}
            item={{ ...item, bookmarked: bookmarked.has(item.id) }}
            onPress={() => router.push(`/resources/${item.id}`)}
            onToggleBookmark={() => toggle(item.id)}
          />
        ))}
      </View>
    </ScrollView>
  );
}

function MapPin({ left, top, tone, active, children }: {
  left: number; top: number; tone: 'dark' | 'brand'; active?: boolean; children: React.ReactNode;
}) {
  const size = active ? 34 : 28;
  return (
    <View style={{
      position: 'absolute', left, top, width: size, height: size, borderRadius: size / 2,
      backgroundColor: tone === 'brand' ? colors.brand : '#1B2430',
      alignItems: 'center', justifyContent: 'center', transform: [{ rotate: '-45deg' }],
    }}>
      <View style={{ transform: [{ rotate: '45deg' }] }}>
        <Svg width={size * 0.4} height={size * 0.4} viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth={2.4}>
          {children}
        </Svg>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  mapBox: { marginHorizontal: 20, marginBottom: 22, height: 220, borderRadius: radius.lg, backgroundColor: '#E9EDE6', overflow: 'hidden', borderWidth: 1, borderColor: colors.line },
  mapBlob: { position: 'absolute', borderRadius: 14, backgroundColor: '#DCE4D8' },
  meDot: { position: 'absolute', left: 112, top: 138, width: 12, height: 12, borderRadius: 6, backgroundColor: '#2C5FE0' },
  sectionHead: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-end', marginBottom: 14 },
  sectionEyebrow: { fontSize: 10.5, fontWeight: '800', letterSpacing: 1, color: colors.brand, marginBottom: 4 },
  sectionTitle: { fontSize: 17, fontWeight: '800', color: colors.ink },
  sectionCount: { fontSize: 12.5, color: colors.ink3, fontWeight: '700' },
});
