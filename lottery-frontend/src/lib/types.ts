export interface Lottery {
    id: number;
    lotteryCode: string;
    name: string;
    description: string | null;
    ticketPrice: number;
    currency: string;
    status: string;
    salesStartAt: string;
    salesEndAt: string;
    drawAt: string;
    maxTickets: number | null;
}

export interface PurchaseInfo {
    lotteryId: number;
    status: string;
    ticketPrice: number;
    currency: string;
    salesStartAt: string;
    salesEndAt: string;
    drawAt: string;
    numberCount: number;
    minNumber: number;
    maxNumber: number;
    allowTicketCancellation: boolean;
    cancellationCutoffMinutes: number | null;
}

export interface Ticket {
    id: number;
    ticketId: string;
    lotteryId: number;
    numbers: number[];
    price: number;
    status: string;
    purchasedAt: string;
}

export interface Transaction {
    id: number;
    reference: string;
    type: string;
    amount: number;
    status: string;
    paymentMethod: string | null;
    createdAt: string;
}

// Field names as returned by POST /api/tickets (please confirm, see the end of this message)
export interface PaymentOrder {
    ticketId: string;
    orderId: string;
    amountInPaise: number;
    currency: string;
    keyId: string;
    status: string;
}

export interface Winner {
    winnerId: string;
    ticketId: number;
    lotteryId: number;
    prizeAmount: number;
    status: string;
    declaredAt: string;
}

export interface Claim {
    claimReference: string;
    status: string;
    winnerId: string;
    ticketId: number;
    prizeAmount: number;
    claimedAt: string | null;
    verifiedAt: string | null;
}