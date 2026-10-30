import type { PaymentOrder } from './types';

export interface RazorpaySuccess {
    razorpay_payment_id: string;
    razorpay_order_id: string;
    razorpay_signature: string;
}

interface RazorpayOptions {
    key: string;
    order_id: string;
    amount: number;
    currency: string;
    name: string;
    description?: string;
    handler: (r: RazorpaySuccess) => void;
    modal?: { ondismiss?: () => void };
}

interface RazorpayInstance {
    open: () => void;
    on: (event: string, cb: (r: { error: { description: string } }) => void) => void;
}

declare global {
    interface Window {
        Razorpay?: new (options: RazorpayOptions) => RazorpayInstance;
    }
}

let loading: Promise<void> | null = null;

function loadScript(): Promise<void> {
    if (window.Razorpay) return Promise.resolve();
    if (!loading) {
        loading = new Promise<void>((resolve, reject) => {
            const s = document.createElement('script');
            s.src = 'https://checkout.razorpay.com/v1/checkout.js';
            s.onload = () => resolve();
            s.onerror = () => {
                loading = null;
                reject(new Error('Could not load the payment window'));
            };
            document.body.appendChild(s);
        });
    }
    return loading;
}

// Opens the Razorpay window. Resolves with the three values the server must verify.
export function payWithRazorpay(order: PaymentOrder, description: string): Promise<RazorpaySuccess> {
    return loadScript().then(
        () =>
            new Promise<RazorpaySuccess>((resolve, reject) => {
                const Rzp = window.Razorpay;
                if (!Rzp) {
                    reject(new Error('The payment window could not be opened'));
                    return;
                }
                const rzp = new Rzp({
                    key: order.keyId,
                    order_id: order.orderId,
                    amount: order.amountInPaise,
                    currency: order.currency,
                    name: 'Lottery',
                    description,
                    handler: resolve,
                    modal: { ondismiss: () => reject(new Error('Payment was not completed')) },
                });
                rzp.on('payment.failed', (r) => reject(new Error(r.error.description)));
                rzp.open();
            }),
    );
}