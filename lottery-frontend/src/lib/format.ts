export function money(amount: number, currency = 'INR'): string {
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency }).format(amount);
}

export function dateTime(value: string | null): string {
    return value ? new Date(value).toLocaleString('en-IN') : '-';
}

export function errorText(e: unknown): string {
    return e instanceof Error ? e.message : 'Something went wrong';
}