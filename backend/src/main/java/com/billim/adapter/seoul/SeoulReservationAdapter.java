package com.billim.adapter.seoul;

import com.billim.domain.item.Category;
import com.billim.domain.resource.PublicResource;
import com.billim.domain.resource.ReceptionStatus;
import com.billim.domain.resource.ResourceSource;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 서울시 공공서비스예약 API 응답(XML)을 우리 서비스의 PublicResource로 변환한다.
 *
 * [확정] 구 접두어 없는 엔드포인트(예: ListPublicReservationSport)는 서울 전역 데이터를
 * 한 번에 준다. 즉 25개 구를 따로 호출할 필요 없이, 카테고리당 엔드포인트 1개면 된다:
 *   - ListPublicReservationSport   (체육시설)
 *   - ListPublicReservationCulture (문화행사)
 *   - 시설대관 / 교육 / 진료 엔드포인트 이름은 API 검색에서 구 접두어 없는 항목으로 확인 예정
 *
 * 단, 한 번에 최대 1,000건이라 list_total_count(예: 594)를 보고 필요시 페이지네이션해야 한다.
 * (URL의 /1/5/ 부분이 start/end index)
 */
public class SeoulReservationAdapter {

    private static final DateTimeFormatter SEOUL_DT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S");

    private final XmlMapper xmlMapper;

    public SeoulReservationAdapter() {
        this.xmlMapper = new XmlMapper();
        // 루트 엘리먼트 이름이 구별로 다르므로(GNListPublicReservationSport 등) 검증을 끈다.
        this.xmlMapper.getFactory().getXMLInputFactory();
    }

    /** XML 원문 → PublicResource 리스트. area(구)는 응답에도 있지만 안전하게 파라미터로도 받는다. */
    public List<PublicResource> parse(String xml) {
        try {
            SeoulReservationXmlResponse response = xmlMapper.readValue(xml, SeoulReservationXmlResponse.class);
            if (response.getRows() == null) {
                return List.of();
            }
            return response.getRows().stream()
                    .map(this::toPublicResource)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new IllegalStateException("서울시 공공서비스예약 XML 파싱 실패: " + e.getMessage(), e);
        }
    }

    private PublicResource toPublicResource(SeoulReservationXmlResponse.Row row) {
        LocalDateTime receptionEndAt = parseDateTimeSafely(row.getReceptionEndDt());

        return PublicResource.fromExternal(
                ResourceSource.SEOUL_RESERVATION,
                row.getSvcId(),
                cleanName(row.getSvcNm()),
                mapCategory(row.getMaxClassNm()),
                row.getPlaceNm(),           // 도로명주소가 없어 장소명으로 대체
                row.getAreaNm(),            // 구
                null,                       // 동 정보 없음 — 추후 좌표 역지오코딩으로 보완 예정
                parseCoordinate(row.getY()), // Y = 위도
                parseCoordinate(row.getX()), // X = 경도
                row.getPayAtNm(),
                mapReceptionStatus(row.getSvcStatNm(), receptionEndAt),
                receptionEndAt,
                row.getSvcUrl(),
                row.getImgUrl(),            // "종합" 엔드포인트에서만 제공됨
                row.getTelNo(),
                formatOperatingHours(row.getOperatingStart(), row.getOperatingEnd()),
                null                        // 원본 수정시각 필드 없음 — lastSyncedAt만 신뢰 가능
        );
    }

    private String formatOperatingHours(String start, String end) {
        if (start == null || end == null) return null;
        return start + " ~ " + end;
    }

    // CDATA로 감싸인 값에 종종 앞뒤 공백이 섞여 있어 정리
    private String cleanName(String raw) {
        return raw == null ? "" : raw.trim();
    }

    private BigDecimal parseCoordinate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return new BigDecimal(raw.trim());
    }

    private LocalDateTime parseDateTimeSafely(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return LocalDateTime.parse(raw.trim(), SEOUL_DT_FORMAT);
        } catch (Exception e) {
            return null; // 형식이 다른 값이 섞여 있어도 전체 배치가 죽지 않도록 방어
        }
    }

    // MAXCLASSNM 문자열 → 우리 Category enum. 목록에 없는 값은 향후 실데이터 보며 계속 채워나갈 예정.
    private static final Map<String, Category> CATEGORY_MAP = Map.of(
            "체육시설", Category.SPORTS,
            "시설대관", Category.FACILITY,
            "공간시설", Category.FACILITY,   // 실제 시설대관 API의 MAXCLASSNM 값 (2026-08-17 확인)
            "교육", Category.EDUCATION,
            "문화행사", Category.CULTURE,
            "진료", Category.CLINIC
    );

    private Category mapCategory(String maxClassNm) {
        return CATEGORY_MAP.getOrDefault(maxClassNm, Category.FACILITY);
    }

    // SVCSTATNM 텍스트 + 접수 마감 시각을 함께 봐서 상태를 정한다.
    private ReceptionStatus mapReceptionStatus(String svcStatNm, LocalDateTime receptionEndAt) {
        if (svcStatNm == null) return ReceptionStatus.UNKNOWN;

        if (svcStatNm.contains("마감") || svcStatNm.contains("종료")) {
            return ReceptionStatus.CLOSED;
        }
        if (svcStatNm.contains("접수중")) {
            if (receptionEndAt != null && receptionEndAt.isBefore(LocalDateTime.now().plusDays(1))) {
                return ReceptionStatus.CLOSING_SOON; // 24시간 이내 마감이면 "오늘 마감"류 배지로 사용
            }
            return ReceptionStatus.OPEN;
        }
        return ReceptionStatus.UNKNOWN;
    }
}
