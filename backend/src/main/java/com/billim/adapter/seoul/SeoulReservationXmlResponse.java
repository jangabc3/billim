package com.billim.adapter.seoul;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.List;

/**
 * 서울시 공공서비스예약(종합) API 응답(tvYeyakCOllect) 기준으로 작성.
 * 루트 엘리먼트 이름이 엔드포인트마다 다르므로(GNListPublicReservationSport, tvYeyakCOllect 등)
 * 
 * @JacksonXmlRootElement의 localName은 무시하고 XmlMapper 설정에서
 *                         root 이름 검증을 끄고 파싱한다 (아래 SeoulReservationAdapter 참고).
 *
 *                         [확정 2026-08-26] 종합 엔드포인트(tvYeyakCOllect)는 서울 전역·전
 *                         카테고리 데이터를
 *                         한 번에 준다. 구/카테고리별 엔드포인트를 따로 돌 필요 없음.
 */
public class SeoulReservationXmlResponse {

    @JacksonXmlProperty(localName = "list_total_count")
    private int listTotalCount;

    @JacksonXmlProperty(localName = "RESULT")
    private Result result;

    @JacksonXmlProperty(localName = "row")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<Row> rows;

    public int getListTotalCount() {
        return listTotalCount;
    }

    public Result getResult() {
        return result;
    }

    public List<Row> getRows() {
        return rows;
    }

    public static class Result {
        @JacksonXmlProperty(localName = "CODE")
        private String code;
        @JacksonXmlProperty(localName = "MESSAGE")
        private String message;

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class Row {
        @JacksonXmlProperty(localName = "DIV")
        private String div; // 대분류 (문화행사 등) — MAXCLASSNM과 겹칠 수 있어 MVP 미사용

        @JacksonXmlProperty(localName = "SERVICE")
        private String service; // MVP 미사용, 원본 그대로 보관

        @JacksonXmlProperty(localName = "GUBUN")
        private String gubun; // 자체 / 위탁 등 — MVP에서는 미사용

        @JacksonXmlProperty(localName = "SVCID")
        private String svcId; // 외부 고유 ID → externalId

        @JacksonXmlProperty(localName = "MAXCLASSNM")
        private String maxClassNm; // 체육시설 / 시설대관 / 교육 / 문화행사 / 진료

        @JacksonXmlProperty(localName = "MINCLASSNM")
        private String minClassNm; // 테니스장, 족구장 등 세부 유형

        @JacksonXmlProperty(localName = "SVCSTATNM")
        private String svcStatNm; // 접수중 / 접수마감 / 이용가능 등

        @JacksonXmlProperty(localName = "SVCNM")
        private String svcNm; // 자원명 → name

        @JacksonXmlProperty(localName = "PAYATNM")
        private String payAtNm; // 무료 / 유료

        @JacksonXmlProperty(localName = "PLACENM")
        private String placeNm; // 장소명 — 도로명주소 없음. address 대체 용도

        @JacksonXmlProperty(localName = "USETGTINFO")
        private String useTgtInfo; // 이용대상 정보 (예: "가족(학부모 1인, 자녀 1인)") — MVP 미사용

        @JacksonXmlProperty(localName = "SVCURL")
        private String svcUrl; // 공식 예약 페이지 → reservationUrl

        @JacksonXmlProperty(localName = "X")
        private String x; // 경도 (longitude)

        @JacksonXmlProperty(localName = "Y")
        private String y; // 위도 (latitude)

        @JacksonXmlProperty(localName = "SVCOPNBGNDT")
        private String svcOpenBeginDt; // 서비스(이용) 시작일시 — 접수기간과 다름, MVP 미사용

        @JacksonXmlProperty(localName = "SVCOPNENDDT")
        private String svcOpenEndDt; // 서비스(이용) 종료일시 — MVP 미사용

        @JacksonXmlProperty(localName = "RCPTBGNDT")
        private String receptionBeginDt; // 접수 시작일시

        @JacksonXmlProperty(localName = "RCPTENDDT")
        private String receptionEndDt; // 접수 종료일시 — 마감임박 판단 기준

        @JacksonXmlProperty(localName = "AREANM")
        private String areaNm; // 구 이름 → gu

        @JacksonXmlProperty(localName = "IMGURL")
        private String imgUrl; // 실제 이미지 URL

        // DTLCONT(상세설명)는 매우 긴 HTML/텍스트 블록이라 매핑하지 않는다.
        // Jackson이 알 수 없는 필드는 기본적으로 무시하지 않으므로, Adapter의 XmlMapper 설정에서
        // FAIL_ON_UNKNOWN_PROPERTIES를 꺼서 파싱이 깨지지 않게 한다.

        @JacksonXmlProperty(localName = "TELNO")
        private String telNo; // 문의 전화번호

        @JacksonXmlProperty(localName = "V_MIN")
        private String operatingStart; // 운영 시작 시각 (예: 07:00)

        @JacksonXmlProperty(localName = "V_MAX")
        private String operatingEnd; // 운영 종료 시각 (예: 19:00)

        @JacksonXmlProperty(localName = "REVSTDDAY")
        private String reserveStdDay; // 예약 기준일 관련 코드 — MVP 미사용

        @JacksonXmlProperty(localName = "REVSTDDAYNM")
        private String reserveStdDayNm; // 예약 기준일 명칭 (예: "이용일") — MVP 미사용

        // getters
        public String getDiv() {
            return div;
        }

        public String getService() {
            return service;
        }

        public String getGubun() {
            return gubun;
        }

        public String getSvcId() {
            return svcId;
        }

        public String getMaxClassNm() {
            return maxClassNm;
        }

        public String getMinClassNm() {
            return minClassNm;
        }

        public String getSvcStatNm() {
            return svcStatNm;
        }

        public String getSvcNm() {
            return svcNm;
        }

        public String getPayAtNm() {
            return payAtNm;
        }

        public String getPlaceNm() {
            return placeNm;
        }

        public String getUseTgtInfo() {
            return useTgtInfo;
        }

        public String getSvcUrl() {
            return svcUrl;
        }

        public String getX() {
            return x;
        }

        public String getY() {
            return y;
        }

        public String getSvcOpenBeginDt() {
            return svcOpenBeginDt;
        }

        public String getSvcOpenEndDt() {
            return svcOpenEndDt;
        }

        public String getReceptionBeginDt() {
            return receptionBeginDt;
        }

        public String getReceptionEndDt() {
            return receptionEndDt;
        }

        public String getAreaNm() {
            return areaNm;
        }

        public String getImgUrl() {
            return imgUrl;
        }

        public String getTelNo() {
            return telNo;
        }

        public String getOperatingStart() {
            return operatingStart;
        }

        public String getOperatingEnd() {
            return operatingEnd;
        }

        public String getReserveStdDay() {
            return reserveStdDay;
        }

        public String getReserveStdDayNm() {
            return reserveStdDayNm;
        }
    }
}