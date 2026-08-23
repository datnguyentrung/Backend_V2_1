package com.dat.ai_receptionist_web.service.Finance;

import com.dat.ai_receptionist_web.domain.Catalog.*;
import com.dat.ai_receptionist_web.domain.Finance.*;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.domain.Training.StudentEnrollment;
import com.dat.ai_receptionist_web.dto.Finance.WalletCommandDTO;
import com.dat.ai_receptionist_web.enums.Catalog.*;
import com.dat.ai_receptionist_web.enums.Finance.*;
import com.dat.ai_receptionist_web.enums.Operation.StudentEnrollmentStatus;
import com.dat.ai_receptionist_web.repository.Catalog.*;
import com.dat.ai_receptionist_web.repository.Finance.*;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import com.dat.ai_receptionist_web.repository.Training.StudentEnrollmentRepository;
import com.dat.ai_receptionist_web.util.error.FinancialException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletCommandService {
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final CoursePurchaseRepository purchaseRepository;
    private final CoursePriceRepository coursePriceRepository;
    private final CourseRepository courseRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    @Transactional
    public WalletCommandDTO.TransactionResponse topUp(
            WalletCommandDTO.TopUpRequest request, UUID actorUserId) {
        BigDecimal amount = money(request.amount());
        Wallet wallet = lockWalletByPerson(request.personId());
        WalletTransaction existing = transactionRepository
                .findByTypeAndExternalReference(WalletTransactionType.TOP_UP, request.externalReference())
                .orElse(null);
        if (existing != null) {
            assertSameOperation(existing, wallet, amount);
            return response(existing, null, null);
        }
        requireActive(wallet);
        User actor = user(actorUserId);
        BigDecimal before = wallet.getBalance();
        BigDecimal after = before.add(amount);
        WalletTransaction transaction = approvedTransaction(wallet, WalletTransactionType.TOP_UP,
                WalletTransactionDirection.CREDIT, amount, before, after,
                request.externalReference(), actor, request.note());
        wallet.setBalance(after);
        transactionRepository.save(transaction);
        return response(transaction, null, null);
    }

    @Transactional
    public WalletCommandDTO.TransactionResponse purchaseCourse(
            WalletCommandDTO.CoursePurchaseRequest request, UUID actorUserId) {
        Wallet wallet = lockWalletByPerson(request.studentPersonId());
        WalletTransaction existing = transactionRepository
                .findByTypeAndExternalReference(WalletTransactionType.COURSE_PURCHASE,
                        request.externalReference()).orElse(null);
        if (existing != null) {
            return existingCoursePurchase(existing, wallet, request);
        }
        requireActive(wallet);
        CoursePrice price = coursePriceRepository.findForPurchase(request.coursePriceId())
                .orElseThrow(() -> failure("COURSE_PRICE_NOT_FOUND", HttpStatus.NOT_FOUND,
                        "Course price not found"));
        Course course = courseRepository.findByIdForUpdate(price.getCourse().getCourseId())
                .orElseThrow(() -> failure("COURSE_NOT_FOUND", HttpStatus.NOT_FOUND, "Course not found"));
        if (price.getStatus() != CoursePriceStatus.ACTIVE || course.getStatus() != CourseStatus.ACTIVE) {
            throw failure("COURSE_NOT_AVAILABLE", HttpStatus.CONFLICT, "Course or price is inactive");
        }
        BigDecimal amount = money(price.getFinalPrice());
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw failure("INSUFFICIENT_BALANCE", HttpStatus.CONFLICT, "Wallet balance is insufficient");
        }
        long enrollmentCount = enrollmentRepository
                .countByCoursePurchase_CoursePrice_Course_CourseId(course.getCourseId());
        if (enrollmentCount >= course.getCapacity()) {
            throw failure("COURSE_CAPACITY_EXCEEDED", HttpStatus.CONFLICT, "Course is full");
        }

        User actor = user(actorUserId);
        BigDecimal before = wallet.getBalance();
        BigDecimal after = before.subtract(amount);
        WalletTransaction transaction = transactionRepository.save(approvedTransaction(
                wallet, WalletTransactionType.COURSE_PURCHASE, WalletTransactionDirection.DEBIT,
                amount, before, after, request.externalReference(), actor, request.note()));
        CoursePurchase purchase = purchaseRepository.save(CoursePurchase.builder()
                .studentPerson(wallet.getPerson())
                .coursePrice(price)
                .debitTransaction(transaction)
                .build());
        LocalDate start = LocalDate.now();
        StudentEnrollment enrollment = enrollmentRepository.save(StudentEnrollment.builder()
                .studentPerson(wallet.getPerson())
                .coursePurchase(purchase)
                .classSchedule(course.getClassSchedule())
                .startDate(start)
                .endDate(start.plusMonths(price.getDurationMonths()))
                .status(StudentEnrollmentStatus.ACTIVE)
                .build());
        wallet.setBalance(after);
        return response(transaction, purchase, enrollment);
    }

    @Transactional
    public WalletCommandDTO.TransactionResponse refund(
            WalletCommandDTO.RefundRequest request, UUID actorUserId) {
        WalletTransaction original = transactionRepository
                .findWithWalletById(request.originalDebitTransactionId())
                .orElseThrow(() -> failure("TRANSACTION_NOT_FOUND", HttpStatus.NOT_FOUND,
                        "Original transaction not found"));
        if (original.getDirection() != WalletTransactionDirection.DEBIT
                || original.getStatus() != WalletTransactionStatus.APPROVED) {
            throw failure("INVALID_REFUND_TRANSACTION", HttpStatus.CONFLICT,
                    "Original transaction is not an approved debit");
        }
        Wallet wallet = walletRepository.findByIdForUpdate(original.getWallet().getWalletId())
                .orElseThrow(() -> failure("WALLET_NOT_FOUND", HttpStatus.NOT_FOUND, "Wallet not found"));
        requireActive(wallet);
        String originalReference = original.getWalletTransactionId().toString();
        WalletTransaction existing = transactionRepository
                .findByTypeAndExternalReference(WalletTransactionType.REFUND, originalReference)
                .orElse(null);
        if (existing != null) {
            return response(existing, null, null);
        }
        BigDecimal amount = original.getAmount();
        BigDecimal before = wallet.getBalance();
        BigDecimal after = before.add(amount);
        User actor = user(actorUserId);
        WalletTransaction refund = transactionRepository.save(approvedTransaction(
                wallet, WalletTransactionType.REFUND, WalletTransactionDirection.CREDIT,
                amount, before, after, originalReference, actor, request.note()));
        revokeCourseEntitlement(original);
        wallet.setBalance(after);
        return response(refund, null, null);
    }

    private WalletCommandDTO.TransactionResponse existingCoursePurchase(
            WalletTransaction existing, Wallet wallet, WalletCommandDTO.CoursePurchaseRequest request) {
        CoursePurchase purchase = purchaseRepository
                .findByDebitTransaction_WalletTransactionId(existing.getWalletTransactionId())
                .orElseThrow(() -> failure("LEDGER_INVARIANT_VIOLATION",
                        HttpStatus.CONFLICT, "Purchase record is missing"));
        if (!existing.getWallet().getWalletId().equals(wallet.getWalletId())
                || !purchase.getStudentPerson().getPersonId().equals(request.studentPersonId())
                || !purchase.getCoursePrice().getCoursePriceId().equals(request.coursePriceId())) {
            throw failure("IDEMPOTENCY_CONFLICT", HttpStatus.CONFLICT,
                    "External reference is already used by another operation");
        }
        StudentEnrollment enrollment = enrollmentRepository
                .findByCoursePurchase_CoursePurchaseId(purchase.getCoursePurchaseId())
                .orElseThrow(() -> failure("LEDGER_INVARIANT_VIOLATION",
                        HttpStatus.CONFLICT, "Enrollment record is missing"));
        return response(existing, purchase, enrollment);
    }

    private void revokeCourseEntitlement(WalletTransaction original) {
        if (original.getType() != WalletTransactionType.COURSE_PURCHASE) {
            return;
        }
        CoursePurchase purchase = purchaseRepository
                .findByDebitTransaction_WalletTransactionId(original.getWalletTransactionId())
                .orElseThrow(() -> failure("LEDGER_INVARIANT_VIOLATION",
                        HttpStatus.CONFLICT, "Purchase record is missing"));
        StudentEnrollment enrollment = enrollmentRepository
                .findByCoursePurchase_CoursePurchaseId(purchase.getCoursePurchaseId())
                .orElseThrow(() -> failure("LEDGER_INVARIANT_VIOLATION",
                        HttpStatus.CONFLICT, "Enrollment record is missing"));
        enrollment.setStatus(StudentEnrollmentStatus.CANCELLED);
        LocalDate today = LocalDate.now();
        if (enrollment.getEndDate().isAfter(today)) {
            enrollment.setEndDate(today);
        }
    }

    private WalletTransaction approvedTransaction(Wallet wallet, WalletTransactionType type,
                                                   WalletTransactionDirection direction,
                                                   BigDecimal amount, BigDecimal before, BigDecimal after,
                                                   String reference, User actor, String note) {
        LocalDateTime now = LocalDateTime.now();
        return WalletTransaction.builder().wallet(wallet).type(type).direction(direction)
                .amount(amount).balanceBefore(before).balanceAfter(after)
                .externalReference(reference).createdByUser(actor).approvedByUser(actor)
                .approvedAt(now).note(note).status(WalletTransactionStatus.APPROVED).build();
    }

    private Wallet lockWalletByPerson(UUID personId) {
        return walletRepository.findByPersonIdForUpdate(personId)
                .orElseThrow(() -> failure("WALLET_NOT_FOUND", HttpStatus.NOT_FOUND, "Wallet not found"));
    }

    private void requireActive(Wallet wallet) {
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw failure("WALLET_NOT_ACTIVE", HttpStatus.CONFLICT, "Wallet is not active");
        }
    }

    private User user(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> failure("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "User not found"));
    }

    private BigDecimal money(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0 || amount.stripTrailingZeros().scale() > 0) {
            throw failure("INVALID_AMOUNT", HttpStatus.BAD_REQUEST,
                    "Amount must be a positive whole monetary value");
        }
        return amount.setScale(0);
    }

    private void assertSameOperation(WalletTransaction existing, Wallet wallet, BigDecimal amount) {
        if (!existing.getWallet().getWalletId().equals(wallet.getWalletId())
                || existing.getAmount().compareTo(amount) != 0) {
            throw failure("IDEMPOTENCY_CONFLICT", HttpStatus.CONFLICT,
                    "External reference is already used by another operation");
        }
    }

    private WalletCommandDTO.TransactionResponse response(WalletTransaction tx,
                                                           CoursePurchase purchase,
                                                           StudentEnrollment enrollment) {
        return new WalletCommandDTO.TransactionResponse(tx.getWalletTransactionId(),
                tx.getWallet().getWalletId(), tx.getType(), tx.getDirection(), tx.getStatus(),
                tx.getAmount(), tx.getBalanceBefore(), tx.getBalanceAfter(), tx.getExternalReference(),
                purchase == null ? null : purchase.getCoursePurchaseId(),
                enrollment == null ? null : enrollment.getStudentEnrollmentId(), tx.getApprovedAt());
    }

    private FinancialException failure(String code, HttpStatus status, String message) {
        return new FinancialException(code, status, message);
    }
}
