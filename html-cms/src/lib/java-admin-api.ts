import { cookies, headers } from 'next/headers';

export interface SpiderAdminApiResponse<T> {
    success?: boolean;
    message?: string;
    data?: T;
    code?: number;
}

const JAVA_ADMIN_API_BASE_URL =
    process.env.JAVA_ADMIN_API_BASE_URL ??
    process.env.SPIDER_ADMIN_BASE_URL ??
    process.env.NEXT_PUBLIC_JAVA_API_BASE_URL ??
    '';

async function getJavaApiBaseUrl(): Promise<string> {
    if (JAVA_ADMIN_API_BASE_URL) {
        return JAVA_ADMIN_API_BASE_URL.replace(/\/$/, '');
    }

    const headerStore = await headers();
    const host = headerStore.get('x-forwarded-host') ?? headerStore.get('host');
    if (!host) {
        throw new Error('Unable to resolve spider-admin API base URL.');
    }

    const protocol = headerStore.get('x-forwarded-proto') ?? 'http';
    return `${protocol}://${host}`;
}

async function getCookieHeader(): Promise<string> {
    const cookieStore = await cookies();
    return cookieStore
        .getAll()
        .map((cookie) => `${cookie.name}=${cookie.value}`)
        .join('; ');
}

export async function fetchJavaAdminApi<T>(path: string, init: RequestInit = {}): Promise<T> {
    const baseUrl = await getJavaApiBaseUrl();
    const normalizedPath = path.startsWith('/') ? path : `/${path}`;
    const cookieHeader = await getCookieHeader();
    const requestHeaders = new Headers(init.headers);

    if (cookieHeader && !requestHeaders.has('cookie')) {
        requestHeaders.set('cookie', cookieHeader);
    }
    if (!requestHeaders.has('accept')) {
        requestHeaders.set('accept', 'application/json');
    }

    const response = await fetch(`${baseUrl}${normalizedPath}`, {
        ...init,
        headers: requestHeaders,
        credentials: 'include',
        cache: 'no-store',
    });

    const body = (await response.json().catch(() => null)) as SpiderAdminApiResponse<T> | null;
    if (!response.ok || !body?.success || body.data === undefined) {
        const err = new Error(body?.message ?? `spider-admin API request failed. (${response.status})`);
        // 인증 만료 여부를 호출자가 판별할 수 있도록 HTTP 상태코드를 attach
        (err as Error & { status?: number }).status = response.status;
        throw err;
    }

    return body.data;
}
