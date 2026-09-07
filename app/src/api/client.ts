import * as SecureStore from "expo-secure-store";
import type { PublicResource } from "../types/resource";

/**
 * 중요: 웹에서는 'localhost:8080'이 그대로 됐지만, 휴대폰 실기기/에뮬레이터에서는
 * 'localhost'가 "폰 자기 자신"을 가리켜서 백엔드 서버(내 컴퓨터)를 못 찾음.
 *
 * - Android 에뮬레이터: 10.0.2.2 사용 (에뮬레이터가 호스트 PC를 가리키는 특수 주소)
 * - iOS 시뮬레이터: localhost 그대로 써도 됨 (같은 머신이라)
 * - 실제 휴대폰(Expo Go): 컴퓨터의 사설 IP 주소 필요 (예: 192.168.0.12)
 *   터미널에서 ipconfig(윈도우) / ifconfig(맥) 로 확인 후 아래 값을 직접 바꿔서 사용.
 */
const DEV_MACHINE_IP = '192.168.45.126';
const BASE_URL = `http://${DEV_MACHINE_IP}:8080/api/v1`;

const TOKEN_KEY = "billim_access_token";

export class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

// ===================== 토큰 저장/조회 =====================
// 로그인 성공 시 저장해두고, 이후 인증이 필요한 요청마다 자동으로 꺼내 실어 보낸다.

export async function saveToken(token: string): Promise<void> {
  await SecureStore.setItemAsync(TOKEN_KEY, token);
}

export async function getToken(): Promise<string | null> {
  return SecureStore.getItemAsync(TOKEN_KEY);
}

export async function clearToken(): Promise<void> {
  await SecureStore.deleteItemAsync(TOKEN_KEY);
}

// ===================== 공통 요청 함수 =====================

interface RequestOptions {
  method?: "GET" | "POST" | "DELETE" | "PUT";
  body?: unknown;
  auth?: boolean; // true면 저장된 토큰을 Authorization 헤더에 실어 보낸다
}

async function request<T>(
  path: string,
  options: RequestOptions = {},
): Promise<T> {
  const { method = "GET", body, auth = false } = options;

  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  };
  if (auth) {
    const token = await getToken();
    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }
  }

  const res = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (!res.ok) {
    // 백엔드 GlobalExceptionHandler가 { message: "..." } 형태로 내려주므로 최대한 살려서 보여준다.
    let message = `요청 실패 (${res.status})`;
    try {
      const errBody = await res.json();
      if (errBody?.message) message = errBody.message;
    } catch {
      // 응답이 JSON이 아니면(204 등) 무시
    }
    throw new ApiError(res.status, message);
  }

  if (res.status === 204) {
    return undefined as T;
  }
  return res.json();
}

// ===================== 자원 검색 =====================

export interface SearchParams {
  category?: string;
  gu?: string;
  receptionStatus?: string;
  keyword?: string;
  page?: number;
  size?: number;
  [key: string]: unknown;
}

export interface NearbyParams {
  lat: number;
  lng: number;
  radiusMeters?: number;
  page?: number;
  size?: number;
  [key: string]: unknown;
}

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

function toQueryString(params: Record<string, unknown>): string {
  const filtered = Object.entries(params).filter(
    ([, v]) => v !== undefined && v !== null && v !== "",
  );
  if (filtered.length === 0) return "";
  const query = filtered
    .map(([k, v]) => `${k}=${encodeURIComponent(String(v))}`)
    .join("&");
  return `?${query}`;
}

export const resourceApi = {
  list: () => request<PublicResource[]>("/resources"),

  search: (params: SearchParams) =>
    request<PageResponse<PublicResource>>(
      `/resources/search${toQueryString(params)}`,
    ),

  nearby: (params: NearbyParams) =>
    request<PageResponse<PublicResource>>(
      `/resources/nearby${toQueryString(params)}`,
    ),

  getOne: (id: number) => request<PublicResource>(`/resources/${id}`),
};

// ===================== 인증 =====================

export interface SignupRequest {
  email: string;
  password: string;
  name: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  userId: number;
  name: string;
}

export const authApi = {
  signup: (data: SignupRequest) =>
    request<void>("/auth/signup", { method: "POST", body: data }),

  login: async (data: LoginRequest): Promise<LoginResponse> => {
    const result = await request<LoginResponse>("/auth/login", {
      method: "POST",
      body: data,
    });
    await saveToken(result.accessToken);
    return result;
  },

  logout: async (): Promise<void> => {
    await clearToken();
  },

  isLoggedIn: async (): Promise<boolean> => {
    const token = await getToken();
    return token !== null;
  },
};

// ===================== 예약 =====================

export interface ReservationResponse {
  id: number;
  rentalItemId: number;
  rentalItemName: string;
  status: "CONFIRMED" | "RENTED" | "RETURNED" | "CANCELED" | "EXPIRED";
  confirmedAt: string;
  expiresAt: string;
}

export const reservationApi = {
  create: (rentalItemId: number) =>
    request<ReservationResponse>("/reservations", {
      method: "POST",
      body: { rentalItemId },
      auth: true,
    }),

  cancel: (reservationId: number) =>
    request<void>(`/reservations/${reservationId}`, {
      method: "DELETE",
      auth: true,
    }),

  getOne: (reservationId: number) =>
    request<ReservationResponse>(`/reservations/${reservationId}`, {
      auth: true,
    }),
};

// ===================== 대기열 =====================

export interface WaitlistResponse {
  id: number;
  rentalItemId: number;
  rentalItemName: string;
  status: "WAITING" | "NOTIFIED" | "CONVERTED" | "EXPIRED";
  requestedAt: string;
  notifiedAt: string | null;
  notifyExpiresAt: string | null;
}

export const waitlistApi = {
  join: (rentalItemId: number) =>
    request<WaitlistResponse>("/waitlist", {
      method: "POST",
      body: { rentalItemId },
      auth: true,
    }),

  cancel: (waitlistId: number) =>
    request<void>(`/waitlist/${waitlistId}`, { method: "DELETE", auth: true }),

  confirm: (waitlistId: number) =>
    request<void>(`/waitlist/${waitlistId}/confirm`, {
      method: "POST",
      auth: true,
    }),
};
