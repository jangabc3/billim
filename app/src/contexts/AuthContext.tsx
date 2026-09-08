import { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import { authApi, getToken } from '../api/client';
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
        async function checkStoredToken() {
            const token = await getToken();
            setIsLoggedIn(token !== null);
            setLoading(false);
        }
        checkStoredToken();
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