import { View, Text, Image, Pressable, ScrollView, StyleSheet } from 'react-native';
import { useRouter } from 'expo-router';
import { LinearGradient } from 'expo-linear-gradient';
import Svg, { Path, Line, Circle } from 'react-native-svg';
import TopBar from '../../src/components/TopBar';
import ResourceCard from '../../src/components/ResourceCard';
import { useBookmarks } from '../../src/contexts/BookmarkContext';
import { mockItems } from '../../src/data/mockResources';
import { colors, radius } from '../../src/theme/tokens';

const categories = [
  { key: 'all', label: '모두', icon: <><Line x1="4" y1="6" x2="20" y2="6" /><Line x1="4" y1="12" x2="14" y2="12" /><Line x1="4" y1="18" x2="10" y2="18" /></> },
  { key: 'tool', label: '공구', icon: <Path d="M14.7 6.3l3 3-8.4 8.4-4-1 1-4z" /> },
  { key: 'suit', label: '정장', icon: <Path d="M8 4h8l2 4-4 2v12h-4V10L6 8z" /> },
  { key: 'medical', label: '의료', icon: <><Circle cx="6" cy="17" r="3" /><Circle cx="18" cy="17" r="3" /><Path d="M6 17V9l6 2 3-5" /></> },
  { key: 'life', label: '생활', icon: <Path d="M12 3c-4 4-7 7-7 11a7 7 0 0 0 14 0c0-4-3-7-7-11z" /> },
];

