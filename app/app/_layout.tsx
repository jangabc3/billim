import { Stack } from 'expo-router';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { BookmarkProvider } from '../src/contexts/BookmarkContext';
import { AuthProvider } from '../src/contexts/AuthContext';
import { colors } from '../src/theme/tokens';

export default function RootLayout() {
  return (
    <SafeAreaProvider>
      <AuthProvider>
        <BookmarkProvider>
          <Stack screenOptions={{ headerShown: false, contentStyle: { backgroundColor: colors.canvas } }}>
            <Stack.Screen name="(tabs)" />
            <Stack.Screen
              name="resources/[id]"
              options={{ presentation: 'card', animation: 'slide_from_right' }}
            />
            <Stack.Screen
              name="login"
              options={{ presentation: 'modal', animation: 'slide_from_bottom' }}
            />
            <Stack.Screen
              name="signup"
              options={{ presentation: 'modal', animation: 'slide_from_bottom' }}
            />
          </Stack>
        </BookmarkProvider>
      </AuthProvider>
    </SafeAreaProvider>
  );
}