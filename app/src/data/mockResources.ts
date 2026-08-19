import type { ResourceCardItem } from '../components/ResourceCard';

// TODO: resourceApi.list() 결과로 교체. 지금은 화면 구조 확인용 목업.
export const mockItems: Omit<ResourceCardItem, 'bookmarked'>[] = [
  {
    id: '1', name: '충전 전동드릴', org: '성수2가 주민센터', distance: '580m', fee: '무료',
    badges: [{ label: '오늘 개시', tone: 'new' }, { label: '공유누리 제공', tone: 'neutral' }],
    photo: 'assets/items/drill-01.jpg',
  },
  {
    id: '2', name: '면접 정장 세트', org: '성동청년지원센터', distance: '1.6km', fee: '무료',
    badges: [{ label: '예약 접수', tone: 'brand' }, { label: '성동구 제공', tone: 'neutral' }],
    photo: 'assets/items/suit-01.jpg',
  },
  {
    id: '3', name: '수동 휠체어', org: '성수종합복지관', distance: '2.1km', fee: '무료',
    badges: [{ label: '마감 임박', tone: 'urgent' }, { label: '공유누리 제공', tone: 'neutral' }],
    photo: 'assets/items/wheelchair-01.jpg',
  },
];
