import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../lib/api';
import { dateTime, errorText, money } from '../lib/format';
import type { Claim, Winner } from '../lib/types';

export default function Winnings() {
    const queryClient = useQueryClient();
    const winners = useQuery({
        queryKey: ['my-winnings'],
        queryFn: () => api<Winner[]>('/api/winners/my'),
    });
    const claims = useQuery({
        queryKey: ['my-claims'],
        queryFn: () => api<Claim[]>('/api/claims/my'),
    });
    const claim = useMutation({
        mutationFn: (winnerId: string) => api<Claim>('/api/claims', { method: 'POST', json: { winnerId } }),
        onSuccess: async () => {
            await queryClient.invalidateQueries({ queryKey: ['my-claims'] });
            await queryClient.invalidateQueries({ queryKey: ['my-winnings'] });
        },
    });

    if (winners.isPending || claims.isPending) return <p>Loading your winnings...</p>;
    if (winners.error || claims.error) {
        return <p className="error" role="alert">{errorText(winners.error ?? claims.error)}</p>;
    }

    const byWinner = new Map(claims.data.map((c) => [c.winnerId, c]));

    return (
        <section>
            <h1>My winnings</h1>
            {claim.error && <p className="error" role="alert">{errorText(claim.error)}</p>}
            {winners.data.length === 0 ? (
                <p>No winnings yet.</p>
            ) : (
                <div className="scroll">
                    <table>
                        <thead>
                        <tr><th>Prize</th><th>Declared</th><th>Status</th><th></th></tr>
                        </thead>
                        <tbody>
                        {winners.data.map((w) => {
                            const c = byWinner.get(w.winnerId);
                            return (
                                <tr key={w.winnerId}>
                                    <td>{money(w.prizeAmount)}</td>
                                    <td>{dateTime(w.declaredAt)}</td>
                                    <td>{c ? `Claim ${c.status.toLowerCase()}` : 'Not claimed'}</td>
                                    <td>
                                        {!c && w.status === 'DECLARED' && (
                                            <button disabled={claim.isPending} onClick={() => claim.mutate(w.winnerId)}>
                                                Claim prize
                                            </button>
                                        )}
                                    </td>
                                </tr>
                            );
                        })}
                        </tbody>
                    </table>
                </div>
            )}
        </section>
    );
}