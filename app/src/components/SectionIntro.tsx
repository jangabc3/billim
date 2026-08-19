import { View, Text, StyleSheet } from 'react-native';
import { colors } from '../theme/tokens';

export default function SectionIntro({
  eyebrow, title, highlight, desc,
}: {
  eyebrow: string;
  title: string;
  highlight?: string;
  desc?: string;
}) {
  return (
    <View style={styles.wrap}>
      <View style={styles.tagRow}>
        <View style={styles.dot} />
        <Text style={styles.eyebrow}>{eyebrow.toUpperCase()}</Text>
      </View>
      <Text style={styles.title}>
        {title}
        {highlight ? '\n' : ''}
        {highlight && <Text style={styles.highlight}>{highlight}</Text>}
      </Text>
      {desc && <Text style={styles.desc}>{desc}</Text>}
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { paddingHorizontal: 20, paddingTop: 4, paddingBottom: 20 },
  tagRow: { flexDirection: 'row', alignItems: 'center', gap: 6, marginBottom: 8 },
  dot: { width: 5, height: 5, borderRadius: 2.5, backgroundColor: colors.brand },
  eyebrow: { fontSize: 10.5, fontWeight: '800', letterSpacing: 1, color: colors.brand },
  title: { fontSize: 20, fontWeight: '800', lineHeight: 26, color: colors.ink },
  highlight: { color: colors.brand },
  desc: { fontSize: 12.5, color: colors.ink3, marginTop: 8, lineHeight: 18 },
});
