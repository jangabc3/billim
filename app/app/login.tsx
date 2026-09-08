import { useState } from 'react';
import { View, Text, TextInput, Pressable, StyleSheet, ActivityIndicator, KeyboardAvoidingView, Platform } from 'react-native';
import { useRouter } from 'expo-router';
import { useAuth } from '../src/contexts/AuthContext';
import { ApiError } from '../src/api/client';
import { colors, radius } from '../src/theme/tokens';

export default function LoginScreen() {
    const router = useRouter();
    const { login } = useAuth();

    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleLogin = async () => {
        if (!email || !password) {
            setError('이메일과 비밀번호를 입력해주세요.');
            return;
        }
        setLoading(true);
        setError(null);
        try {
            await login({ email, password });
            router.replace('/(tabs)');
        } catch (e) {
            setError(e instanceof ApiError ? e.message : '로그인에 실패했어요.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <KeyboardAvoidingView
            style={{ flex: 1, backgroundColor: colors.surface }}
            behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        >
            <View style={styles.container}>
                <View style={styles.header}>
                    <View style={styles.logo}>
                        <Text style={styles.logoNum}>8</Text>
                        <Text style={styles.logoText}>빌림</Text>
                    </View>
                    <Text style={styles.title}>다시 만나서 반가워요</Text>
                    <Text style={styles.subtitle}>로그인하고 예약을 이어가세요.</Text>
                </View>

                <View style={styles.form}>
                    <TextInput
                        style={styles.input}
                        placeholder="이메일"
                        placeholderTextColor={colors.ink3}
                        value={email}
                        onChangeText={setEmail}
                        autoCapitalize="none"
                        keyboardType="email-address"
                    />
                    <TextInput
                        style={styles.input}
                        placeholder="비밀번호"
                        placeholderTextColor={colors.ink3}
                        value={password}
                        onChangeText={setPassword}
                        secureTextEntry
                    />

                    {error && <Text style={styles.errorText}>{error}</Text>}

                    <Pressable
                        style={[styles.submitBtn, loading && { opacity: 0.6 }]}
                        onPress={handleLogin}
                        disabled={loading}
                    >
                        {loading ? <ActivityIndicator color="#fff" /> : <Text style={styles.submitText}>로그인</Text>}
                    </Pressable>

                    <Pressable onPress={() => router.push('/signup')} style={styles.linkRow}>
                        <Text style={styles.linkText}>아직 계정이 없으신가요? <Text style={styles.linkStrong}>회원가입</Text></Text>
                    </Pressable>
                </View>
            </View>
        </KeyboardAvoidingView>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1, paddingHorizontal: 24, justifyContent: 'center' },
    header: { alignItems: 'center', marginBottom: 40 },
    logo: { flexDirection: 'row', alignItems: 'center', gap: 6, marginBottom: 20 },
    logoNum: { fontSize: 28, fontWeight: '900', color: colors.brand },
    logoText: { fontSize: 24, fontWeight: '800', color: colors.ink },
    title: { fontSize: 18, fontWeight: '800', color: colors.ink, marginBottom: 6 },
    subtitle: { fontSize: 13, color: colors.ink3 },
    form: { gap: 12 },
    input: {
        height: 52, borderRadius: radius.md, borderWidth: 1, borderColor: colors.line,
        backgroundColor: colors.surface, paddingHorizontal: 16, fontSize: 14.5, color: colors.ink,
    },
    errorText: { fontSize: 12.5, color: '#E0453C', marginTop: -4 },
    submitBtn: {
        height: 52, borderRadius: radius.md, backgroundColor: colors.brand,
        alignItems: 'center', justifyContent: 'center', marginTop: 8,
    },
    submitText: { fontSize: 15, fontWeight: '800', color: '#fff' },
    linkRow: { alignItems: 'center', marginTop: 8 },
    linkText: { fontSize: 12.5, color: colors.ink3 },
    linkStrong: { color: colors.brand, fontWeight: '800' },
});