package com.billim.adapter.seoul;

import com.billim.domain.item.Category;
import com.billim.domain.resource.PublicResource;
import com.billim.domain.resource.ReceptionStatus;
import com.billim.domain.resource.ResourceSource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 서울시 공공서비스예약 API 테스트 도구에서 받은 응답(강남구 체육시설)을
 * 그대로 픽스처로 써서, 파싱이 깨지지 않는지 검증한다.
 */
class SeoulReservationAdapterTest {

    private final SeoulReservationAdapter adapter = new SeoulReservationAdapter();

    @Test
    void 강남구_체육시설_응답을_PublicResource로_변환한다() {
        // given: 2026-08-17에 실제로 받은 응답 (테스트 데이터에 개인정보 없음)
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <GNListPublicReservationSport>
            <list_total_count>1</list_total_count>
            <RESULT><CODE>INFO-000</CODE><MESSAGE>정상 처리되었습니다</MESSAGE></RESULT>
            <row>
            <GUBUN>자체</GUBUN>
            <SVCID>S220420104434156882</SVCID>
            <MAXCLASSNM>체육시설</MAXCLASSNM>
            <MINCLASSNM>테니스장</MINCLASSNM>
            <SVCSTATNM>접수중</SVCSTATNM>
            <SVCNM><![CDATA[탄천센터 테니스장 1면 조명쪽]]></SVCNM>
            <PAYATNM>무료</PAYATNM>
            <PLACENM>서울물재생시설공단&gt;탄천물재생센터</PLACENM>
            <SVCURL>https://yeyak.seoul.go.kr/web/reservation/selectReservView.do?rsv_svc_id=S220420104434156882</SVCURL>
            <X>127.07937089232</X>
            <Y>37.496830858969</Y>
            <RCPTBGNDT>2026-08-16 14:00:00.0</RCPTBGNDT>
            <RCPTENDDT>2026-12-31 23:59:00.0</RCPTENDDT>
            <AREANM>강남구</AREANM>
            </row>
            </GNListPublicReservationSport>
            """;

        // when
        List<PublicResource> result = adapter.parse(xml);

        // then
        assertThat(result).hasSize(1);
        PublicResource r = result.get(0);
        assertThat(r.getSource()).isEqualTo(ResourceSource.SEOUL_RESERVATION);
        assertThat(r.getName()).isEqualTo("탄천센터 테니스장 1면 조명쪽");
        assertThat(r.getCategory()).isEqualTo(Category.SPORTS);
        assertThat(r.getGu()).isEqualTo("강남구");
        assertThat(r.getFee()).isEqualTo("무료");
        assertThat(r.getReceptionStatus()).isIn(ReceptionStatus.OPEN, ReceptionStatus.CLOSING_SOON);
        // X=경도, Y=위도가 뒤바뀌지 않았는지 확인 — 실수하기 쉬운 지점이라 명시적으로 검증
        assertThat(r.getLongitude().doubleValue()).isCloseTo(127.079, org.assertj.core.data.Offset.offset(0.01));
        assertThat(r.getLatitude().doubleValue()).isCloseTo(37.497, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void rows가_없는_응답은_빈_리스트를_반환한다() {
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <GNListPublicReservationSport>
            <list_total_count>0</list_total_count>
            <RESULT><CODE>INFO-000</CODE><MESSAGE>조회된 데이터가 없습니다</MESSAGE></RESULT>
            </GNListPublicReservationSport>
            """;

        List<PublicResource> result = adapter.parse(xml);

        assertThat(result).isEmpty();
    }
}
