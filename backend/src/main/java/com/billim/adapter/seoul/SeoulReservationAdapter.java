package com.billim.adapter.seoul;

import com.billim.adapter.ResilientApiClient;
import com.billim.common.Times;
import com.billim.domain.item.Category;
import com.billim.domain.resource.PublicResource;
import com.billim.domain.resource.ReceptionStatus;
import com.billim.domain.resource.ResourceSource;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
 *
 * [2026-10-05] 기존에는 Spring의 RestClient로 직접 호출했는데, Gongyunuri 쪽에서 RestClient의
 * read timeout이 실제로는 적용되지 않는 문제를 겪은 적이 있어 동일한 리스크를 안고 있었다.
 * 외부 API 장애 대응(Retry/CircuitBreaker)을 적용하면서, 순수 HttpClient 기반으로 타임아웃을
 * 직접 제어하는 ResilientApiClient를 통해 호출하도록 함께 정리했다.
 *
 * [2026-10-09] 외부 데이터 한 건이 이상해도 배치 전체가 죽지 않도록 방어: 좌표 파싱 실패·범위 이탈은 그 건만
 * 건너뛰고, 이름/주소는 컬럼 길이를 넘으면 잘라서 저장한다.
 */
@Component
public class SeoulReservationAdapter {

    private static final Logger log = LoggerFactory.getLogger(SeoulReservationAdapter.class);

    private static final String SERVICE_NAME = "tvYeyakCOllect";
    private static final int PAGE_SIZE = 1000;
    private static final String BASE_URL = "http://openapi.seoul.go.kr:8088";

    // PublicResource 컬럼 길이와 맞춘다.
    private static final int MAX_NAME = 100;
    private static final int MAX_ADDRESS = 200;
    private static final int MAX_GU = 20;

    // 서울 외 지역(서울농장 등)도 일부 포함되므로 서울이 아니라 대한민국 범위로 거른다. (0,0 같은 쓰레기 값 차단용)
    private static final double MIN_LAT = 33.0;
    private static final double MAX_LAT = 39.0;
    private static final double MIN_LNG = 124.0;
    private static final double MAX_LNG = 132.0;

    private static final DateTimeFormatter SEOUL_DT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S");

    private final XmlMapper xmlMapper;
    private final ResilientApiClient resilientApiClient;

    @Value("${seoul.reservation.api-key}")
    private String apiKey;

    public SeoulReservationAdapter(ResilientApiClient resilientApiClient) {
        this.resilientApiClient = resilientApiClient;
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
        int skipped = 0;

        while (true) {
            int end = start + PAGE_SIZE - 1;
            String xml = fetchPage(start, end);
            SeoulReservationXmlResponse response = readXml(xml);

            if (response.getRows() != null && !response.getRows().isEmpty()) {
                for (SeoulReservationXmlResponse.Row row : response.getRows()) {
                    if (!hasValidCoordinate(row)) {
                        skipped++;
                        continue;
                    }
                    result.add(toPublicResource(row));
                }
            } else {
                break;
            }

            int total = response.getListTotalCount();
            if (end >= total) {
                break;
            }
            start = end + 1;
        }

        if (skipped > 0) {
            log.warn("서울시 예약 데이터 중 좌표가 없거나 비정상이라 건너뛴 건수: {}", skipped);
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

        return resilientApiClient.getFromSeoul(url, "서울시 공공서비스예약 API");
    }

    private SeoulReservationXmlResponse readXml(String xml) {
        try {
            return xmlMapper.readValue(xml, SeoulReservationXmlResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("서울시 공공서비스예약 XML 파싱 실패: " + e.getMessage(), e);
        }
    }

    // 좌표 없는 자원은 DB의 longitude/latitude NOT NULL 제약조건 위반으로 저장이 실패한다.
    // 위치 기반 검색(PostGIS)이 핵심 기능인 서비스 특성상, 좌표가 없거나 숫자가 아니거나 범위를 벗어난 자원은 걸러낸다.
    private boolean hasValidCoordinate(SeoulReservationXmlResponse.Row row) {
        BigDecimal lat = parseCoordinateOrNull(row.getY());
        BigDecimal lng = parseCoordinateOrNull(row.getX());
        if (lat == null || lng == null) {
            return false;
        }
        double latitude = lat.doubleValue();
        double longitude = lng.doubleValue();
        return latitude >= MIN_LAT && latitude <= MAX_LAT
                && longitude >= MIN_LNG && longitude <= MAX_LNG;
    }

    private PublicResource toPublicResource(SeoulReservationXmlResponse.Row row) {
        LocalDateTime receptionEndAt = parseDateTimeSafely(row.getReceptionEndDt());

        return PublicResource.fromExternal(
                ResourceSource.SEOUL_RESERVATION,
                row.getSvcId(),
                truncate(cleanName(row.getSvcNm()), MAX_NAME),
                mapCategory(row.getMaxClassNm()),
                truncate(row.getPlaceNm() == null ? "" : row.getPlaceNm(), MAX_ADDRESS), // 도로명주소가 없어 장소명으로 대체
                row.getAreaNm() == null ? "확인 필요" : truncate(row.getAreaNm(), MAX_GU), // 구
                null, // 동 정보 없음 — 추후 좌표 역지오코딩으로 보완 예정
                parseCoordinateOrNull(row.getY()), // Y = 위도
                parseCoordinateOrNull(row.getX()), // X = 경도
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

    // CDATA로 감싸인 값에 종종 앞뒤 공백과, 이스케이프되지 않은 HTML 엔티티가 섞여 있어 정리
    private String cleanName(String raw) {
        if (raw == null)
            return "";
        return raw.trim()
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&amp;", "&"); // &amp;는 마지막에 — 먼저 하면 다른 엔티티가 이중 치환될 수 있음
    }

    /** DB 컬럼 길이를 넘는 값이 한 건만 있어도 배치 전체가 실패하므로, 넘치면 잘라서 저장한다. */
    private String truncate(String value, int max) {
        if (value == null || value.length() <= max)
            return value;
        return value.substring(0, max);
    }

    /** 숫자가 아니면 예외 대신 null — 호출부(hasValidCoordinate)가 그 건만 걸러낸다. */
    private BigDecimal parseCoordinateOrNull(String raw) {
        if (raw == null || raw.isBlank())
            return null;
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
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
            if (receptionEndAt != null && receptionEndAt.isBefore(Times.now().plusDays(1))) {
                return ReceptionStatus.CLOSING_SOON; // 24시간 이내 마감이면 "오늘 마감"류 배지로 사용
            }
            return ReceptionStatus.OPEN;
        }
        return ReceptionStatus.UNKNOWN;
    }
}