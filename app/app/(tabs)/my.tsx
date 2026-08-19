import { View, Text, ScrollView, Pressable, StyleSheet } from 'react-native';
import Svg, { Path, Rect } from 'react-native-svg';
import TopBar from '../../src/components/TopBar';
import { useBookmarks } from '../../src/contexts/BookmarkContext';
import { colors, radius } from '../../src/theme/tokens';

const menuRows = [
  { label: '관심 지역 설정', desc: '성동구를 기준으로 보고 있어요', icon: <><Path d="M12 22s7-6.5 7-12a7 7 0 1 0-14 0c0 5.5 7 12 7 12z" /></> },
  { label: '신청 일정 알림', desc: '놓치지 않도록 안내해드릴게요', icon: <><Rect x="4" y="5" width="16" height="16" rx="2" /><Path d="M8 3v4M16 3v4M4 10h16" /></> },
  { label: '정보 오류 제보', desc: '정확한 정보를 함께 만들어요', icon: <><Rect x="3" y="6" width="18" height="14" rx="2" /><Path d="M3 10h18" /></> },
];

export default function MyScreen() {
  const { bookmarked } = useBookmarks();

  return (
    <ScrollView style={{ flex: 1, backgroundColor: colors.surface }} contentContainerStyle={{ paddingBottom: 20 }}>
      <TopBar />

      <View style={styles.greeting}>
        <View style={styles.avatar}><Text style={styles.avatarText}>빌</Text></View>
        <View>
          <Text style={styles.hello}>안녕하세요</Text>
          <Text style={styles.greetTitle}>빌림을 시작해 볼까요?</Text>
        </View>
      </View>

      <View style={styles.statBar}>
        <StatCell value={String(bookmarked.size)} label="관심 자원" />
        <View style={styles.divider} />
        <StatCell value="0" label="진행 중인 대여" />
        <View style={styles.divider} />
        <StatCell value="성동구" label="관심 지역" highlight />
      </View>

      <View style={{ paddingHorizontal: 20 }}>
        {menuRows.map((row) => (
          <Pressable key={row.label} style={styles.menuRow}>
            <View style={styles.menuIcon}>
              <Svg width={17} height={17} viewBox="0 0 24 24" fill="none" stroke={colors.brand} strokeWidth={2}>
                {row.icon}
              </Svg>
            </View>
            <View style={{ flex: 1 }}>
              <Text style={styles.menuLabel}>{row.label}</Text>
              <Text style={styles.menuDesc}>{row.desc}</Text>
            </View>
            <Svg width={14} height={14} viewBox="0 0 24 24" fill="none" stroke={colors.ink3} strokeWidth={2.4}>
              <Path d="M9 6l6 6-6 6" />
            </Svg>
          </Pressable>
        ))}
      </View>

      <View style={styles.infoBox}>
        <Text style={styles.infoTitle}>빌림이 하는 일</Text>
        <Text style={styles.infoDesc}>기관마다 다른 대여 정보를 가까운 거리와 이용 조건을 기준으로 정리해 드립니다.</Text>
      </View>
    </ScrollView>
  );
}

function StatCell({ value, label, highlight }: { value: string; label: string; highlight?: boolean }) {
  return (
    <View style={{ flex: 1, alignItems: 'center' }}>
      <Text style={[styles.statValue, highlight && { color: colors.accentLime }]}>{value}</Text>
      <Text style={styles.statLabel}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  greeting: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingHorizontal: 20, paddingTop: 4, paddingBottom: 18 },
  avatar: { width: 46, height: 46, borderRadius: 14, backgroundColor: colors.accentLime, alignItems: 'center', justifyContent: 'center' },
  avatarText: { fontSize: 15, fontWeight: '800' },
  hello: { fontSize: 11.5, color: colors.ink3, marginBottom: 2 },
  greetTitle: { fontSize: 17, fontWeight: '800', color: colors.ink },
  statBar: { flexDirection: 'row', marginHorizontal: 20, marginBottom: 22, borderRadius: radius.md, backgroundColor: '#161C26', paddingVertical: 18 },
  divider: { width: 1, backgroundColor: 'rgba(255,255,255,0.12)' },
  statValue: { fontSize: 15, fontWeight: '800', color: '#fff' },
  statLabel: { fontSize: 10.5, color: 'rgba(255,255,255,0.6)', marginTop: 3 },
  menuRow: { flexDirection: 'row', alignItems: 'center', gap: 14, paddingVertical: 14, borderTopWidth: 1, borderTopColor: colors.line },
  menuIcon: { width: 38, height: 38, borderRadius: 12, backgroundColor: colors.grayFill, alignItems: 'center', justifyContent: 'center' },
  menuLabel: { fontSize: 14, fontWeight: '700', color: colors.ink },
  menuDesc: { fontSize: 11.5, color: colors.ink3, marginTop: 2 },
  infoBox: { marginHorizontal: 20, marginTop: 4, padding: 16, borderRadius: radius.md, backgroundColor: colors.brandTint },
  infoTitle: { fontSize: 13, fontWeight: '800', color: colors.brandStrong, marginBottom: 6 },
  infoDesc: { fontSize: 12, color: colors.ink2, lineHeight: 18 },
});
