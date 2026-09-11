// billim-web의 tokens.css와 값은 완전히 동일. RN엔 CSS 변수가 없어서 JS 객체로 관리.
export const colors = {
  ink: "#14171C",
  ink2: "#4A5058",
  ink3: "#767C86",
  ink4: "#C4C8CF",
  canvas: "#F3F4F6",
  surface: "#FFFFFF",
  grayFill: "#F1F2F5",
  line: "#E5E7EB",

  brand: "#2E5EEA",
  brandStrong: "#1F45C4",
  brandTint: "#E8EEFE",
  accentLime: "#D6F24E",
  urgent: "#E07A1E",
  urgentTint: "#FCEEDD",
  newTone: "#1F9D6C",
  newToneTint: "#E4F6ED",
};

export const radius = {
  sm: 8,
  md: 14,
  lg: 18,
  xl: 20,
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
