import { View, Text, Image, Pressable, StyleSheet } from 'react-native';
import Svg, { Path, Rect } from 'react-native-svg';
import { colors, radius } from '../theme/tokens';

export interface ResourceCardItem {
  id: string;
  name: string;
  org: string;
  distance: string;
  fee: string;
  photo: string;
  badges: { label: string; tone: 'urgent' | 'brand' | 'neutral' | 'new' }[];
  bookmarked?: boolean;
}

export default function ResourceCard({
  item, onPress, onToggleBookmark,
}: {
  item: ResourceCardItem;
  onPress: () => void;
  onToggleBookmark?: () => void;
}) {
  return (
    <Pressable onPress={onPress} style={styles.card}>
      <View style={styles.thumb}>
        {/* RN의 <Image>는 onError 시 자동으로 안 사라지므로, source 없을 때 아이콘만 보이게 처리 */}
        <Image source={{ uri: item.photo }} style={StyleSheet.absoluteFill} resizeMode="cover" />
        <Svg width={26} height={26} viewBox="0 0 24 24" fill="none" stroke={colors.brand} strokeWidth={1.6}>
          <Rect x="4" y="4" width="16" height="16" rx="2" />
        </Svg>
      </View>

      <View style={{ flex: 1 }}>
        <View style={styles.badgeRow}>
          {item.badges.map((b) => (
            <View key={b.label} style={[styles.badge, badgeStyle(b.tone)]}>
              <Text style={[styles.badgeText, badgeTextStyle(b.tone)]}>{b.label}</Text>
            </View>
          ))}
        </View>
        <Text style={styles.name}>{item.name}</Text>
        <Text style={styles.org}>{item.org}</Text>
        <Text style={styles.meta}>{item.distance} · {item.fee}</Text>
      </View>

      <Pressable onPress={onToggleBookmark} hitSlop={8} style={styles.bookmarkBtn}>
        <Svg width={18} height={18} viewBox="0 0 24 24"
          fill={item.bookmarked ? colors.brand : 'none'}
          stroke={item.bookmarked ? colors.brand : colors.ink3} strokeWidth={2}>
          <Path d="M6 4h12a1 1 0 0 1 1 1v15l-7-4-7 4V5a1 1 0 0 1 1-1z" />
        </Svg>
      </Pressable>
    </Pressable>
  );
}

function badgeStyle(tone: 'urgent' | 'brand' | 'neutral' | 'new') {
  if (tone === 'urgent') return { backgroundColor: colors.urgentTint };
  if (tone === 'brand') return { backgroundColor: colors.brandTint };
  if (tone === 'new') return { backgroundColor: colors.newToneTint };
  return { backgroundColor: colors.grayFill };
}
function badgeTextStyle(tone: 'urgent' | 'brand' | 'neutral' | 'new') {
  if (tone === 'urgent') return { color: colors.urgent };
  if (tone === 'brand') return { color: colors.brandStrong };
  if (tone === 'new') return { color: colors.newTone };
  return { color: colors.ink3 };
}

const styles = StyleSheet.create({
  card: {
    flexDirection: 'row', gap: 13, padding: 14, borderRadius: radius.lg,
    backgroundColor: colors.surface, borderWidth: 1, borderColor: colors.line,
    marginBottom: 12,
  },
  thumb: {
    width: 76, height: 76, borderRadius: radius.md, backgroundColor: colors.grayFill,
    alignItems: 'center', justifyContent: 'center', overflow: 'hidden',
  },
  badgeRow: { flexDirection: 'row', gap: 5, marginBottom: 7 },
  badge: { paddingHorizontal: 7, paddingVertical: 3, borderRadius: 6 },
  badgeText: { fontSize: 10, fontWeight: '700' },
  name: { fontSize: 14.5, fontWeight: '800', marginBottom: 3, color: colors.ink },
  org: { fontSize: 11.5, color: colors.ink3, marginBottom: 8 },
  meta: { fontSize: 11.5, color: colors.ink2, fontWeight: '600' },
  bookmarkBtn: { position: 'absolute', top: 12, right: 12 },
});
