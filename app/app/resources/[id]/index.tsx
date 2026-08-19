import { useEffect, useState } from 'react';
import { View, Text, Image, Pressable, ScrollView, StyleSheet, Linking } from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import Svg, { Path, Rect, Circle } from 'react-native-svg';
import { resourceApi } from '../../../src/api/client';
import type { PublicResource } from '../../../src/types/resource';
import { colors, radius } from '../../../src/theme/tokens';

export default function DetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const router = useRouter();
  const [item, setItem] = useState<PublicResource | null>(null);
  const [heart, setHeart] = useState(false);

  useEffect(() => {
    // TODO: 백엔드에 GET /api/v1/resources/{id} 생기면 교체
    resourceApi.list().then((all) => {
      setItem(all.find((r) => String(r.id) === id) ?? null);
    });
  }, [id]);

  if (!item) return <View style={{ flex: 1, backgroundColor: colors.surface }} />;

  return (
    <ScrollView style={{ flex: 1, backgroundColor: colors.surface }}>
      <View style={styles.heroPhoto}>
        <Image source={{ uri: 'https://example.com/item.jpg' }} style={StyleSheet.absoluteFill} resizeMode="cover" />
        <Svg width={80} height={80} viewBox="0 0 24 24" fill="none" stroke={colors.brand} strokeWidth={1.4}>
          <Rect x="4" y="4" width="16" height="16" rx="2" />
        </Svg>
        <Pressable onPress={() => router.back()} style={[styles.floatBtn, { left: 20 }]}>
          <Svg width={17} height={17} viewBox="0 0 24 24" fill="none" stroke={colors.ink} strokeWidth={2.4}>
            <Path d="M15 18l-6-6 6-6" />
          </Svg>
        </Pressable>
        <Pressable onPress={() => setHeart(!heart)} style={[styles.floatBtn, { right: 20 }]}>
          <Svg width={17} height={17} viewBox="0 0 24 24" fill="none" stroke={heart ? colors.urgent : colors.ink} strokeWidth={2}>
            <Path d="M20.8 8.6c0 5.4-8.8 10.4-8.8 10.4S3.2 14 3.2 8.6a4.6 4.6 0 0 1 8.8-1.9 4.6 4.6 0 0 1 8.8 1.9z" />
          </Svg>
        </Pressable>
      </View>

      <View style={styles.pad}>
        <View style={styles.srcLine}>
          <View style={styles.srcTag}><Text style={styles.srcTagText}>{sourceLabel(item.source)}</Text></View>
          {item.lastSyncedAt && (
            <Text style={styles.updated}>{new Date(item.lastSyncedAt).toLocaleString('ko-KR')} 업데이트</Text>
          )}
        </View>
        <Text style={styles.title}>{item.name}</Text>

        <View style={styles.facts}>
          <FactRow label="이용료" value={item.fee ?? '확인 필요'} first />
          <FactRow label="구" value={item.gu} />
          <FactRow label="위치" value={item.address} />
        </View>

        <Pressable
          style={styles.ctaPrimary}
          onPress={() => item.reservationUrl && Linking.openURL(item.reservationUrl)}
        >
          <Text style={styles.ctaPrimaryText}>
            {item.reservationType === 'DIRECT_BOOKING' ? '바로 예약하기' : '공식 예약처에서 확인'}
          </Text>
        </Pressable>
        <Pressable style={styles.ctaSecondary}>
          <Text style={styles.ctaSecondaryText}>신청 시작·마감 알림 받기</Text>
        </Pressable>

        <Text style={styles.caption}>
          {item.source === 'BILLIM_PARTNER'
            ? '빌림이 직접 관리하는 자원이에요. 예약은 앱 안에서 바로 처리돼요.'
            : `${sourceLabel(item.source)}에서 제공하는 정보예요. 실제 예약 가능 여부는 공식 예약처에서 최종 확인해주세요.`}
        </Text>
      </View>
    </ScrollView>
  );
}

function FactRow({ label, value, first }: { label: string; value: string; first?: boolean }) {
  return (
    <View style={[styles.factRow, !first && { borderTopWidth: 1, borderTopColor: colors.line }]}>
      <Text style={styles.factLabel}>{label}</Text>
      <Text style={styles.factValue}>{value}</Text>
    </View>
  );
}

function sourceLabel(source: PublicResource['source']) {
  if (source === 'SHARENURI') return '공유누리 제공';
  if (source === 'SEOUL_RESERVATION') return '서울시 제공';
  return '빌림 제휴 기관';
}

const styles = StyleSheet.create({
  heroPhoto: { height: 260, backgroundColor: colors.brandTint, alignItems: 'center', justifyContent: 'center' },
  floatBtn: { position: 'absolute', top: 50, width: 38, height: 38, borderRadius: 19, backgroundColor: colors.surface, alignItems: 'center', justifyContent: 'center', elevation: 3, shadowColor: '#000', shadowOpacity: 0.15, shadowRadius: 6, shadowOffset: { width: 0, height: 2 } },
  pad: { padding: 22 },
  srcLine: { flexDirection: 'row', alignItems: 'center', gap: 8, marginBottom: 11, flexWrap: 'wrap' },
  srcTag: { backgroundColor: colors.grayFill, borderRadius: radius.full, paddingHorizontal: 8, paddingVertical: 3 },
  srcTagText: { fontSize: 10.5, fontWeight: '700', color: colors.ink2 },
  updated: { fontSize: 11.5, color: colors.ink3 },
  title: { fontSize: 22, fontWeight: '800', marginBottom: 18, color: colors.ink, lineHeight: 28 },
  facts: { marginBottom: 18 },
  factRow: { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 13 },
  factLabel: { fontSize: 13, color: colors.ink2, fontWeight: '600' },
  factValue: { fontSize: 13.5, fontWeight: '800', color: colors.ink, flexShrink: 1, textAlign: 'right' },
  ctaPrimary: { height: 54, borderRadius: radius.md, backgroundColor: colors.brand, alignItems: 'center', justifyContent: 'center', marginBottom: 10 },
  ctaPrimaryText: { color: '#fff', fontSize: 15.5, fontWeight: '800' },
  ctaSecondary: { height: 54, borderRadius: radius.md, backgroundColor: colors.grayFill, alignItems: 'center', justifyContent: 'center' },
  ctaSecondaryText: { color: colors.ink, fontSize: 14.5, fontWeight: '800' },
  caption: { textAlign: 'center', fontSize: 11.5, color: colors.ink3, marginTop: 13, lineHeight: 17, paddingBottom: 20 },
});