// billim-web의 tokens.css와 값은 완전히 동일. RN엔 CSS 변수가 없어서 JS 객체로 관리.
export const colors = {
  ink: "#141A24",
  ink2: "#354153",
  ink3: "#697383",
  ink4: "#99A1B1",
  canvas: "#F2F3F8",
  surface: "#FFFFFF",
  grayFill: "#F2F3F8",
  line: "#E5E8EF",

  brand: "#0078FF",
  brandStrong: "#0052E0",
  brandTint: "#EBF5FF",
  brandTintStrong: "#D6EBFF",

  heroDark: "#141A24",
  heroAccent: "#99CEFF",

  urgent: "#FA7900",
  urgentTint: "#FFF8E6",
  newTone: "#00BB83",
  newToneTint: "#E6FEF0",
};

export const radius = {
  xs: 6,
  sm: 8,
  md: 12,
  lg: 14,
  xl: 16,
  full: 999,
};

export const spacing = {
  xs: 4,
  sm: 8,
  md: 14,
  lg: 20,
  xl: 28,
};

// Pretendard 4-weight 매핑. RN은 variable font 미지원이라 static weight별 family name으로 분기.
export const fonts = {
  regular: "Pretendard-Regular",
  medium: "Pretendard-Medium",
  semibold: "Pretendard-SemiBold",
  bold: "Pretendard-Bold",
};

// 토스 type ramp 참고 — 한글 가독성 기준 body는 1.5 line-height.
export const typography = {
  h1: { fontFamily: fonts.bold, fontSize: 24, lineHeight: 31 },
  h2: { fontFamily: fonts.bold, fontSize: 20, lineHeight: 27 },
  title1: { fontFamily: fonts.semibold, fontSize: 17, lineHeight: 25 },
  title2: { fontFamily: fonts.semibold, fontSize: 15.5, lineHeight: 22 },
  body1: { fontFamily: fonts.regular, fontSize: 15, lineHeight: 22.5 },
  body2: { fontFamily: fonts.regular, fontSize: 13, lineHeight: 19.5 },
  labelL: { fontFamily: fonts.bold, fontSize: 15, lineHeight: 19 },
  labelM: { fontFamily: fonts.semibold, fontSize: 13, lineHeight: 16 },
  caption: { fontFamily: fonts.medium, fontSize: 11.5, lineHeight: 16 },
};

// SOCAR Frame 참고 — 카드 표면 분리는 그림자보다 헤어라인이 우선, 그림자는 최소로만.
export const shadow = {
  sm: {
    shadowColor: "#141A24",
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.04,
    shadowRadius: 2,
    elevation: 1,
  },
};
