import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../lib/api';
import { dateTime, errorText, money } from '../lib/format';
import type { Ticket, Transaction } from '../lib/types';

function Payments({ ticketId }: { ticketId: number }) {
    const { data, isPending, error } = useQuery({
        queryKey: ['transactions', ticketId],
        queryFn: () => api<Transaction[]>(`/api/tickets/my/${ticketId}/transactions`),
    });
    if (isPending) return <p>Loading payments...</p>;
    if (error) return <p className="error">{errorText(error)}</p>;
    return (
        <ul>
            {data.map((t) => (
                <li key={t.id}>
                    {t.type} {money(t.amount)} - {t.status}
                    {t.paymentMethod ? ` (${t.paymentMethod})` : ''} - {dateTime(t.createdAt)}
                </li>
            ))}
        </ul>
    );
}

function Row({ ticket }: { ticket: Ticket }) {
    const queryClient = useQueryClient();
    const [open, setOpen] = useState(false);
    const cancel = useMutation({
        mutationFn: () => api<Ticket>(`/api/tickets/my/${ticket.id}/cancel`, { method: 'PATCH' }),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ['my-tickets'] }),
    });

    return (
        <>
            <tr>
                <td>{ticket.ticketId}</td>
                <td>{ticket.numbers.join(' - ')}</td>
                <td>{money(ticket.price)}</td>
                <td><span className={`badge ${ticket.status.toLowerCase()}`}>{ticket.status.replace('_', ' ')}</span></td>
                <td>{dateTime(ticket.purchasedAt)}</td>
                <td>
                    <button className="plain" onClick={() => setOpen(!open)}>
                        {open ? 'Hide payments' : 'Payments'}
                    </button>{' '}
                    {ticket.status === 'ISSUED' && (
                        <button
                            className="plain"
                            disabled={cancel.isPending}
                            onClick={() => {
                                if (window.confirm('Cancel this ticket and request a refund?')) cancel.mutate();
                            }}
                        >
                            Cancel
                        </button>
                    )}
                </td>
            </tr>
            {(open || cancel.error) && (
                <tr>
                    <td colSpan={6}>
                        {cancel.error && <p className="error" role="alert">{errorText(cancel.error)}</p>}
                        {open && <Payments ticketId={ticket.id} />}
                    </td>
                </tr>
            )}
        </>
    );
}

export default function MyTickets() {
    const { data, isPending, error } = useQuery({
        queryKey: ['my-tickets'],
        queryFn: () => api<Ticket[]>('/api/tickets/my'),
    });
    if (isPending) return <p>Loading your tickets...</p>;
    if (error) return <p className="error" role="alert">{errorText(error)}</p>;

    return (
        <section>
            <h1>My tickets</h1>
            {data.length === 0 ? (
                <p>You have no tickets yet.</p>
            ) : (
                <div className="scroll">
                    <table>
                        <thead>
                        <tr><th>Ticket</th><th>Numbers</th><th>Price</th><th>Status</th><th>Bought</th><th></th></tr>
                        </thead>
                        <tbody>{data.map((t) => <Row key={t.id} ticket={t} />)}</tbody>
                    </table>
                </div>
            )}
        </section>
    );
}