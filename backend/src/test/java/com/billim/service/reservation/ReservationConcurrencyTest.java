package com.billim.service.reservation;

import com.billim.domain.institution.Institution;
import com.billim.domain.item.Category;
import com.billim.domain.item.FeeType;
import com.billim.domain.item.Inventory;
import com.billim.domain.item.RentalItem;
import com.billim.domain.user.User;
import com.billim.domain.user.UserRole;
import com.billim.repository.InventoryRepository;
import com.billim.repository.RentalItemRepository;
import com.billim.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ReservationConcurrencyTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private RentalItemRepository rentalItemRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void 재고_1개에_동시_예약_10건_중_1건만_성공한다() throws InterruptedException {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        RentalItem rentalItem = txTemplate.execute(status -> createRentalItemWithStock(1));
        int threadCount = 10;
        Long[] userIds = new Long[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            userIds[i] = txTemplate.execute(
                    status -> createUser("concurrency-test-" + idx + "-" + System.nanoTime() + "@billim.com").getId());
        }

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        Long rentalItemId = rentalItem.getId();
        for (int i = 0; i < threadCount; i++) {
            Long userId = userIds[i];
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    reservationService.reserve(userId, rentalItemId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(threadCount - 1);

        Inventory finalInventory = inventoryRepository.findByRentalItemId(rentalItemId).orElseThrow();
        assertThat(finalInventory.getAvailableQuantity()).isEqualTo(0);
    }

    private RentalItem createRentalItemWithStock(int stock) {
        Institution institution = new Institution(
                "TEST-" + System.nanoTime(), "테스트기관", "테스트 주소", "성동구", "성수동",
                BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.0));
        entityManager.persist(institution);

        RentalItem rentalItem = new RentalItem(institution, "동시성테스트물품", Category.TOOL,
                FeeType.FREE, null, 3, false);
        rentalItemRepository.save(rentalItem);

        Inventory inventory = new Inventory(rentalItem, stock);
        inventoryRepository.save(inventory);

        return rentalItem;
    }

    private User createUser(String email) {
        User user = new User(email, "test-hash", "동시성테스트유저", UserRole.USER);
        return userRepository.save(user);
    }
}