const BASE = import.meta.env.VITE_API_URL as string;

let token: string | null = null;
let onUnauthorized: () => void = () => {};

export function setToken(t: string | null) {
    token = t;
}
export function setUnauthorizedHandler(fn: () => void) {
    onUnauthorized = fn;
}

export class ApiError extends Error {
    status: number;
    fieldErrors?: Record<string, string>;
    constructor(status: number, message: string, fieldErrors?: Record<string, string>) {
        super(message);
        this.status = status;
        this.fieldErrors = fieldErrors;
    }
}

interface Options extends Omit<RequestInit, 'body'> {
    json?: unknown;
}

// One place that adds the token and turns every error into an ApiError
export async function api<T>(path: string, options: Options = {}): Promise<T> {
    const { json, ...rest } = options;
    const headers = new Headers(rest.headers);
    if (token) headers.set('Authorization', `Bearer ${token}`);
    let body: string | undefined;
    if (json !== undefined) {
        headers.set('Content-Type', 'application/json');
        body = JSON.stringify(json);
    }

    let res: Response;
    try {
        res = await fetch(`${BASE}${path}`, { ...rest, headers, body });
    } catch {
        throw new ApiError(0, 'Cannot reach the server. Is the gateway running?');
    }

    // A token that stopped working means the session is over
    if (res.status === 401 && token) onUnauthorized();
    if (res.status === 204) return undefined as T;

    let data: { message?: string; fieldErrors?: Record<string, string> } | null = null;
    const text = await res.text();
    try {
        data = text ? JSON.parse(text) : null;
    } catch {
        data = null;
    }
    if (!res.ok) {
        throw new ApiError(res.status, data?.message ?? res.statusText, data?.fieldErrors);
    }
    return data as T;
}