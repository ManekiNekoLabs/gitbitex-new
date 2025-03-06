package com.gitbitex.wallet.controller;

import com.gitbitex.marketdata.entity.User;
import com.gitbitex.wallet.dto.AddressDto;
import com.gitbitex.wallet.dto.DepositDto;
import com.gitbitex.wallet.dto.WithdrawalDto;
import com.gitbitex.wallet.dto.WithdrawalRequestDto;
import com.gitbitex.wallet.model.Deposit;
import com.gitbitex.wallet.model.WalletAddress;
import com.gitbitex.wallet.model.Withdrawal;
import com.gitbitex.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@ConditionalOnBean(WalletService.class)
public class WalletController {
    private final WalletService walletService;

    @GetMapping("/addresses/{currency}")
    public List<AddressDto> getAddresses(@PathVariable String currency,
                                         @RequestAttribute(required = false) User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        try {
            List<WalletAddress> addresses = walletService.getAddresses(currentUser.getId(), currency);
            return addresses.stream().map(this::toAddressDto).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting addresses: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @PostMapping("/addresses/{currency}")
    public AddressDto generateAddress(@PathVariable String currency,
                                      @RequestAttribute(required = false) User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        try {
            WalletAddress address = walletService.generateAddress(currentUser.getId(), currency);
            if (address == null) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Wallet service is not available");
            }
            return toAddressDto(address);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            logger.error("Error generating address: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate address");
        }
    }

    @GetMapping("/deposits")
    public List<DepositDto> getDeposits(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "20") int size,
                                        @RequestAttribute(required = false) User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        try {
            List<Deposit> deposits = walletService.getDeposits(currentUser.getId(), page, size);
            return deposits.stream().map(this::toDepositDto).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting deposits: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @GetMapping("/withdrawals")
    public List<WithdrawalDto> getWithdrawals(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "20") int size,
                                              @RequestAttribute(required = false) User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        try {
            List<Withdrawal> withdrawals = walletService.getWithdrawals(currentUser.getId(), page, size);
            return withdrawals.stream().map(this::toWithdrawalDto).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting withdrawals: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @PostMapping("/withdrawals")
    public WithdrawalDto requestWithdrawal(@RequestBody @Valid WithdrawalRequestDto request,
                                           @RequestAttribute(required = false) User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        try {
            Withdrawal withdrawal = walletService.requestWithdrawal(
                    currentUser.getId(),
                    request.getCurrency(),
                    request.getAddress(),
                    request.getAmount());
            
            if (withdrawal == null) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Wallet service is not available");
            }

            return toWithdrawalDto(withdrawal);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            logger.error("Error requesting withdrawal: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to request withdrawal");
        }
    }

    // Admin endpoints (should be secured appropriately in production)
    @PostMapping("/admin/withdrawals/{id}/approve")
    public WithdrawalDto approveWithdrawal(@PathVariable String id,
                                           @RequestAttribute(required = false) User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        try {
            // In a real implementation, check if user has admin privileges
            Withdrawal withdrawal = walletService.approveWithdrawal(id);
            if (withdrawal == null) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Wallet service is not available");
            }
            return toWithdrawalDto(withdrawal);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            logger.error("Error approving withdrawal: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to approve withdrawal");
        }
    }

    @PostMapping("/admin/withdrawals/{id}/reject")
    public WithdrawalDto rejectWithdrawal(@PathVariable String id,
                                          @RequestAttribute(required = false) User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        try {
            // In a real implementation, check if user has admin privileges
            Withdrawal withdrawal = walletService.rejectWithdrawal(id);
            if (withdrawal == null) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Wallet service is not available");
            }
            return toWithdrawalDto(withdrawal);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            logger.error("Error rejecting withdrawal: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to reject withdrawal");
        }
    }

    private AddressDto toAddressDto(WalletAddress address) {
        AddressDto dto = new AddressDto();
        dto.setId(address.getId());
        dto.setCurrency(address.getCurrency());
        dto.setAddress(address.getAddress());
        dto.setCreatedAt(address.getCreatedAt().toString());
        return dto;
    }

    private DepositDto toDepositDto(Deposit deposit) {
        DepositDto dto = new DepositDto();
        dto.setId(deposit.getId());
        dto.setCurrency(deposit.getCurrency());
        dto.setAddress(deposit.getAddress());
        dto.setAmount(deposit.getAmount().toString());
        dto.setTxId(deposit.getTxId());
        dto.setConfirmations(deposit.getConfirmations());
        dto.setStatus(deposit.getStatus().name());
        dto.setCreatedAt(deposit.getCreatedAt().toString());
        return dto;
    }

    private WithdrawalDto toWithdrawalDto(Withdrawal withdrawal) {
        WithdrawalDto dto = new WithdrawalDto();
        dto.setId(withdrawal.getId());
        dto.setCurrency(withdrawal.getCurrency());
        dto.setAddress(withdrawal.getAddress());
        dto.setAmount(withdrawal.getAmount().toString());
        dto.setTxId(withdrawal.getTxId());
        dto.setStatus(withdrawal.getStatus().name());
        dto.setCreatedAt(withdrawal.getCreatedAt().toString());
        return dto;
    }
} 