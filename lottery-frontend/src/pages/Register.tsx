import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api, ApiError } from '../lib/api.ts';

export default function Register() {
    const navigate = useNavigate();
    const [form, setForm] = useState({ username: '', email: '', password: '' });
    const [error, setError] = useState('');
    const [fields, setFields] = useState<Record<string, string>>({});
    const [busy, setBusy] = useState(false);

    const set = (k: keyof typeof form) => (e: { target: { value: string } }) =>
        setForm({ ...form, [k]: e.target.value });

    async function submit(e: FormEvent) {
        e.preventDefault();
        setError('');
        setFields({});
        setBusy(true);
        try {
            await api('/api/auth/register', { method: 'POST', json: form });
            navigate('/login', { state: { registered: true } });
        } catch (err) {
            if (err instanceof ApiError) {
                setError(err.message);
                setFields(err.fieldErrors ?? {});
            } else {
                setError('Registration failed');
            }
        } finally {
            setBusy(false);
        }
    }

    return (
        <form className="card" onSubmit={submit}>
            <h1>Create account</h1>
            {error && <p className="error" role="alert">{error}</p>}
            <label>
                Username
                <input value={form.username} onChange={set('username')} required />
                {fields.username && <small className="error">{fields.username}</small>}
            </label>
            <label>
                Email
                <input type="email" value={form.email} onChange={set('email')} required />
                {fields.email && <small className="error">{fields.email}</small>}
            </label>
            <label>
                Password
                <input type="password" value={form.password} onChange={set('password')} required />
                {fields.password && <small className="error">{fields.password}</small>}
            </label>
            <button disabled={busy}>{busy ? 'Creating...' : 'Create account'}</button>
            <p>Already registered? <Link to="/login">Sign in</Link></p>
        </form>
    );
}