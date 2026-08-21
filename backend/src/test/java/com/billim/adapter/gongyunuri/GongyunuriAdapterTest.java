package com.billim.adapter.gongyunuri;

import com.billim.domain.resource.PublicResource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GongyunuriAdapterTest {

  private final GongyunuriAdapter adapter = new GongyunuriAdapter();

  @Test
  void 서울_주소가_아닌_데이터는_걸러진다() {
    // given: 실제로 받았던 응답 중 일부(세종 데이터 1건 + 서울 데이터 1건 임의 추가)
    String json = """
        {
          "resultCode": "OK",
          "resultMsg": "정상",
          "resultCount": 2,
          "data": [
            {
              "rsrcNo": "HH13T1328008",
              "rsrcNm": "세종지사 체성분분석기(인바디)",
              "addr": "세종특별자치시 세종로 1234-5 (아름동)",
              "daddr": "1층 종합민원실",
              "lot": 127.241982331961,
              "lat": 36.5134584370333,
              "instUrlAddr": "https://www.eshare.go.kr/test1",
              "imgFileUrlAddr": "https://www.eshare.go.kr/img1.jpg"
            },
            {
              "rsrcNo": "TEST_SEOUL_001",
              "rsrcNm": "성동구 전동드릴",
              "addr": "서울 성동구 성수동 123",
              "daddr": "1층",
              "lot": 127.047,
              "lat": 37.547,
              "instUrlAddr": "https://www.eshare.go.kr/test2",
              "imgFileUrlAddr": "https://www.eshare.go.kr/img2.jpg"
            }
          ]
        }
        """;

    // when
    List<PublicResource> result = adapter.parse(json, "020000");

    // then: 세종 데이터는 빠지고 서울 데이터 1건만 남아야 한다
    assertThat(result).hasSize(1);
    PublicResource seoulItem = result.get(0);
    assertThat(seoulItem.getGu()).isEqualTo("성동구");
  }
}