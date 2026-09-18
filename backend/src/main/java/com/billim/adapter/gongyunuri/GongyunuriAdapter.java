package com.billim.adapter.gongyunuri;

import com.billim.domain.item.Category;
import com.billim.domain.resource.PublicResource;
import com.billim.domain.resource.ReceptionStatus;
import com.billim.domain.resource.ResourceSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 공유누리(gongyunuri) 목록 API 응답(JSON)을 PublicResource로 변환한다.
 * 패키지명은 gongyunuri로 통일했지만, 저장되는 ResourceSource enum 값은 SHARENURI 그대로 둔다
 * (seoul 패키지의 enum 값이 SEOUL_RESERVATION인 것과 같은 방식).
 *
 * [중요] 목록 API는 카테고리·이용료·재고 여부를 안 준다. 이용료(freeYn)와 세부분류(rsrcClsNm)는
 * 별도 상세 API(rsrc/detail)를 rsrcNoList로 배치 조회해서 보완한다 — fetchDetailBatch() 참고.
 * [중요] 전국 데이터이고 지역 필터 파라미터가 없어서, 페이지네이션으로 전체를 다 받은 뒤
 * addr에 "서울"이 포함된 것만 우리 쪽에서 걸러낸다. 종료 조건은 "이번 페이지에서 실제로 받은
 * raw 건수"로 판단해야 한다 — 서울만 걸러진 건수로 판단하면, 전국 데이터가 많은 카테고리에서
 * 종료 조건에 영영 도달하지 못해 사실상 무한 루프에 빠진다(실제로 겪은 버그).
 * [중요] 일부 자원(예: 관공서명만 있고 도로명주소가 없는 경우)은 "구"를 정규식으로 못 뽑아낼 수 있다.
 * gu 컬럼이 DB에서 필수값이라, 못 찾으면 "확인 필요"로 채워서 억지로 값을 지어내지 않는다.
 * [중요] RestClient의 read timeout이 실제로는 적용되지 않는 문제를 겪어, 순수
 * java.net.http.HttpClient로 직접 타임아웃을 제어한다(sendPost 참고).
 */
