import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

export default function Login() {
    const { login } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();
    const registered = (location.state as { registered?: boolean } | null)?.registered;
    const [name, setName] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [busy, setBusy] = useState(false);

    async function submit(e: FormEvent) {
        e.preventDefault();
        setError('');
        setBusy(true);
        try {
            await login(name.trim(), password);
            navigate('/', { replace: true });
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Login failed');
        } finally {
            setBusy(false);
        }
    }

    return (
        <form className="card" onSubmit={submit}>
            <h1>Sign in</h1>
            {registered && <p className="ok">Account created. Please sign in.</p>}
            {error && <p className="error" role="alert">{error}</p>}
            <label>
                Username or email
                <input value={name} onChange={(e) => setName(e.target.value)} required autoFocus />
            </label>
            <label>
                Password
                <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
            </label>
            <button disabled={busy}>{busy ? 'Signing in...' : 'Sign in'}</button>
            <p>New here? <Link to="/register">Create an account</Link></p>
        </form>
    );
}