export default function HomeScreen() {
  const router = useRouter();
  const { bookmarked, toggle } = useBookmarks();

  return (
    <ScrollView style={{ flex: 1, backgroundColor: colors.surface }} contentContainerStyle={{ paddingBottom: 20 }}>
      <TopBar />

      {/* 히어로 */}
      <View style={styles.hero}>
        <Image source={{ uri: 'https://example.com/hero.jpg' }} style={StyleSheet.absoluteFill} resizeMode="cover" />
        <LinearGradient
          colors={['rgba(10,15,25,0.15)', 'rgba(8,12,20,0.55)', 'rgba(6,9,15,0.85)']}
          style={StyleSheet.absoluteFill}
        />
        <View style={styles.heroContent}>
          <View style={styles.tagRow}>
            <View style={styles.limeDot} />
            <Text style={styles.tagText}>PUBLIC RESOURCE, NEARBY</Text>
          </View>
          <Text style={styles.heroTitle}>
            필요한 건,{'\n'}
            <Text style={{ color: colors.accentLime }}>가까이서 빌려요.</Text>
          </Text>
          <Text style={styles.heroDesc}>흩어진 공공 대여 자원을{'\n'}내 동네 기준으로 모아드려요</Text>
        </View>
      </View>

      {/* 검색바 */}
      <Pressable onPress={() => router.push('/search')} style={styles.searchBar}>
        <Svg width={16} height={16} viewBox="0 0 24 24" fill="none" stroke={colors.ink3} strokeWidth={2.2}>
          <Circle cx="11" cy="11" r="7" /><Path d="M21 21l-4.3-4.3" />
        </Svg>
        <Text style={styles.searchPlaceholder}>무엇이 필요하세요?</Text>
      </Pressable>

      {/* 카테고리 */}
      <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.catRow} contentContainerStyle={{ gap: 18, paddingHorizontal: 20 }}>
        {categories.map((c, i) => {
          const active = i === 0;
          return (
            <View key={c.key} style={styles.catItem}>
              <View style={[styles.catCircle, active && { backgroundColor: colors.brand, borderWidth: 0 }]}>
                <Svg width={20} height={20} viewBox="0 0 24 24" fill="none" stroke={active ? '#fff' : colors.ink2} strokeWidth={2}>
                  {c.icon}
                </Svg>
              </View>
              <Text style={[styles.catLabel, active && { color: colors.brand, fontWeight: '800' }]}>{c.label}</Text>
            </View>
          );
        })}
      </ScrollView>

      {/* 오늘 빌릴 수 있어요 */}
      <View style={{ paddingHorizontal: 20, paddingTop: 12 }}>
        <View style={styles.sectionHead}>
          <View>
            <Text style={styles.sectionEyebrow}>AVAILABLE TODAY</Text>
            <Text style={styles.sectionTitle}>오늘 빌릴 수 있어요</Text>
          </View>
          <Pressable onPress={() => router.push('/map')}>
            <Text style={styles.sectionMore}>전체 보기 ›</Text>
          </Pressable>
        </View>

        {mockItems.map((item) => (
          <ResourceCard
            key={item.id}
            item={{ ...item, bookmarked: bookmarked.has(item.id) }}
            onPress={() => router.push(`/resources/${item.id}`)}
            onToggleBookmark={() => toggle(item.id)}
          />
        ))}

        <Pressable style={styles.alertBanner} onPress={() => router.push('/my')}>
          <View style={styles.alertIcon}>
            <Svg width={16} height={16} viewBox="0 0 24 24" fill="none" stroke={colors.accentLime} strokeWidth={2}>
              <Path d="M18 8a6 6 0 1 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" />
              <Path d="M13.7 21a2 2 0 0 1-3.4 0" />
            </Svg>
          </View>
          <View style={{ flex: 1 }}>
            <Text style={styles.alertTitle}>관심 지역알림</Text>
            <Text style={styles.alertDesc}>새로 뜬 대여 자원을 놓치지 마세요.</Text>
          </View>
          <Svg width={16} height={16} viewBox="0 0 24 24" fill="none" stroke={colors.accentLime} strokeWidth={2.4}>
            <Path d="M9 6l6 6-6 6" />
          </Svg>
        </Pressable>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  hero: { height: 200, marginHorizontal: 20, marginBottom: 18, borderRadius: radius.xl, overflow: 'hidden', backgroundColor: '#1B2430' },
  heroContent: { flex: 1, justifyContent: 'flex-end', padding: 20 },
  tagRow: { flexDirection: 'row', alignItems: 'center', gap: 6, marginBottom: 10 },
  limeDot: { width: 5, height: 5, borderRadius: 2.5, backgroundColor: colors.accentLime },
  tagText: { fontSize: 10.5, fontWeight: '800', letterSpacing: 1, color: colors.accentLime },
  heroTitle: { fontSize: 22, fontWeight: '800', lineHeight: 28, color: '#fff' },
  heroDesc: { fontSize: 12, color: 'rgba(255,255,255,0.78)', marginTop: 8, lineHeight: 17 },
  searchBar: {
    marginHorizontal: 20, marginBottom: 18, height: 48, borderRadius: radius.full,
    backgroundColor: colors.surface, borderWidth: 1, borderColor: colors.line,
    flexDirection: 'row', alignItems: 'center', gap: 10, paddingHorizontal: 18,
  },
  searchPlaceholder: { fontSize: 14, color: colors.ink3 },
  catRow: { marginBottom: 26, flexGrow: 0 },
  catItem: { alignItems: 'center', gap: 7, width: 50 },
  catCircle: { width: 50, height: 50, borderRadius: 25, borderWidth: 1, borderColor: colors.line, alignItems: 'center', justifyContent: 'center' },
  catLabel: { fontSize: 11.5, color: colors.ink3 },
  sectionHead: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-end', marginBottom: 14 },
  sectionEyebrow: { fontSize: 10.5, fontWeight: '800', letterSpacing: 1, color: colors.brand, marginBottom: 4 },
  sectionTitle: { fontSize: 17, fontWeight: '800', color: colors.ink },
  sectionMore: { fontSize: 12.5, color: colors.ink3, fontWeight: '700' },
  alertBanner: {
    flexDirection: 'row', alignItems: 'center', gap: 12,
    backgroundColor: '#161C26', borderRadius: radius.md, padding: 16, marginTop: 4,
  },
  alertIcon: { width: 32, height: 32, borderRadius: 16, backgroundColor: 'rgba(214,242,78,0.15)', alignItems: 'center', justifyContent: 'center' },
  alertTitle: { fontSize: 13.5, fontWeight: '800', color: '#fff', marginBottom: 2 },
  alertDesc: { fontSize: 11, color: 'rgba(255,255,255,0.65)' },
});
