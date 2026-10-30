import { Link, Navigate, Outlet, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth/AuthContext';
import Home from './pages/Home';
import Login from './pages/Login';
import Lotteries from './pages/Lotteries';
import LotteryDetail from './pages/LotteryDetail';
import MyTickets from './pages/MyTickets';
import Register from './pages/Register';
import Winnings from './pages/Winnings';

// Everything inside needs a signed-in user
function Protected() {
    const { user, logout, hasRole } = useAuth();
    if (!user) return <Navigate to="/login" replace />;
    return (
        <>
            <header className="bar">
                <nav>
                    <Link to="/">Home</Link>
                    {hasRole('CUSTOMER') && (
                        <>
                            <Link to="/lotteries">Lotteries</Link>
                            <Link to="/tickets">My tickets</Link>
                            <Link to="/winnings">My winnings</Link>
                        </>
                    )}
                    {hasRole('AGENT') && <span className="soon">Agent</span>}
                    {hasRole('ADMIN') && <span className="soon">Admin</span>}
                    {hasRole('AUDITOR') && <span className="soon">Audit log</span>}
                </nav>
                <button onClick={logout}>Sign out ({user.username})</button>
            </header>
            <main><Outlet /></main>
        </>
    );
}

// The server refuses other roles anyway. This only avoids showing a broken page.
function RequireRole({ roles }: { roles: string[] }) {
    const { hasRole } = useAuth();
    return hasRole(...roles) ? <Outlet /> : <p className="error">You do not have access to this page.</p>;
}

export default function App() {
    return (
        <Routes>
            <Route path="/login" element={<main><Login /></main>} />
            <Route path="/register" element={<main><Register /></main>} />
            <Route element={<Protected />}>
                <Route path="/" element={<Home />} />
                <Route element={<RequireRole roles={['CUSTOMER']} />}>
                    <Route path="/lotteries" element={<Lotteries />} />
                    <Route path="/lotteries/:id" element={<LotteryDetail />} />
                    <Route path="/tickets" element={<MyTickets />} />
                    <Route path="/winnings" element={<Winnings />} />
                </Route>
            </Route>
            <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
    );
}