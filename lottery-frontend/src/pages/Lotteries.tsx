import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { api } from '../lib/api';
import { dateTime, errorText, money } from '../lib/format';
import type { Lottery } from '../lib/types';

export default function Lotteries() {
    const { data, isPending, error } = useQuery({
        queryKey: ['lotteries'],
        queryFn: () => api<Lottery[]>('/api/lotteries'),
    });

    if (isPending) return <p>Loading lotteries...</p>;
    if (error) return <p className="error" role="alert">{errorText(error)}</p>;

    // Only lotteries that are open for sale. The server still checks the dates on purchase.
    const open = data.filter((l) => l.status === 'ACTIVE');

    return (
        <section>
            <h1>Open lotteries</h1>
            {open.length === 0 && <p>No lottery is open for sale right now.</p>}
            <div className="grid">
                {open.map((l) => (
                    <article className="card" key={l.id}>
                        <h2>{l.name}</h2>
                        {l.description && <p>{l.description}</p>}
                        <p><strong>{money(l.ticketPrice, l.currency)}</strong> per ticket</p>
                        <p className="muted">Sales end {dateTime(l.salesEndAt)}<br />Draw {dateTime(l.drawAt)}</p>
                        <Link className="button" to={`/lotteries/${l.id}`}>Buy a ticket</Link>
                    </article>
                ))}
            </div>
        </section>
    );
}