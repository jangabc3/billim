package com.billim.adapter.gongyunuri;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GongyunuriListResponse {

    private String resultCode;
    private String resultMsg;
    private int resultCount;
    private List<Row> data;

    public String getResultCode() {
        return resultCode;
    }

    public int getResultCount() {
        return resultCount;
    }

    public List<Row> getData() {
        return data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Row {
        private String rsrcNo;
        private String rsrcNm;
        private String zip;
        private String addr;
        private String daddr;
        private Double lot; // 경도 (문서엔 "위도"로 오타나 있음, 실제로는 경도)
        private Double lat; // 위도
        private String instUrlAddr;
        private String imgFileUrlAddr;

        public String getRsrcNo() {
            return rsrcNo;
        }

        public String getRsrcNm() {
            return rsrcNm;
        }

        public String getAddr() {
            return addr;
        }

        public String getDaddr() {
            return daddr;
        }

        public Double getLot() {
            return lot;
        }

        public Double getLat() {
            return lat;
        }

        public String getInstUrlAddr() {
            return instUrlAddr;
        }

        public String getImgFileUrlAddr() {
            return imgFileUrlAddr;
        }
    }
}