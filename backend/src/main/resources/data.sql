-- 서버 처음 켰을 때 GET /api/v1/resources 로 뭔가 바로 보이도록 하는 시드 데이터.
-- 실제 API 연동 전까지는 이 데이터로 프론트와 연결 테스트를 한다.

INSERT INTO public_resources
    (source, external_id, name, category, address, gu, latitude, longitude,
     fee, reception_status, reservation_type, reservation_url, image_url, created_at, last_synced_at)
VALUES
    ('SEOUL_RESERVATION', 'SEED-001', '응봉공원 다목적구장', 'SPORTS', '응봉공원', '성동구',
     37.5569473910838, 127.02182026085195,
     '무료', 'OPEN', 'EXTERNAL_LINK',
     'https://yeyak.seoul.go.kr/web/reservation/selectReservView.do?rsv_svc_id=S251121103722953401',
     'https://yeyak.seoul.go.kr/web/common/file/FileDown.do?file_id=1763689112262SNJEZF2E396XOQ0WIW7JR75SL',
     now(), now()),

    ('SEOUL_RESERVATION', 'SEED-002', '서울역사박물관 야주개홀(강당)', 'FACILITY', '서울역사박물관', '종로구',
     37.570500279648634, 126.97037430869801,
     '유료(요금안내문의)', 'CLOSED', 'EXTERNAL_LINK',
     'https://yeyak.seoul.go.kr/web/reservation/selectReservView.do?rsv_svc_id=S260427095548625157',
     'https://yeyak.seoul.go.kr/web/common/file/FileDown.do?file_id=1777251639809RR32LBNSIBSYC9A1I1PPUBZD5',
     now(), now()),

    ('SHARENURI', 'SEED-003', '충전 전동드릴', 'TOOL', '서울 성동구 성수동', '성동구',
    37.547, 127.047,
    '무료', 'UNKNOWN', 'EXTERNAL_LINK',
    'https://www.eshare.go.kr', null, now(), now())
    ON CONFLICT (source, external_id) DO NOTHING;
