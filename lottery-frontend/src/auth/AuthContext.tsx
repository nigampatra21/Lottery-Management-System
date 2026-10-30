import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { api, setToken, setUnauthorizedHandler } from '../lib/api';
import { decodeToken } from '../lib/jwt';
import type { TokenInfo } from '../lib/jwt';

const KEY = 'lottery.token';

interface AuthValue {
    user: TokenInfo | null;
    login: (usernameOrEmail: string, password: string) => Promise<void>;
    logout: () => void;
    hasRole: (...roles: string[]) => boolean;
}

const AuthContext = createContext<AuthValue | null>(null);

function loadStored(): TokenInfo | null {
    const t = sessionStorage.getItem(KEY);
    const info = t ? decodeToken(t) : null;
    if (!t || !info || info.exp * 1000 < Date.now()) {
        sessionStorage.removeItem(KEY);
        return null;
    }
    setToken(t);
    return info;
}

export function AuthProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<TokenInfo | null>(loadStored);

    const logout = useCallback(() => {
        sessionStorage.removeItem(KEY);
        setToken(null);
        setUser(null);
    }, []);

    useEffect(() => {
        setUnauthorizedHandler(logout);
    }, [logout]);

    const login = useCallback(async (usernameOrEmail: string, password: string) => {
        const data = await api<{ token?: string; accessToken?: string }>('/api/auth/login', {
            method: 'POST',
            json: { usernameOrEmail, password },
        });
        const t = data.token ?? data.accessToken;
        const info = t ? decodeToken(t) : null;
        if (!t || !info) throw new Error('The server did not return a usable token');
        // sessionStorage is cleared when the tab closes. Any script on the page could read it,
        // so keep the app free of untrusted content.
        sessionStorage.setItem(KEY, t);
        setToken(t);
        setUser(info);
    }, []);

    const value = useMemo<AuthValue>(
        () => ({
            user,
            login,
            logout,
            hasRole: (...roles) => !!user && roles.some((r) => user.roles.includes(r)),
        }),
        [user, login, logout],
    );

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthValue {
    const ctx = useContext(AuthContext);
    if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
    return ctx;
}