@Component
public class GongyunuriAdapter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 전국 데이터를 페이지네이션으로 돌며 서울만 필터링하다 보니, 종료 조건 판단을 잘못하면
    // 무한정 페이지를 넘길 수 있다. 안전장치로 최대 페이지 수를 못박는다.
    // 캠핑처럼 전국 데이터가 수만 건인 카테고리도 있어(예: 17,300건) — 다 받으려면 페이지가
    // 수백 번 필요해 비효율적이다. 초기 단계에서는 상한을 낮게 잡아 "일부라도 빠르게" 확보한다.
    private static final int MAX_PAGES = 10;

    // 상세 API 한 번에 보낼 최대 rsrcNo 개수. 실측 결과 서로 다른 rsrcNo 다수를 보내면
    // 20~30개 근처에서 400이 나서 보수적으로 낮춰 잡는다.
    private static final int DETAIL_BATCH_SIZE = 20;

    private static final Map<String, Category> CATEGORY_CODE_MAP = Map.of(
            "010000", Category.FACILITY,
            "010100", Category.FACILITY,
            "010200", Category.FACILITY,
            "010500", Category.SPORTS,
            "030000", Category.EDUCATION,
            "040000", Category.EDUCATION);

    // ===================== 공개 메서드 =====================

    /**
     * 특정 카테고리(rsrcClsCd)의 전국 데이터를 페이지네이션으로 전부 받아온 뒤,
     * 주소에 "서울"이 포함된 것만 걸러서 PublicResource 리스트로 반환한다.
     * 하루 1회 배치에서 호출할 메서드 — 사용자 요청 경로에서 직접 호출하지 않는다.
     */
    public List<PublicResource> fetchAllSeoulResources(String rsrcClsCd, String apiKey) {
        List<PublicResource> result = new ArrayList<>();
        int pageNo = 1;
        int numOfRows = 100; // 공식 API 스펙 상한(LimitCnt: 1~100)

        while (pageNo <= MAX_PAGES) {
            GongyunuriListResponse page = fetchPage(rsrcClsCd, apiKey, pageNo, numOfRows);

            if (page.getData() != null) {
                page.getData().stream()
                        .filter(row -> row.getAddr() != null && row.getAddr().contains("서울"))
                        .map(row -> toPublicResource(row, rsrcClsCd))
                        .forEach(result::add);
            }

            // 이번 페이지에서 실제로 받은 raw 건수(서울 필터링 전)가 numOfRows보다 적으면 마지막 페이지.
            int rawCount = page.getData() == null ? 0 : page.getData().size();
            if (rawCount < numOfRows) {
                break;
            }
            pageNo++;
        }

        return result;
    }

    /**
     * rsrcNo 목록을 상세 API(rsrc/detail)로 배치 조회해서 {rsrcNo: DetailInfo} 맵으로 반환한다.
     * DETAIL_BATCH_SIZE 단위로 쪼개서 여러 번 호출한다 — 목록이 아무리 커도 한 번에 다 안 보낸다.
     * 상세 API 하나가 실패해도 전체 동기화가 죽지 않도록, 배치 단위 예외는 호출부에서 잡는다.
     */
    public Map<String, DetailInfo> fetchDetailBatch(List<String> rsrcNos, String apiKey) {
        Map<String, DetailInfo> result = new HashMap<>();

        for (int i = 0; i < rsrcNos.size(); i += DETAIL_BATCH_SIZE) {
            List<String> chunk = rsrcNos.subList(i, Math.min(i + DETAIL_BATCH_SIZE, rsrcNos.size()));
            GongyunuriDetailResponse response = fetchDetailChunk(chunk, apiKey);

            if (response.getData() != null) {
                for (GongyunuriDetailResponse.Row row : response.getData()) {
                    result.put(row.getRsrcNo(), new DetailInfo(
                            toFeeLabel(row.getFreeYn()),
                            row.getRsrcClsNm()));
                }
            }
        }

        return result;
    }

    /** "Y"/"N" → 화면에 보여줄 "무료"/"유료" 라벨. 값이 없으면 null(= 표시 안 함). */
    private String toFeeLabel(String freeYn) {
        if ("Y".equalsIgnoreCase(freeYn))
            return "무료";
        if ("N".equalsIgnoreCase(freeYn))
            return "유료";
        return null;
    }

    /** 상세 API 조회 결과 중 우리가 실제로 쓰는 두 값만 담는 값 객체. */
    public record DetailInfo(String fee, String subCategory) {
    }

    /** 이미 응답 JSON을 갖고 있을 때(테스트 등) 바로 파싱만 하고 싶을 때 사용. */
    public List<PublicResource> parse(String json, String rsrcClsCd) {
        try {
            GongyunuriListResponse response = objectMapper.readValue(json, GongyunuriListResponse.class);
            if (response.getData() == null)
                return List.of();

            return response.getData().stream()
                    .filter(row -> row.getAddr() != null && row.getAddr().contains("서울"))
                    .map(row -> toPublicResource(row, rsrcClsCd))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new IllegalStateException("공유누리 JSON 파싱 실패: " + e.getMessage(), e);
        }
    }

    // ===================== 내부 메서드 =====================

    /** 한 페이지(최대 numOfRows개)만 실제 API로 요청해서 가져온다. */
    private GongyunuriListResponse fetchPage(String rsrcClsCd, String apiKey, int pageNo, int numOfRows) {
        String url = "https://www.eshare.go.kr/eshare-openapi/rsrc/list/" + rsrcClsCd + "/" + apiKey;
        String requestBody = String.format("{\"pageNo\":%d,\"numOfRows\":%d}", pageNo, numOfRows);

        String json = sendPost(url, requestBody, "공유누리 목록 API");

        try {
            return objectMapper.readValue(json, GongyunuriListResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("공유누리 응답 파싱 실패: " + e.getMessage(), e);
        }
    }

    /** 상세 API에 rsrcNoList(최대 DETAIL_BATCH_SIZE개)를 POST로 보내 조회한다. */
    private GongyunuriDetailResponse fetchDetailChunk(List<String> rsrcNos, String apiKey) {
        String url = "https://www.eshare.go.kr/eshare-openapi/rsrc/detail/" + apiKey;

        Map<String, Object> body = Map.of("rsrcNoList", rsrcNos);
        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new IllegalStateException("공유누리 상세 요청 바디 생성 실패: " + e.getMessage(), e);
        }

        String json = sendPost(url, requestBody, "공유누리 상세 API");

        try {
            return objectMapper.readValue(json, GongyunuriDetailResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("공유누리 상세 응답 파싱 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 순수 java.net.http.HttpClient로 POST 요청을 보낸다. 연결 5초, 응답 대기 15초로 확실히 타임아웃을 건다.
     */
    private String sendPost(String url, String requestBody, String label) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        label + " 실패 (status=" + response.statusCode() + "): " + response.body());
            }
            return response.body();
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(label + " 호출 실패: " + e.getMessage(), e);
        }
    }

    private PublicResource toPublicResource(GongyunuriListResponse.Row row, String rsrcClsCd) {
        String address = row.getAddr() + (row.getDaddr() != null ? " " + row.getDaddr() : "");

        return PublicResource.fromExternal(
                ResourceSource.SHARENURI,
                row.getRsrcNo(),
                row.getRsrcNm(),
                resolveCategory(rsrcClsCd, row.getRsrcNm()),
                address,
                extractGu(row.getAddr()),
                null,
                toDecimal(row.getLat()),
                toDecimal(row.getLot()),
                null,
                ReceptionStatus.UNKNOWN,
                null,
                row.getInstUrlAddr(),
                row.getImgFileUrlAddr(),
                null,
                null,
                null);
    }

    private Category resolveCategory(String rsrcClsCd, String name) {
        if ("020000".equals(rsrcClsCd))
            return classifyByKeyword(name);
        if ("010700".equals(rsrcClsCd))
            return Category.FACILITY;
        return CATEGORY_CODE_MAP.getOrDefault(rsrcClsCd, Category.FACILITY);
    }

    private Category classifyByKeyword(String name) {
        if (name == null)
            return Category.TOOL;
        if (name.contains("드릴") || name.contains("공구") || name.contains("전동"))
            return Category.TOOL;
        if (name.contains("정장") || name.contains("자켓"))
            return Category.SUIT;
        if (name.contains("유모차") || name.contains("카시트") || name.contains("아기"))
            return Category.BABY;
        if (name.contains("혈압") || name.contains("체성분") || name.contains("AED")
                || name.contains("휠체어") || name.contains("신장계"))
            return Category.MEDICAL;
        if (name.contains("텐트") || name.contains("캠핑"))
            return Category.CAMPING;
        return Category.TOOL;
    }

    private static final Pattern GU_PATTERN = Pattern.compile("(\\S+구)\\s");

    // gu는 DB 필수값(nullable=false)이라 못 찾으면 "확인 필요"로 채운다 — null을 넣으면 저장 자체가 실패한다.
    private String extractGu(String addr) {
        if (addr == null)
            return "확인 필요";
        Matcher m = GU_PATTERN.matcher(addr);
        return m.find() ? m.group(1) : "확인 필요";
    }

    private BigDecimal toDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }
}