import { Ionicons } from "@expo/vector-icons";

// 백엔드 Category enum 중 실제 데이터가 있는 값만 노출한다 (건수 순 정렬).
// 정장/교육강좌/문화행사/진료예약은 현재 0건이라 제외 — 데이터 동기화되면 추가.
export interface CategoryDef {
  key: string;
  label: string;
  icon: keyof typeof Ionicons.glyphMap;
}

export const categories: CategoryDef[] = [
  { key: "ALL", label: "전체", icon: "grid-outline" },
  { key: "TOOL", label: "공구", icon: "hammer-outline" },
  { key: "FACILITY", label: "시설대관", icon: "business-outline" },
  { key: "SPORTS", label: "체육", icon: "basketball-outline" },
  { key: "MEDICAL", label: "의료용품", icon: "medkit-outline" },
  { key: "CAMPING", label: "캠핑", icon: "bonfire-outline" },
  { key: "BABY", label: "유아", icon: "happy-outline" },
];
