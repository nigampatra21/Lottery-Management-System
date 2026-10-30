export interface TokenInfo {
    userId: number;
    username: string;
    roles: string[];
    exp: number; // seconds since 1970
}

// Reads the token payload. This is only for showing the right menu items:
// the backend checks the signature and the role on every request.
export function decodeToken(token: string): TokenInfo | null {
    try {
        const part = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
        const json = decodeURIComponent(
            atob(part)
                .split('')
                .map((c) => '%' + c.charCodeAt(0).toString(16).padStart(2, '0'))
                .join(''),
        );
        const p = JSON.parse(json);
        return {
            userId: Number(p.userId),
            username: String(p.sub),
            roles: Array.isArray(p.roles) ? p.roles.map(String) : [],
            exp: Number(p.exp),
        };
    } catch {
        return null;
    }
}