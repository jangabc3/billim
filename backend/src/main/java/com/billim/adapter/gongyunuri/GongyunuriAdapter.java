package com.billim.adapter.gongyunuri;

import com.billim.domain.item.Category;
import com.billim.domain.resource.PublicResource;
import com.billim.domain.resource.ReceptionStatus;
import com.billim.domain.resource.ResourceSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.ArrayList;
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
 * [중요] 이 목록 API는 카테고리·이용료·재고 여부를 안 준다. 상세 API 연동은 다음 단계.
 * [중요] 전국 데이터이고 지역 필터 파라미터가 없어서, 페이지네이션으로 전체를 다 받은 뒤
 * addr에 "서울"이 포함된 것만 우리 쪽에서 걸러낸다.
 */
@Component
public class GongyunuriAdapter {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient = RestClient.create();

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
        int numOfRows = 1000;

        while (true) {
            GongyunuriListResponse page = fetchPage(rsrcClsCd, apiKey, pageNo, numOfRows);

            if (page.getData() != null) {
                page.getData().stream()
                        .filter(row -> row.getAddr() != null && row.getAddr().contains("서울"))
                        .map(row -> toPublicResource(row, rsrcClsCd))
                        .forEach(result::add);
            }

            // 이번 페이지 결과가 numOfRows보다 적게 왔으면 마지막 페이지라는 뜻
            if (page.getData() == null || page.getData().size() < numOfRows) {
                break;
            }
            pageNo++;
        }

        return result;
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

        String json = restClient.method(HttpMethod.GET)
                .uri(url)
                .header("Content-Type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(String.class);

        try {
            return objectMapper.readValue(json, GongyunuriListResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("공유누리 응답 파싱 실패: " + e.getMessage(), e);
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

    private String extractGu(String addr) {
        if (addr == null)
            return null;
        Matcher m = GU_PATTERN.matcher(addr);
        return m.find() ? m.group(1) : null;
    }

    private BigDecimal toDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }
}