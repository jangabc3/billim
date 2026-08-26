package com.billim.adapter.seoul;

import com.billim.domain.item.Category;
import com.billim.domain.resource.PublicResource;
import com.billim.domain.resource.ReceptionStatus;
import com.billim.domain.resource.ResourceSource;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 서울시 공공서비스예약(종합) API 응답(XML)을 우리 서비스의 PublicResource로 변환한다.
 *
 * [확정 2026-08-26] 종합 엔드포인트(서비스명: tvYeyakCOllect)는 서울 전역·전 카테고리
 * (체육시설/시설대관/교육/문화행사/진료) 데이터를 한 번에 준다.
 * 구/카테고리별 엔드포인트(예: GNListPublicReservationSport)를 따로 돌 필요 없음.
 *
 * 요청 URL 패턴:
 * http://openapi.seoul.go.kr:8088/{인증키}/xml/tvYeyakCOllect/{시작}/{종료}/
 * 한 번에 최대 1,000건이라 list_total_count를 보고 필요시 페이지네이션한다.
 */
@Component
public class SeoulReservationAdapter {

    private static final String SERVICE_NAME = "tvYeyakCOllect";
    private static final int PAGE_SIZE = 1000;
    private static final String BASE_URL = "http://openapi.seoul.go.kr:8088";

    private static final DateTimeFormatter SEOUL_DT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S");

    private final XmlMapper xmlMapper;
    private final RestClient restClient = RestClient.create();

    @Value("${seoul.reservation.api-key}")
    private String apiKey;

    public SeoulReservationAdapter() {
        this.xmlMapper = new XmlMapper();
        // 루트 엘리먼트 이름이 엔드포인트마다 다르므로(GNListPublicReservationSport, tvYeyakCOllect 등) 검증을
        // 끈다.
        this.xmlMapper.getFactory().getXMLInputFactory();
        // DTLCONT 등 매핑하지 않은 필드가 있어도 파싱이 깨지지 않도록 설정.
        this.xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    // ===================== 공개 메서드 =====================

    /**
     * 서울시 전역(전 카테고리) 예약 데이터를 페이지네이션으로 전부 받아온다.
     * 하루 1회 배치에서 호출할 메서드 — 사용자 요청 경로에서 직접 호출하지 않는다.
     */
    public List<PublicResource> fetchAll() {
        List<PublicResource> result = new ArrayList<>();
        int start = 1;

        while (true) {
            int end = start + PAGE_SIZE - 1;
            String xml = fetchPage(start, end);
            SeoulReservationXmlResponse response = readXml(xml);

            if (response.getRows() != null && !response.getRows().isEmpty()) {
                response.getRows().stream()
                        .map(this::toPublicResource)
                        .forEach(result::add);
            } else {
                break;
            }

            int total = response.getListTotalCount();
            if (end >= total) {
                break;
            }
            start = end + 1;
        }

        return result;
    }

    /** 이미 응답 XML을 갖고 있을 때(테스트 등) 바로 파싱만 하고 싶을 때 사용. */
    public List<PublicResource> parse(String xml) {
        SeoulReservationXmlResponse response = readXml(xml);
        if (response.getRows() == null) {
            return List.of();
        }
        return response.getRows().stream()
                .map(this::toPublicResource)
                .collect(Collectors.toList());
    }

    // ===================== 내부 메서드 =====================

    /** 한 페이지(start~end, 최대 1,000건)만 실제 API로 요청해서 XML 원문을 받아온다. */
    private String fetchPage(int start, int end) {
        String url = String.format("%s/%s/xml/%s/%d/%d/",
                BASE_URL, apiKey, SERVICE_NAME, start, end);

        return restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);
    }

    private SeoulReservationXmlResponse readXml(String xml) {
        try {
            return xmlMapper.readValue(xml, SeoulReservationXmlResponse.class);
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
                row.getPlaceNm(), // 도로명주소가 없어 장소명으로 대체
                row.getAreaNm(), // 구
                null, // 동 정보 없음 — 추후 좌표 역지오코딩으로 보완 예정
                parseCoordinate(row.getY()), // Y = 위도
                parseCoordinate(row.getX()), // X = 경도
                row.getPayAtNm(),
                mapReceptionStatus(row.getSvcStatNm(), receptionEndAt),
                receptionEndAt,
                row.getSvcUrl(),
                row.getImgUrl(),
                row.getTelNo(),
                formatOperatingHours(row.getOperatingStart(), row.getOperatingEnd()),
                null // 원본 수정시각 필드 없음 — lastSyncedAt만 신뢰 가능
        );
    }

    private String formatOperatingHours(String start, String end) {
        if (start == null || end == null)
            return null;
        return start + " ~ " + end;
    }

    // CDATA로 감싸인 값에 종종 앞뒤 공백이 섞여 있어 정리
    private String cleanName(String raw) {
        return raw == null ? "" : raw.trim();
    }

    private BigDecimal parseCoordinate(String raw) {
        if (raw == null || raw.isBlank())
            return null;
        return new BigDecimal(raw.trim());
    }

    private LocalDateTime parseDateTimeSafely(String raw) {
        if (raw == null || raw.isBlank())
            return null;
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
            "공간시설", Category.FACILITY, // 실제 시설대관 API의 MAXCLASSNM 값 (2026-08-17 확인)
            "교육", Category.EDUCATION,
            "문화행사", Category.CULTURE,
            "진료", Category.CLINIC);

    private Category mapCategory(String maxClassNm) {
        return CATEGORY_MAP.getOrDefault(maxClassNm, Category.FACILITY);
    }

    // SVCSTATNM 텍스트 + 접수 마감 시각을 함께 봐서 상태를 정한다.
    private ReceptionStatus mapReceptionStatus(String svcStatNm, LocalDateTime receptionEndAt) {
        if (svcStatNm == null)
            return ReceptionStatus.UNKNOWN;

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