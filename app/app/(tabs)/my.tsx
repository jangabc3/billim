import { useEffect, useState } from 'react';
import { View, Text, ScrollView, Pressable, ActivityIndicator, StyleSheet } from 'react-native';
import { useRouter } from 'expo-router';
import Svg, { Path, Rect } from 'react-native-svg';
import TopBar from '../../src/components/TopBar';
import { useBookmarks } from '../../src/contexts/BookmarkContext';
import { useAuth } from '../../src/contexts/AuthContext';
import { reservationApi, ApiError, type ReservationResponse } from '../../src/api/client';
import { colors, radius } from '../../src/theme/tokens';

const menuRows = [
  { label: '관심 지역 설정', desc: '성동구를 기준으로 보고있어요', icon: <><Path d="M12 22s7-6.5 7-12a7 7 0 1 0-14 0c0 5.5 7 12 7 12z" /></> },
  { label: '신청 일정 알림', desc: '놓치지 않도록 안내해드릴게요', icon: <><Rect x="4" y="5" width="16" height="16" rx="2" /><Path d="M8 3v4M16 3v4M4 10h16" /></> },
  { label: '정보 오류 제보', desc: '정확한 정보를 함께 만들어요', icon: <><Rect x="3" y="6" width="18" height="14" rx="2" /><Path d="M3 10h18" /></> },
];

const STATUS_LABEL: Record<ReservationResponse['status'], string> = {
  CONFIRMED: '예약중',
  RENTED: '대여중',
  RETURNED: '반납완료',
  CANCELED: '취소됨',
  EXPIRED: '기간만료',
};

const STATUS_COLOR: Record<ReservationResponse['status'], string> = {
  CONFIRMED: colors.brand,
  RENTED: colors.accentLime,
  RETURNED: colors.ink3,
  CANCELED: '#E0453C',
  EXPIRED: colors.ink3,
};

const ACTIVE_STATUSES: ReservationResponse['status'][] = ['CONFIRMED', 'RENTED'];

