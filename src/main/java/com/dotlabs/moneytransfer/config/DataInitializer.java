package com.dotlabs.moneytransfer.config;

import com.dotlabs.moneytransfer.entity.Account;
import com.dotlabs.moneytransfer.enums.Currency;
import com.dotlabs.moneytransfer.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AccountRepository accountRepository;

    @Override
    public void run(String... args) {
        if (accountRepository.count() == 0) {
            log.info("Seeding initial demo bank accounts...");

            List<Account> seedAccounts = List.of(
                    Account.builder()
                            .accountNumber("1000000001")
                            .accountHolderName("Alice Johnson")
                            .balance(new BigDecimal("500000.00"))
                            .currency(Currency.NGN)
                            .build(),
                    Account.builder()
                            .accountNumber("1000000002")
                            .accountHolderName("Bob Smith")
                            .balance(new BigDecimal("250000.00"))
                            .currency(Currency.NGN)
                            .build(),
                    Account.builder()
                            .accountNumber("1000000003")
                            .accountHolderName("Charlie Brown (Low Balance)")
                            .balance(new BigDecimal("50.00"))
                            .currency(Currency.NGN)
                            .build(),
                    Account.builder()
                            .accountNumber("1000000004")
                            .accountHolderName("DotLabs Treasury")
                            .balance(new BigDecimal("10000000.00"))
                            .currency(Currency.NGN)
                            .build()
            );

            accountRepository.saveAll(seedAccounts);
            log.info("Successfully seeded {} bank accounts.", seedAccounts.size());
        }
    }
}
