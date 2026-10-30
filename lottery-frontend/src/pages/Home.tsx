import { useAuth } from '../auth/AuthContext';

export default function Home() {
    const { user } = useAuth();
    return (
        <section className="card">
            <h1>Welcome, {user?.username}</h1>
            <p>Your roles: {user?.roles.join(', ') || 'none'}</p>
            <p>The screens for your roles will appear here as we build them.</p>
        </section>
    );
}