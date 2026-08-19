import { Tabs } from 'expo-router';
import Svg, { Path, Rect, Circle } from 'react-native-svg';
import { colors } from '../../src/theme/tokens';

export default function TabLayout() {
  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: colors.brand,
        tabBarInactiveTintColor: colors.ink3,
        tabBarStyle: { borderTopColor: colors.line, height: 84, paddingTop: 8, paddingBottom: 24 },
        tabBarLabelStyle: { fontSize: 10, fontWeight: '700' },
      }}
    >
      <Tabs.Screen
        name="index"
        options={{ title: '홈', tabBarIcon: ({ color }) => <HomeIcon color={color} /> }}
      />
      <Tabs.Screen
        name="map"
        options={{ title: '지도', tabBarIcon: ({ color }) => <MapIcon color={color} /> }}
      />
      <Tabs.Screen
        name="bookmarks"
        options={{ title: '저장', tabBarIcon: ({ color }) => <BookmarkIcon color={color} /> }}
      />
      <Tabs.Screen
        name="my"
        options={{ title: '마이', tabBarIcon: ({ color }) => <MyIcon color={color} /> }}
      />
    </Tabs>
  );
}

function HomeIcon({ color }: { color: string }) {
  return (
    <Svg width={22} height={22} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Path d="M4 11l8-7 8 7v9a1 1 0 0 1-1 1h-4v-6H9v6H5a1 1 0 0 1-1-1z" />
    </Svg>
  );
}
function MapIcon({ color }: { color: string }) {
  return (
    <Svg width={22} height={22} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Rect x="3" y="6" width="7" height="14" rx="1" />
      <Rect x="14" y="4" width="7" height="16" rx="1" />
    </Svg>
  );
}
function BookmarkIcon({ color }: { color: string }) {
  return (
    <Svg width={22} height={22} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Path d="M6 4h12a1 1 0 0 1 1 1v15l-7-4-7 4V5a1 1 0 0 1 1-1z" />
    </Svg>
  );
}
function MyIcon({ color }: { color: string }) {
  return (
    <Svg width={22} height={22} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Circle cx="12" cy="8" r="4" />
      <Path d="M4 20c0-4 3.6-6 8-6s8 2 8 6" />
    </Svg>
  );
}
