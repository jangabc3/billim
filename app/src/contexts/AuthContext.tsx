import { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import { authApi, getToken, clearToken } from '../api/client';
import type { LoginRequest, SignupRequest } from '../api/client';

interface AuthContextValue {
    isLoggedIn: boolean;
    userName: string | null;
    loading: boolean;
    login: (data: LoginRequest) => Promise<void>;
    signup: (data: SignupRequest) => Promise<void>;
    logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
    const [isLoggedIn, setIsLoggedIn] = useState(false);
    const [userName, setUserName] = useState<string | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        async function restoreSession() {
            const token = await getToken();
            if (!token) {
                setLoading(false);
                return;
            }
            try {
                const me = await authApi.me();
                setIsLoggedIn(true);
                setUserName(me.name);
            } catch {
                // 토큰이 만료됐거나 유효하지 않음 — 로그아웃 상태로 정리
                await clearToken();
                setIsLoggedIn(false);
                setUserName(null);
            } finally {
                setLoading(false);
            }
        }
        restoreSession();
    }, []);

    const login = async (data: LoginRequest) => {
        const result = await authApi.login(data);
        setIsLoggedIn(true);
        setUserName(result.name);
    };

    const signup = async (data: SignupRequest) => {
        await authApi.signup(data);
    };

    const logout = async () => {
        await authApi.logout();
        setIsLoggedIn(false);
        setUserName(null);
    };

    return (
        <AuthContext.Provider value={{ isLoggedIn, userName, loading, login, signup, logout }}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const ctx = useContext(AuthContext);
    if (!ctx) throw new Error('useAuth는 AuthProvider 안에서만 쓸 수 있어요.');
    return ctx;
}