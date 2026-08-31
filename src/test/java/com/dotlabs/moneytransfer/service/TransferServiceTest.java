package com.dotlabs.moneytransfer.service;

import com.dotlabs.moneytransfer.dto.request.TransferRequest;
import com.dotlabs.moneytransfer.dto.response.TransferResponse;
import com.dotlabs.moneytransfer.entity.Account;
import com.dotlabs.moneytransfer.entity.Transaction;
import com.dotlabs.moneytransfer.enums.Currency;
import com.dotlabs.moneytransfer.enums.TransactionStatus;
import com.dotlabs.moneytransfer.exception.AccountNotFoundException;
import com.dotlabs.moneytransfer.exception.InvalidTransferException;
import com.dotlabs.moneytransfer.repository.AccountRepository;
import com.dotlabs.moneytransfer.repository.TransactionRepository;
import com.dotlabs.moneytransfer.service.impl.TransferServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransferServiceImpl transferService;

    private Account senderAccount;
    private Account recipientAccount;

    @BeforeEach
    void setUp() {
        senderAccount = Account.builder()
                .id(1L)
                .accountNumber("1000000001")
                .accountHolderName("Emmanuel Ugwueze")
                .balance(new BigDecimal("10000.00"))
                .currency(Currency.NGN)
                .build();

        recipientAccount = Account.builder()
                .id(2L)
                .accountNumber("1000000002")
                .accountHolderName("Ekene iloezumma")
                .balance(new BigDecimal("5000.00"))
                .currency(Currency.NGN)
                .build();
    }

    @Test
    @DisplayName("Should successfully process transfer when funds are sufficient")
    void testSuccessfulTransfer() {
        TransferRequest request = TransferRequest.builder()
                .sourceAccountNumber("1000000001")
                .destinationAccountNumber("1000000002")
                .amount(new BigDecimal("2000.00"))
                .description("Lunch payment")
                .build();

        when(transactionRepository.existsByTransactionReference(anyString())).thenReturn(false);
        when(accountRepository.findByAccountNumberWithLock("1000000001")).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByAccountNumberWithLock("1000000002")).thenReturn(Optional.of(recipientAccount));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransferResponse response = transferService.processTransfer(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.SUCCESSFUL);
        assertThat(response.getAmount()).isEqualByComparingTo("2000.00");
        assertThat(response.getTransactionFee()).isEqualByComparingTo("10.00"); // 2000 * 0.005 = 10.00
        assertThat(response.getBilledAmount()).isEqualByComparingTo("2010.00"); // 2000 + 10.00

        // Check balances
        assertThat(senderAccount.getBalance()).isEqualByComparingTo("7990.00"); // 10000 - 2010
        assertThat(recipientAccount.getBalance()).isEqualByComparingTo("7000.00"); // 5000 + 2000

        verify(accountRepository).save(senderAccount);
        verify(accountRepository).save(recipientAccount);
    }

    @Test
    @DisplayName("Should record INSUFFICIENT_FUNDS transaction when sender lacks balance")
    void testInsufficientFundsTransfer() {
        senderAccount.setBalance(new BigDecimal("100.00")); // only 100 available

        TransferRequest request = TransferRequest.builder()
                .sourceAccountNumber("1000000001")
                .destinationAccountNumber("1000000002")
                .amount(new BigDecimal("500.00")) // fee is 2.50, total billed is 502.50
                .description("Rent")
                .build();

        when(transactionRepository.existsByTransactionReference(anyString())).thenReturn(false);
        when(accountRepository.findByAccountNumberWithLock("1000000001")).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByAccountNumberWithLock("1000000002")).thenReturn(Optional.of(recipientAccount));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransferResponse response = transferService.processTransfer(request);

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.INSUFFICIENT_FUNDS);
        assertThat(response.getStatusMessage()).contains("Insufficient funds");

        // Sender and recipient balances must remain untouched
        assertThat(senderAccount.getBalance()).isEqualByComparingTo("100.00");
        assertThat(recipientAccount.getBalance()).isEqualByComparingTo("5000.00");

        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should reject transfer when source and destination accounts are identical")
    void testSameAccountTransferThrows() {
        TransferRequest request = TransferRequest.builder()
                .sourceAccountNumber("1000000001")
                .destinationAccountNumber("1000000001")
                .amount(new BigDecimal("100.00"))
                .build();

        assertThatThrownBy(() -> transferService.processTransfer(request))
                .isInstanceOf(InvalidTransferException.class)
                .hasMessageContaining("cannot be the same");
    }

    @Test
    @DisplayName("Should throw AccountNotFoundException when account does not exist")
    void testAccountNotFound() {
        TransferRequest request = TransferRequest.builder()
                .sourceAccountNumber("9999999999")
                .destinationAccountNumber("1000000002")
                .amount(new BigDecimal("100.00"))
                .build();

        when(transactionRepository.existsByTransactionReference(anyString())).thenReturn(false);
        when(accountRepository.findByAccountNumberWithLock("9999999999")).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumberWithLock("1000000002")).thenReturn(Optional.of(recipientAccount));

        assertThatThrownBy(() -> transferService.processTransfer(request))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("Source account not found");
    }
}
