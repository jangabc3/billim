# 빌림 앱 (Expo + React Native) — 실행 방법

## 1. 사전 조건

- 휴대폰에 **Expo Go** 앱 설치 (App Store / Play Store에서 "Expo Go" 검색)
- `billim-backend`가 `http://<컴퓨터 IP>:8080`에서 돌고 있어야 데이터가 뜸
- `src/api/client.ts`의 `DEV_MACHINE_IP`를 본인 컴퓨터의 사설 IP로 수정
  (터미널에서 맥은 `ifconfig`, 윈도우는 `ipconfig` → Wi-Fi 어댑터의 IPv4 주소)
- 휴대폰과 컴퓨터가 **같은 Wi-Fi**에 연결돼 있어야 함

## 2. 실행

```bash
npm install
npm start
```

터미널에 QR코드가 뜨면 Expo Go 앱으로 스캔 → 바로 실행됨. 코드 저장할 때마다 앱이 자동으로 새로고침됨(Fast Refresh).

## 3. Vite/Next.js 버전과 근본적으로 다른 점

| | 웹 (billim-web) | 앱 (billim-app) |
|---|---|---|
| 렌더링 | 브라우저 DOM | 네이티브 컴포넌트 (iOS/Android 진짜 UI) |
| 스타일 | CSS / CSS-in-JS | `StyleSheet.create()` — CSS 아님, JS 객체 |
| 태그 | `<div>`, `<span>`, `<img>` | `<View>`, `<Text>`, `<Image>` |
| 클릭 | `onClick` | `onPress` (Pressable/TouchableOpacity) |
| 라우팅 | Next.js App Router (`app/map/page.tsx`) | Expo Router (`app/(tabs)/map.tsx`) — 개념은 거의 동일 |
| 그라디언트 | CSS `linear-gradient` | `expo-linear-gradient` 패키지 필요 |

디자인 토큰 값(색상 hex 코드)은 `billim-web`이랑 완전히 동일해서, 눈에 보이는 결과물은 최대한 똑같이 맞췄어요.

## 4. 앱스토어/플레이스토어에 실제로 올리는 방법 (다음 단계)

지금은 Expo Go로 "미리보기"만 되는 상태예요. 진짜 스토어에 올리려면:

1. **EAS(Expo Application Services) 계정 생성** — `npx expo login`
2. **빌드 설정 파일 생성** — `npx eas build:configure` (eas.json 자동 생성)
3. **빌드 실행** — `npx eas build --platform ios` / `--platform android`
   (애플 개발자 계정 연 99달러/년, 구글 플레이 콘솔 등록비 25달러 1회 필요)
4. **스토어 제출** — `npx eas submit`

이 단계는 실제 배포할 준비가 됐을 때(아이콘, 스플래시 이미지, 개인정보처리방침, 앱 설명 등 다 갖춰진 뒤) 진행하는 게 맞아요. 지금은 기능 완성에 집중.

## 5. 다음에 할 것

- `src/api/client.ts`의 `DEV_MACHINE_IP` 본인 환경에 맞게 수정
- 백엔드 단건 조회 API 연동 (`/resources/[id]/index.tsx`의 TODO)
- 실제 지도 SDK 연동 (`react-native-maps` + 카카오/네이버 지도, 또는 Google Maps)
- 실제 사진 연동 (지금은 `https://example.com/...` 같은 더미 URL이라 안 뜨는 게 정상)
- 로그인(JWT) + 북마크 서버 동기화
