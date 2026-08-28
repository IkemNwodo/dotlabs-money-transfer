package com.dotlabs.moneytransfer.concurrency;

import com.dotlabs.moneytransfer.dto.request.TransferRequest;
import com.dotlabs.moneytransfer.dto.response.TransferResponse;
import com.dotlabs.moneytransfer.entity.Account;
import com.dotlabs.moneytransfer.enums.Currency;
import com.dotlabs.moneytransfer.enums.TransactionStatus;
import com.dotlabs.moneytransfer.repository.AccountRepository;
import com.dotlabs.moneytransfer.repository.TransactionRepository;
import com.dotlabs.moneytransfer.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ConcurrentTransferIntegrationTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private static final String ACC_1 = "CONC_ACC_1";
    private static final String ACC_2 = "CONC_ACC_2";

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();

        // Create two accounts with 50,000.00 each
        Account a1 = Account.builder()
                .accountNumber(ACC_1)
                .accountHolderName("Concurrent Test User 1")
                .balance(new BigDecimal("50000.00"))
                .currency(Currency.NGN)
                .build();

        Account a2 = Account.builder()
                .accountNumber(ACC_2)
                .accountHolderName("Concurrent Test User 2")
                .balance(new BigDecimal("50000.00"))
                .currency(Currency.NGN)
                .build();

        accountRepository.saveAll(List.of(a1, a2));
    }

    @Test
    @DisplayName("Should execute bidirectional concurrent transfers without deadlocks or lost updates")
    void testBidirectionalConcurrentTransfers() throws InterruptedException, ExecutionException {
        int numberOfTransfers = 20;
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<TransferResponse>> futures = new ArrayList<>();

        for (int i = 0; i < numberOfTransfers; i++) {
            final int index = i;
            // Alternate transfer direction (A -> B and B -> A) to test deadlock immunity
            boolean forward = (index % 2 == 0);
            String from = forward ? ACC_1 : ACC_2;
            String to = forward ? ACC_2 : ACC_1;
            BigDecimal amount = new BigDecimal("500.00"); // fee is 2.50, billed 502.50

            futures.add(executorService.submit(() -> {
                startLatch.await(); // wait for simultaneous start
                TransferRequest request = TransferRequest.builder()
                        .sourceAccountNumber(from)
                        .destinationAccountNumber(to)
                        .amount(amount)
                        .description("Concurrent transfer #" + index)
                        .build();
                return transferService.processTransfer(request);
            }));
        }

        // Trigger all threads simultaneously
        startLatch.countDown();

        BigDecimal totalFeesCollected = BigDecimal.ZERO;
        int successfulCount = 0;

        for (Future<TransferResponse> future : futures) {
            TransferResponse response = future.get();
            if (response.getStatus() == TransactionStatus.SUCCESSFUL) {
                successfulCount++;
                totalFeesCollected = totalFeesCollected.add(response.getTransactionFee());
            }
        }

        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);

        assertThat(successfulCount).isEqualTo(numberOfTransfers);

        Account finalAcc1 = accountRepository.findByAccountNumber(ACC_1).orElseThrow();
        Account finalAcc2 = accountRepository.findByAccountNumber(ACC_2).orElseThrow();

        // Initial total money was 100,000.00. Total money remaining + total fees collected must equal 100,000.00!
        BigDecimal finalTotalInAccounts = finalAcc1.getBalance().add(finalAcc2.getBalance());
        BigDecimal totalAccountedFor = finalTotalInAccounts.add(totalFeesCollected);

        assertThat(totalAccountedFor).isEqualByComparingTo("100000.00");
    }
}
