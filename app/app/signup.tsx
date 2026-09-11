import { useState } from 'react';
import { View, Text, TextInput, Pressable, StyleSheet, ActivityIndicator, KeyboardAvoidingView, Platform } from 'react-native';
import { useRouter } from 'expo-router';
import { useAuth } from '../src/contexts/AuthContext';
import { ApiError } from '../src/api/client';
import { colors, radius, fonts } from '../src/theme/tokens';

export default function SignupScreen() {
    const router = useRouter();
    const { signup } = useAuth();

    const [name, setName] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleSignup = async () => {
        if (!name || !email || !password) {
            setError('모든 항목을 입력해주세요.');
            return;
        }
        if (password.length < 4) {
            setError('비밀번호는 4자 이상이어야 해요.');
            return;
        }
        setLoading(true);
        setError(null);
        try {
            await signup({ name, email, password });
            router.replace('/login');
        } catch (e) {
            setError(e instanceof ApiError ? e.message : '회원가입에 실패했어요.');
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
                    <Text style={styles.title}>빌림을 시작해볼까요?</Text>
                    <Text style={styles.subtitle}>몇 가지만 입력하면 바로 시작할 수 있어요.</Text>
                </View>

                <View style={styles.form}>
                    <TextInput
                        style={styles.input}
                        placeholder="이름"
                        placeholderTextColor={colors.ink3}
                        value={name}
                        onChangeText={setName}
                    />
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
                        placeholder="비밀번호 (4자 이상)"
                        placeholderTextColor={colors.ink3}
                        value={password}
                        onChangeText={setPassword}
                        secureTextEntry
                    />

                    {error && <Text style={styles.errorText}>{error}</Text>}

                    <Pressable
                        style={[styles.submitBtn, loading && { opacity: 0.6 }]}
                        onPress={handleSignup}
                        disabled={loading}
                    >
                        {loading ? <ActivityIndicator color="#fff" /> : <Text style={styles.submitText}>회원가입</Text>}
                    </Pressable>

                    <Pressable onPress={() => router.back()} style={styles.linkRow}>
                        <Text style={styles.linkText}>이미 계정이 있으신가요? <Text style={styles.linkStrong}>로그인</Text></Text>
                    </Pressable>
                </View>
            </View>
        </KeyboardAvoidingView>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1, paddingHorizontal: 24, justifyContent: 'center' },
    header: { marginBottom: 32 },
    title: { fontSize: 20, fontFamily: fonts.bold, color: colors.ink, marginBottom: 8 },
    subtitle: { fontSize: 13, fontFamily: fonts.regular, color: colors.ink3, lineHeight: 18 },
    form: { gap: 12 },
    input: {
        height: 52, borderRadius: radius.md, borderWidth: 1, borderColor: colors.line,
        backgroundColor: colors.surface, paddingHorizontal: 16, fontSize: 14.5, fontFamily: fonts.regular, color: colors.ink,
    },
    errorText: { fontSize: 12.5, fontFamily: fonts.regular, color: '#E0453C', marginTop: -4 },
    submitBtn: {
        height: 52, borderRadius: radius.md, backgroundColor: colors.brand,
        alignItems: 'center', justifyContent: 'center', marginTop: 8,
    },
    submitText: { fontSize: 15, fontFamily: fonts.bold, color: '#fff' },
    linkRow: { alignItems: 'center', marginTop: 8 },
    linkText: { fontSize: 12.5, fontFamily: fonts.regular, color: colors.ink3 },
    linkStrong: { color: colors.brand, fontFamily: fonts.bold },
});