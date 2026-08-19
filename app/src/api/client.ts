import type { PublicResource } from '../types/resource';

/**
 * 중요: 웹에서는 'localhost:8080'이 그대로 됐지만, 휴대폰 실기기/에뮬레이터에서는
 * 'localhost'가 "폰 자기 자신"을 가리켜서 백엔드 서버(내 컴퓨터)를 못 찾음.
 *
 * - Android 에뮬레이터: 10.0.2.2 사용 (에뮬레이터가 호스트 PC를 가리키는 특수 주소)
 * - iOS 시뮬레이터: localhost 그대로 써도 됨 (같은 머신이라)
 * - 실제 휴대폰(Expo Go): 컴퓨터의 사설 IP 주소 필요 (예: 192.168.0.12)
 *   터미널에서 ipconfig(윈도우) / ifconfig(맥) 로 확인 후 아래 값을 직접 바꿔서 사용.
 */
const DEV_MACHINE_IP = '192.168.0.12'; // TODO: 본인 컴퓨터의 사설 IP로 교체
const BASE_URL = `http://${DEV_MACHINE_IP}:8080/api/v1`;

export class ApiError extends Error {}

async function request<T>(path: string): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`);
  if (!res.ok) {
    throw new ApiError(`요청 실패 (${res.status}): ${path}`);
  }
  return res.json();
}

export const resourceApi = {
  list: () => request<PublicResource[]>('/resources'),
};
