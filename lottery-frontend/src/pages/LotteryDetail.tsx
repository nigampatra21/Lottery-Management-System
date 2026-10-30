import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api } from '../lib/api';
import { dateTime, errorText, money } from '../lib/format';
import { payWithRazorpay } from '../lib/razorpay';
import type { Lottery, PaymentOrder, PurchaseInfo } from '../lib/types';

export default function LotteryDetail() {
    const { id } = useParams();
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const [selected, setSelected] = useState<number[]>([]);
    const [step, setStep] = useState('');
    const [message, setMessage] = useState('');

    const lottery = useQuery({
        queryKey: ['lottery', id],
        queryFn: () => api<Lottery>(`/api/lotteries/${id}`),
    });
    const info = useQuery({
        queryKey: ['purchase-info', id],
        queryFn: () => api<PurchaseInfo>(`/api/lotteries/${id}/purchase-info`),
    });

    const buy = useMutation({
        mutationFn: async () => {
            setMessage('');
            setStep('Creating your order...');
            const order = await api<PaymentOrder>('/api/tickets', {
                method: 'POST',
                json: { lotteryId: Number(id), selectedNumbers: selected },
            });
            if (!order?.orderId || !order?.keyId || !order?.amountInPaise) {
                throw new Error('Unexpected payment details from the server');
            }

            setStep('Waiting for payment...');
            const paid = await payWithRazorpay(order, info.data?.lotteryId ? `Ticket ${order.ticketId}` : 'Ticket');

            setStep('Confirming your payment...');
            try {
                await api('/api/tickets/verify-payment', { method: 'POST', json: paid });
            } catch {
                // The payment went through. The server also issues the ticket on its own shortly.
                setMessage(
                    'Your payment was received. We could not confirm it right now, but your ticket will appear in My tickets within a minute or two.',
                );
                return false;
            }
            return true;
        },
        onSuccess: async (confirmed) => {
            setStep('');
            await queryClient.invalidateQueries({ queryKey: ['my-tickets'] });
            if (confirmed) navigate('/tickets');
        },
        onError: (e) => {
            setStep('');
            setMessage(
                errorText(e) === 'Payment was not completed'
                    ? 'The payment was not completed. These numbers are held for a few minutes, then released. You can try again after that.'
                    : errorText(e),
            );
        },
    });

    if (lottery.isPending || info.isPending) return <p>Loading...</p>;
    if (lottery.error || info.error) {
        return <p className="error" role="alert">{errorText(lottery.error ?? info.error)}</p>;
    }

    const { numberCount, minNumber, maxNumber } = info.data;
    const all = Array.from({ length: maxNumber - minNumber + 1 }, (_, i) => minNumber + i);
    const full = selected.length === numberCount;

    function toggle(n: number) {
        setSelected((cur) =>
            cur.includes(n)
                ? cur.filter((x) => x !== n)
                : cur.length < numberCount
                    ? [...cur, n].sort((a, b) => a - b)
                    : cur,
        );
    }

    function quickPick() {
        const pool = [...all];
        const picked: number[] = [];
        while (picked.length < numberCount && pool.length > 0) {
            const i = crypto.getRandomValues(new Uint32Array(1))[0] % pool.length;
            picked.push(pool.splice(i, 1)[0]);
        }
        setSelected(picked.sort((a, b) => a - b));
    }

    return (
        <section>
            <p><Link to="/lotteries">Back to lotteries</Link></p>
            <h1>{lottery.data.name}</h1>
            <p>
                <strong>{money(info.data.ticketPrice, info.data.currency)}</strong> per ticket. Pick{' '}
                {numberCount} different numbers from {minNumber} to {maxNumber}.
            </p>
            <p className="muted">
                Sales end {dateTime(info.data.salesEndAt)}. Draw {dateTime(info.data.drawAt)}.{' '}
                {info.data.allowTicketCancellation
                    ? 'You can cancel before the draw.'
                    : 'Tickets cannot be cancelled.'}
            </p>

            <div className="numbers" role="group" aria-label="Choose your numbers">
                {all.map((n) => (
                    <button
                        key={n}
                        type="button"
                        className={selected.includes(n) ? 'num on' : 'num'}
                        aria-pressed={selected.includes(n)}
                        onClick={() => toggle(n)}
                    >
                        {n}
                    </button>
                ))}
            </div>

            <p>
                Your numbers: <strong>{selected.length ? selected.join(', ') : 'none yet'}</strong>{' '}
                ({selected.length} of {numberCount})
            </p>
            <p>
                <button type="button" className="plain" onClick={quickPick} disabled={buy.isPending}>
                    Quick pick
                </button>{' '}
                <button type="button" className="plain" onClick={() => setSelected([])} disabled={buy.isPending}>
                    Clear
                </button>
            </p>

            {step && <p aria-live="polite">{step}</p>}
            {message && <p className="error" role="alert">{message}</p>}

            <button disabled={!full || buy.isPending} onClick={() => buy.mutate()}>
                {buy.isPending ? 'Please wait...' : `Pay ${money(info.data.ticketPrice, info.data.currency)}`}
            </button>
        </section>
    );
}