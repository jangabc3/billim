import { View, Text, StyleSheet } from 'react-native';
import Svg, { Path, Circle } from 'react-native-svg';
import { colors, radius } from '../theme/tokens';

export default function TopBar() {
  return (
    <View style={styles.row}>
      <View style={styles.logo}>
        <Text style={styles.logoNum}>8</Text>
        <Text style={styles.logoText}>빌림</Text>
      </View>
      <View style={styles.right}>
        <View style={styles.loc}>
          <Svg width={13} height={13} viewBox="0 0 24 24" fill="none" stroke={colors.ink2} strokeWidth={2}>
            <Path d="M12 22s7-6.5 7-12a7 7 0 1 0-14 0c0 5.5 7 12 7 12z" />
            <Circle cx="12" cy="10" r="2.5" />
          </Svg>
          <Text style={styles.locText}>서울 성동구</Text>
        </View>
        <View style={styles.iconBtn}>
          <Svg width={15} height={15} viewBox="0 0 24 24" fill="none" stroke={colors.ink} strokeWidth={2}>
            <Circle cx="12" cy="12" r="9" />
            <Path d="M12 8v4l2.5 2.5" />
          </Svg>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 20, paddingTop: 8, paddingBottom: 14 },
  logo: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  logoNum: { fontSize: 20, fontWeight: '900', color: colors.brand },
  logoText: { fontSize: 17, fontWeight: '800', color: colors.ink },
  right: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  loc: { flexDirection: 'row', alignItems: 'center', gap: 4, paddingHorizontal: 12, paddingVertical: 7, borderRadius: radius.full, backgroundColor: colors.grayFill },
  locText: { fontSize: 12.5, fontWeight: '700', color: colors.ink2 },
  iconBtn: { width: 34, height: 34, borderRadius: 17, borderWidth: 1, borderColor: colors.line, alignItems: 'center', justifyContent: 'center' },
});
