package com.billim.adapter.seoul;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.List;

/**
 * 실제 확인된 강남구 체육시설 응답 기준으로 작성.
 * 루트 엘리먼트 이름이 "GNListPublicReservationSport"처럼 구별로 다르기 때문에,
 * @JacksonXmlRootElement의 localName은 무시하고 XmlMapper 설정에서
 * root 이름 검증을 끄고 파싱한다 (아래 SeoulReservationAdapter 참고).
 */
public class SeoulReservationXmlResponse {

    @JacksonXmlProperty(localName = "list_total_count")
    private int listTotalCount;

    @JacksonXmlProperty(localName = "RESULT")
    private Result result;

    @JacksonXmlProperty(localName = "row")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<Row> rows;

    public int getListTotalCount() { return listTotalCount; }
    public Result getResult() { return result; }
    public List<Row> getRows() { return rows; }

    public static class Result {
        @JacksonXmlProperty(localName = "CODE")
        private String code;
        @JacksonXmlProperty(localName = "MESSAGE")
        private String message;

        public String getCode() { return code; }
        public String getMessage() { return message; }
    }

    public static class Row {
        @JacksonXmlProperty(localName = "GUBUN")
        private String gubun;              // 자체 / 위탁 등 — MVP에서는 미사용

        @JacksonXmlProperty(localName = "SVCID")
        private String svcId;              // 외부 고유 ID → externalId

        @JacksonXmlProperty(localName = "MAXCLASSNM")
        private String maxClassNm;         // 체육시설 / 시설대관 / 교육 / 문화행사 / 진료

        @JacksonXmlProperty(localName = "MINCLASSNM")
        private String minClassNm;         // 테니스장, 족구장 등 세부 유형

        @JacksonXmlProperty(localName = "SVCSTATNM")
        private String svcStatNm;          // 접수중 / 접수마감 / 이용가능 등

        @JacksonXmlProperty(localName = "SVCNM")
        private String svcNm;              // 자원명 → name

        @JacksonXmlProperty(localName = "PAYATNM")
        private String payAtNm;            // 무료 / 유료

        @JacksonXmlProperty(localName = "PLACENM")
        private String placeNm;            // 장소명 — 도로명주소 없음. address 대체 용도

        @JacksonXmlProperty(localName = "SVCURL")
        private String svcUrl;             // 공식 예약 페이지 → reservationUrl

        @JacksonXmlProperty(localName = "X")
        private String x;                  // 경도 (longitude)

        @JacksonXmlProperty(localName = "Y")
        private String y;                  // 위도 (latitude)

        @JacksonXmlProperty(localName = "RCPTBGNDT")
        private String receptionBeginDt;   // 접수 시작일시

        @JacksonXmlProperty(localName = "RCPTENDDT")
        private String receptionEndDt;     // 접수 종료일시 — 마감임박 판단 기준

        @JacksonXmlProperty(localName = "AREANM")
        private String areaNm;             // 구 이름 → gu

        @JacksonXmlProperty(localName = "IMGURL")
        private String imgUrl;             // 실제 이미지 URL — "종합" 엔드포인트에만 있음

        @JacksonXmlProperty(localName = "TELNO")
        private String telNo;              // 문의 전화번호

        @JacksonXmlProperty(localName = "V_MIN")
        private String operatingStart;     // 운영 시작 시각 (예: 07:00)

        @JacksonXmlProperty(localName = "V_MAX")
        private String operatingEnd;       // 운영 종료 시각 (예: 19:00)

        // getters
        public String getImgUrl() { return imgUrl; }
        public String getTelNo() { return telNo; }
        public String getOperatingStart() { return operatingStart; }
        public String getOperatingEnd() { return operatingEnd; }
        public String getSvcId() { return svcId; }
        public String getMaxClassNm() { return maxClassNm; }
        public String getMinClassNm() { return minClassNm; }
        public String getSvcStatNm() { return svcStatNm; }
        public String getSvcNm() { return svcNm; }
        public String getPayAtNm() { return payAtNm; }
        public String getPlaceNm() { return placeNm; }
        public String getSvcUrl() { return svcUrl; }
        public String getX() { return x; }
        public String getY() { return y; }
        public String getReceptionBeginDt() { return receptionBeginDt; }
        public String getReceptionEndDt() { return receptionEndDt; }
        public String getAreaNm() { return areaNm; }
    }
}
