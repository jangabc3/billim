package com.billim.adapter;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 외부 공공 API(공유누리·서울시) 호출을 담당하는 공통 HTTP 클라이언트.
 *
 * Retry/CircuitBreaker가 실제로 동작하려면 Spring AOP 프록시를 거쳐 호출돼야 하는데,
 * 같은 클래스 안의 메서드를 this.로 직접 부르면 프록시를 우회해 애노테이션이 무시된다
 * (Reservation 동시성 처리에서 InventoryReservationExecutor를 별도 빈으로 분리한 것과
 * 동일한 이유의 self-invocation 문제). 그래서 GongyunuriAdapter/SeoulReservationAdapter와
 * 분리된 별도 빈으로 둔다.
 *
 * RestClient의 read timeout이 실제로 적용되지 않는 문제를 겪어, 순수 java.net.http.HttpClient로
 * 직접 타임아웃을 제어한다 (연결 5초, 응답 대기 15초).
 */
@Component
public class ResilientApiClient {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Retry(name = "gongyunuri")
    @CircuitBreaker(name = "gongyunuri")
    public String postToGongyunuri(String url, String requestBody, String label) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            return send(request, label);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(label + " 호출 실패: " + e.getMessage(), e);
        }
    }

    @Retry(name = "seoul")
    @CircuitBreaker(name = "seoul")
    public String getFromSeoul(String url, String label) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
            return send(request, label);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(label + " 호출 실패: " + e.getMessage(), e);
        }
    }

    private String send(HttpRequest request, String label) throws Exception {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    label + " 실패 (status=" + response.statusCode() + "): " + response.body());
        }
        return response.body();
    }
}