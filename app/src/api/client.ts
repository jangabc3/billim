import * as SecureStore from "expo-secure-store";
import { Platform } from "react-native";
import type { PublicResource } from "../types/resource";

// 빌드 시점에 주입되는 환경변수. 개발은 app/.env.local, 배포는 EAS 빌드 프로파일(eas.json)에서 설정한다.
// Expo는 process.env.EXPO_PUBLIC_* 를 "점 표기 그대로" 쓸 때만 치환하므로 구조분해하면 안 된다.
const BASE_URL = process.env.EXPO_PUBLIC_API_URL;

if (!BASE_URL) {
  throw new Error(
    "EXPO_PUBLIC_API_URL이 설정되지 않았어요. app/.env.example을 복사해 app/.env.local을 만들어주세요.",
  );
}

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
// expo-secure-store는 웹에서 동작하지 않아서, 웹은 localStorage로 분기한다.

export async function saveToken(token: string): Promise<void> {
  if (Platform.OS === "web") {
    localStorage.setItem(TOKEN_KEY, token);
  } else {
    await SecureStore.setItemAsync(TOKEN_KEY, token);
  }
}

export async function getToken(): Promise<string | null> {
  if (Platform.OS === "web") {
    return localStorage.getItem(TOKEN_KEY);
  }
  return SecureStore.getItemAsync(TOKEN_KEY);
}

export async function clearToken(): Promise<void> {
  if (Platform.OS === "web") {
    localStorage.removeItem(TOKEN_KEY);
  } else {
    await SecureStore.deleteItemAsync(TOKEN_KEY);
  }
}

// ===================== 공통 요청 함수 =====================

interface RequestOptions {
  method?: "GET" | "POST" | "DELETE" | "PUT";
  body?: unknown;
  auth?: boolean;
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

  const text = await res.text();
  if (!text) {
    return undefined as T;
  }
  return JSON.parse(text);
}

// ===================== 자원 검색 =====================

export interface SearchParams {
  category?: string;
  gu?: string;
  receptionStatus?: string;
  keyword?: string;
  freeOnly?: boolean;
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

export interface MeResponse {
  userId: number;
  email: string;
  name: string;
  role: string;
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

  me: () => request<MeResponse>("/auth/me", { auth: true }),

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

  list: () =>
    request<ReservationResponse[]>("/reservations", {
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

// ===================== 즐겨찾기 =====================

export const favoriteApi = {
  list: () => request<PublicResource[]>("/favorites", { auth: true }),

  add: (resourceId: number) =>
    request<void>(`/favorites/${resourceId}`, { method: "POST", auth: true }),

  remove: (resourceId: number) =>
    request<void>(`/favorites/${resourceId}`, { method: "DELETE", auth: true }),
};
