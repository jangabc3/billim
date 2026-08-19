// 이 타입은 billim-backend의 com.billim.api.PublicResourceResponse 필드와 1:1로 맞춰야 함.
// 백엔드 DTO가 바뀌면 여기도 같이 바꿀 것 — 안 그러면 조용히 undefined가 됨.

export type ResourceSource = 'SHARENURI' | 'SEOUL_RESERVATION' | 'BILLIM_PARTNER';

export type Category =
  | 'TOOL' | 'SUIT' | 'BABY' | 'MEDICAL' | 'CAMPING' | 'SPORTS'
  | 'FACILITY' | 'EDUCATION' | 'CULTURE' | 'CLINIC';

export type ReceptionStatus = 'OPEN' | 'CLOSING_SOON' | 'CLOSED' | 'UNKNOWN';
export type ReservationType = 'EXTERNAL_LINK' | 'DIRECT_BOOKING';

export interface PublicResource {
  id: number;
  source: ResourceSource;
  name: string;
  category: Category;
  address: string;
  gu: string;
  latitude: number;
  longitude: number;
  fee: string | null;
  receptionStatus: ReceptionStatus;
  receptionEndAt: string | null; // ISO datetime string
  reservationType: ReservationType;
  reservationUrl: string | null;
  imageUrl: string | null;
  lastSyncedAt: string | null;
}
