import type {
  PublicResource,
  ResourceSource,
  ReceptionStatus,
} from "../types/resource";
import type { ResourceCardItem } from "../components/ResourceCard";

/** ResourceSource → 화면에 보여줄 짧은 뱃지 라벨. */
function sourceLabel(source: ResourceSource): string {
  switch (source) {
    case "SHARENURI":
      return "공유누리 제공";
    case "SEOUL_RESERVATION":
      return "서울시 제공";
    case "BILLIM_PARTNER":
      return "빌림 제휴";
    default:
      return "";
  }
}

/** ReceptionStatus → 뱃지 { label, tone }. null이면 표시 안 함. */
function receptionBadge(
  status: ReceptionStatus,
): { label: string; tone: "urgent" | "brand" | "neutral" | "new" } | null {
  switch (status) {
    case "OPEN":
      return { label: "오늘 가능", tone: "brand" };
    case "CLOSING_SOON":
      return { label: "마감 임박", tone: "urgent" };
    case "CLOSED":
      return { label: "접수 마감", tone: "neutral" };
    case "UNKNOWN":
    default:
      return null;
  }
}

/**
 * 두 좌표 사이의 거리를 미터 단위로 계산한다 (Haversine 공식).
 * 사용자 현재 위치를 아직 못 구했을 때(origin이 없을 때)는 null을 반환 — 화면에서 "-"로 처리한다.
 */
export function calculateDistanceMeters(
  origin: { lat: number; lng: number } | null,
  target: { lat: number; lng: number },
): number | null {
  if (!origin) return null;

  const R = 6371000; // 지구 반지름(미터)
  const toRad = (deg: number) => (deg * Math.PI) / 180;

  const dLat = toRad(target.lat - origin.lat);
  const dLng = toRad(target.lng - origin.lng);
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(origin.lat)) *
      Math.cos(toRad(target.lat)) *
      Math.sin(dLng / 2) ** 2;
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

/** 미터 단위 거리를 "580m" / "1.6km" 같은 표시 문자열로 바꾼다. null이면 "-". */
export function formatDistance(meters: number | null): string {
  if (meters === null) return "-";
  if (meters < 1000) return `${Math.round(meters)}m`;
  return `${(meters / 1000).toFixed(1)}km`;
}

/**
 * 백엔드 PublicResource → 화면의 ResourceCard가 쓰는 ResourceCardItem으로 변환한다.
 * userLocation이 있으면 실제 거리를 계산하고, 없으면 "-"로 표시한다.
 */
export function toResourceCardItem(
  resource: PublicResource,
  userLocation: { lat: number; lng: number } | null = null,
): ResourceCardItem {
  const badges = [];
  const reception = receptionBadge(resource.receptionStatus);
  if (reception) badges.push(reception);
  badges.push({
    label: sourceLabel(resource.source),
    tone: "neutral" as const,
  });

  const distanceMeters = calculateDistanceMeters(userLocation, {
    lat: resource.latitude,
    lng: resource.longitude,
  });

  return {
    id: String(resource.id),
    name: resource.name,
    org: resource.address,
    distance: formatDistance(distanceMeters),
    fee: resource.fee ?? "확인 필요",
    photo: resource.imageUrl ?? "",
    badges,
  };
}
