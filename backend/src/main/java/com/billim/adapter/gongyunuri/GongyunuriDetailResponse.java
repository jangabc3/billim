package com.billim.adapter.gongyunuri;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GongyunuriDetailResponse {

    private String resultCode;
    private String resultMsg;
    private List<Row> data;

    public String getResultCode() {
        return resultCode;
    }

    public List<Row> getData() {
        return data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Row {
        private String rsrcNo;
        private String rsrcClsNm; // 세부 카테고리명, 예: "캠핑·레저"
        private String freeYn; // "Y" 무료 / "N" 유료

        public String getRsrcNo() {
            return rsrcNo;
        }

        public String getRsrcClsNm() {
            return rsrcClsNm;
        }

        public String getFreeYn() {
            return freeYn;
        }
    }
}