export default function MyScreen() {
  const router = useRouter();
  const { bookmarked } = useBookmarks();
  const { isLoggedIn, userName, loading, logout } = useAuth();

  const [reservations, setReservations] = useState<ReservationResponse[]>([]);
  const [resLoading, setResLoading] = useState(true);
  const [resError, setResError] = useState<string | null>(null);

  useEffect(() => {
    if (loading) return;

    if (!isLoggedIn) {
      setReservations([]);
      setResLoading(false);
      return;
    }

    setResLoading(true);
    setResError(null);
    reservationApi
      .list()
      .then(setReservations)
      .catch((e) => setResError(e instanceof ApiError ? e.message : '예약 목록을 불러오지 못했어요.'))
      .finally(() => setResLoading(false));
  }, [isLoggedIn, loading]);

  const activeCount = reservations.filter((r) => ACTIVE_STATUSES.includes(r.status)).length;

  return (
    <ScrollView style={{ flex: 1, backgroundColor: colors.surface }} contentContainerStyle={{ paddingBottom: 20 }}>
      <TopBar />

      {!loading && !isLoggedIn && (
        <View style={styles.authCard}>
          <Text style={styles.authTitle}>로그인하고 시작해보세요</Text>
          <Text style={styles.authDesc}>예약 현황과 대기 순번을 확인할 수 있어요.</Text>
          <View style={styles.authBtnRow}>
            <Pressable style={styles.loginBtn} onPress={() => router.push('/login')}>
              <Text style={styles.loginBtnText}>로그인</Text>
            </Pressable>
            <Pressable style={styles.signupBtn} onPress={() => router.push('/signup')}>
              <Text style={styles.signupBtnText}>회원가입</Text>
            </Pressable>
          </View>
        </View>
      )}

      {!loading && isLoggedIn && (
        <View style={styles.greeting}>
          <View style={styles.avatar}>
            <Text style={styles.avatarText}>{userName ? userName[0] : '빌'}</Text>
          </View>
          <View>
            <Text style={styles.hello}>안녕하세요</Text>
            <Text style={styles.greetTitle}>{userName ?? '회원'}님, 반가워요!</Text>
          </View>
        </View>
      )}

      <View style={styles.statBar}>
        <StatCell value={String(bookmarked.size)} label="관심 자원" />
        <View style={styles.divider} />
        <StatCell value={String(activeCount)} label="진행 중인 대여" />
        <View style={styles.divider} />
        <StatCell value="성동구" label="관심 지역" highlight />
      </View>

      {!loading && isLoggedIn && (
        <View style={{ paddingHorizontal: 20, marginBottom: 22 }}>
          <Text style={styles.sectionTitle}>내 예약</Text>

          {resLoading && (
            <View style={{ paddingVertical: 24, alignItems: 'center' }}>
              <ActivityIndicator color={colors.brand} />
            </View>
          )}

          {!resLoading && resError && (
            <Text style={{ fontSize: 12.5, color: colors.ink3, textAlign: 'center', paddingVertical: 16 }}>{resError}</Text>
          )}

          {!resLoading && !resError && reservations.length === 0 && (
            <Text style={{ fontSize: 12.5, color: colors.ink3, paddingVertical: 16 }}>아직 예약한 자원이 없어요.</Text>
          )}

          {!resLoading && !resError && reservations.map((r) => (
            <View key={r.id} style={styles.reservationRow}>
              <View style={{ flex: 1 }}>
                <Text style={styles.reservationName}>{r.rentalItemName}</Text>
                <Text style={styles.reservationDate}>
                  {new Date(r.confirmedAt).toLocaleDateString('ko-KR')} ~ {new Date(r.expiresAt).toLocaleDateString('ko-KR')}
                </Text>
              </View>
              <View style={[styles.statusBadge, { backgroundColor: STATUS_COLOR[r.status] + '22' }]}>
                <Text style={[styles.statusText, { color: STATUS_COLOR[r.status] }]}>{STATUS_LABEL[r.status]}</Text>
              </View>
            </View>
          ))}
        </View>
      )}

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

        {!loading && isLoggedIn && (
          <Pressable style={styles.menuRow} onPress={logout}>
            <View style={styles.menuIcon}>
              <Svg width={17} height={17} viewBox="0 0 24 24" fill="none" stroke="#E0453C" strokeWidth={2}>
                <Path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                <Path d="M16 17l5-5-5-5" />
                <Path d="M21 12H9" />
              </Svg>
            </View>
            <View style={{ flex: 1 }}>
              <Text style={[styles.menuLabel, { color: '#E0453C' }]}>로그아웃</Text>
            </View>
          </Pressable>
        )}
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
  authCard: { marginHorizontal: 20, marginBottom: 18, padding: 20, borderRadius: radius.lg, backgroundColor: colors.brandTint },
  authTitle: { fontSize: 15.5, fontWeight: '800', color: colors.ink, marginBottom: 4 },
  authDesc: { fontSize: 12, color: colors.ink3, marginBottom: 14 },
  authBtnRow: { flexDirection: 'row', gap: 8 },
  loginBtn: { flex: 1, height: 42, borderRadius: radius.md, backgroundColor: colors.brand, alignItems: 'center', justifyContent: 'center' },
  loginBtnText: { fontSize: 13, fontWeight: '800', color: '#fff' },
  signupBtn: { flex: 1, height: 42, borderRadius: radius.md, borderWidth: 1, borderColor: colors.brand, alignItems: 'center', justifyContent: 'center' },
  signupBtnText: { fontSize: 13, fontWeight: '800', color: colors.brand },
  greeting: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingHorizontal: 20, paddingTop: 4, paddingBottom: 18 },
  avatar: { width: 46, height: 46, borderRadius: 14, backgroundColor: colors.accentLime, alignItems: 'center', justifyContent: 'center' },
  avatarText: { fontSize: 15, fontWeight: '800' },
  hello: { fontSize: 11.5, color: colors.ink3, marginBottom: 2 },
  greetTitle: { fontSize: 17, fontWeight: '800', color: colors.ink },
  statBar: { flexDirection: 'row', marginHorizontal: 20, marginBottom: 22, borderRadius: radius.md, backgroundColor: '#161C26', paddingVertical: 18 },
  divider: { width: 1, backgroundColor: 'rgba(255,255,255,0.12)' },
  statValue: { fontSize: 15, fontWeight: '800', color: '#fff' },
  statLabel: { fontSize: 10.5, color: 'rgba(255,255,255,0.6)', marginTop: 3 },
  sectionTitle: { fontSize: 14, fontWeight: '800', color: colors.ink, marginBottom: 10 },
  reservationRow: { flexDirection: 'row', alignItems: 'center', paddingVertical: 12, borderTopWidth: 1, borderTopColor: colors.line },
  reservationName: { fontSize: 13.5, fontWeight: '700', color: colors.ink },
  reservationDate: { fontSize: 11, color: colors.ink3, marginTop: 3 },
  statusBadge: { paddingHorizontal: 10, paddingVertical: 5, borderRadius: 8 },
  statusText: { fontSize: 11, fontWeight: '800' },
  menuRow: { flexDirection: 'row', alignItems: 'center', gap: 14, paddingVertical: 14, borderTopWidth: 1, borderTopColor: colors.line },
  menuIcon: { width: 38, height: 38, borderRadius: 12, backgroundColor: colors.grayFill, alignItems: 'center', justifyContent: 'center' },
  menuLabel: { fontSize: 14, fontWeight: '700', color: colors.ink },
  menuDesc: { fontSize: 11.5, color: colors.ink3, marginTop: 2 },
  infoBox: { marginHorizontal: 20, marginTop: 4, padding: 16, borderRadius: radius.md, backgroundColor: colors.brandTint },
  infoTitle: { fontSize: 13, fontWeight: '800', color: colors.brandStrong, marginBottom: 6 },
  infoDesc: { fontSize: 12, color: colors.ink2, lineHeight: 18 },
});