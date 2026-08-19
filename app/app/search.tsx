import { useEffect, useState } from 'react';
import { View, Text, Pressable, ScrollView, ActivityIndicator, StyleSheet } from 'react-native';
import { useRouter, Stack } from 'expo-router';
import Svg, { Path, Circle, Rect } from 'react-native-svg';
import { resourceApi } from '../src/api/client';
import type { PublicResource } from '../src/types/resource';
import { colors, radius } from '../src/theme/tokens';

type LoadState = 'loading' | 'success' | 'empty' | 'error';

export default function SearchScreen() {
  const [items, setItems] = useState<PublicResource[]>([]);
  const [state, setState] = useState<LoadState>('loading');
  const router = useRouter();

  const load = () => {
    setState('loading');
    resourceApi.list()
      .then((data) => { setItems(data); setState(data.length === 0 ? 'empty' : 'success'); })
      .catch(() => setState('error'));
  };

  useEffect(load, []);

  return (
    <View style={{ flex: 1, backgroundColor: colors.surface }}>
      <Stack.Screen options={{ headerShown: true, title: '검색', headerBackTitle: '홈' }} />

      {state === 'loading' && (
        <View style={styles.center}><ActivityIndicator color={colors.brand} /></View>
      )}
      {state === 'error' && (
        <StateView title="연결이 원활하지 않아요" desc="네트워크 상태를 확인하고 다시 시도해주세요." actionLabel="다시 시도" onAction={load} />
      )}
      {state === 'empty' && (
        <StateView title="검색 결과가 없어요" desc="아직 등록된 자원이 없어요. 잠시 후 다시 확인해주세요." actionLabel="새로고침" onAction={load} />
      )}
      {state === 'success' && (
        <ScrollView contentContainerStyle={{ padding: 20 }}>
          {items.map((item) => (
            <Pressable key={item.id} onPress={() => router.push(`/resources/${item.id}`)} style={styles.row}>
              <View style={styles.thumb}>
                <Svg width={24} height={24} viewBox="0 0 24 24" fill="none" stroke={colors.brand} strokeWidth={1.6}>
                  <Rect x="4" y="4" width="16" height="16" rx="2" />
                </Svg>
              </View>
              <View style={{ flex: 1 }}>
                <Text style={styles.name} numberOfLines={2}>{item.name}</Text>
                <Text style={styles.addr} numberOfLines={1}>{item.address}</Text>
                <View style={styles.feeBadge}><Text style={styles.feeBadgeText}>{item.fee ?? '확인 필요'}</Text></View>
              </View>
            </Pressable>
          ))}
        </ScrollView>
      )}
    </View>
  );
}

function StateView({ title, desc, actionLabel, onAction }: {
  title: string; desc: string; actionLabel: string; onAction: () => void;
}) {
  return (
    <View style={styles.stateView}>
      <View style={styles.stateIcon}>
        <Svg width={28} height={28} viewBox="0 0 24 24" fill="none" stroke={colors.ink3} strokeWidth={2}>
          <Circle cx="11" cy="11" r="7" /><Path d="M21 21l-4.3-4.3" />
        </Svg>
      </View>
      <Text style={styles.stateTitle}>{title}</Text>
      <Text style={styles.stateDesc}>{desc}</Text>
      <Pressable onPress={onAction} style={styles.stateBtn}>
        <Text style={styles.stateBtnText}>{actionLabel}</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  row: { flexDirection: 'row', gap: 13, paddingVertical: 15, borderTopWidth: 1, borderTopColor: colors.line },
  thumb: { width: 58, height: 58, borderRadius: 14, backgroundColor: colors.grayFill, alignItems: 'center', justifyContent: 'center' },
  name: { fontSize: 14.5, fontWeight: '800', marginBottom: 5, color: colors.ink },
  addr: { fontSize: 11.5, color: colors.ink3, marginBottom: 8 },
  feeBadge: { alignSelf: 'flex-start', backgroundColor: colors.brandTint, borderRadius: radius.full, paddingHorizontal: 8, paddingVertical: 4 },
  feeBadgeText: { fontSize: 10.5, fontWeight: '700', color: colors.brandStrong },
  stateView: { flex: 1, alignItems: 'center', paddingTop: 90, paddingHorizontal: 40 },
  stateIcon: { width: 64, height: 64, borderRadius: 32, backgroundColor: colors.grayFill, alignItems: 'center', justifyContent: 'center', marginBottom: 18 },
  stateTitle: { fontSize: 15, fontWeight: '800', marginBottom: 8, color: colors.ink },
  stateDesc: { fontSize: 12.5, color: colors.ink3, textAlign: 'center', lineHeight: 18, marginBottom: 20 },
  stateBtn: { paddingHorizontal: 18, paddingVertical: 10, borderRadius: 12, borderWidth: 1.5, borderColor: colors.line },
  stateBtnText: { fontSize: 13, fontWeight: '800', color: colors.ink },
});
