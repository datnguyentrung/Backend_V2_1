package com.dat.ai_receptionist_web.service.Finance;

import com.dat.ai_receptionist_web.domain.Finance.WalletTransaction;
import com.dat.ai_receptionist_web.dto.Finance.WalletTransactionDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.mapper.Finance.WalletTransactionMapper;
import com.dat.ai_receptionist_web.repository.Finance.WalletTransactionRepository;
import com.dat.ai_receptionist_web.repository.Finance.WalletRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import com.dat.ai_receptionist_web.enums.Finance.WalletTransactionStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WalletTransactionService {
    private final WalletTransactionRepository repository;
    private final WalletTransactionMapper mapper;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    /**
     * Tác dụng: Lấy danh sách bản ghi theo điều kiện phân trang.
     * Input: Nhận Pageable pageable từ caller hoặc request.
     * Output: Trả về PageResponse<WalletTransactionDTO.Response> theo kết quả xử lý.
     */
    public PageResponse<WalletTransactionDTO.Response> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    /**
     * Tác dụng: Lấy chi tiết một bản ghi theo khóa định danh.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về WalletTransactionDTO.Response theo kết quả xử lý.
     */
    public WalletTransactionDTO.Response get(UUID id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    /**
     * Tác dụng: Tạo mới bản ghi và trả về dữ liệu sau khi tạo.
     * Input: Nhận WalletTransactionDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về WalletTransactionDTO.Response theo kết quả xử lý.
     */
    public WalletTransactionDTO.Response create(WalletTransactionDTO.CreateRequest request) {
        WalletTransaction entity = new WalletTransaction();
        entity.setWallet(walletRepository.findById(request.walletId()).orElseThrow(() -> new IllegalArgumentException("Wallet not found")));
        entity.setCreatedByUser(userRepository.findById(request.createdByUserId()).orElseThrow(() -> new IllegalArgumentException("User not found")));
        entity.setApprovedByUser(userRepository.findById(request.approvedByUserId()).orElseThrow(() -> new IllegalArgumentException("User not found")));
        entity.setType(request.type());
        entity.setDirection(request.direction());
        entity.setAmount(request.amount());
        entity.setBalanceBefore(request.balanceBefore());
        entity.setBalanceAfter(request.balanceAfter());
        entity.setExternalReference(request.externalReference());
        entity.setApprovedAt(request.approvedAt());
        entity.setNote(request.note());
        entity.setStatus(request.status());
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    /**
     * Tác dụng: Cập nhật bản ghi hiện có và trả về dữ liệu sau khi cập nhật.
     * Input: Nhận UUID id, WalletTransactionDTO.UpdateRequest request từ caller hoặc request.
     * Output: Trả về WalletTransactionDTO.Response theo kết quả xử lý.
     */
    public WalletTransactionDTO.Response update(UUID id, WalletTransactionDTO.UpdateRequest request) {
        var entity = find(id);
        entity.setWallet(walletRepository.findById(request.walletId()).orElseThrow(() -> new IllegalArgumentException("Wallet not found")));
        entity.setCreatedByUser(userRepository.findById(request.createdByUserId()).orElseThrow(() -> new IllegalArgumentException("User not found")));
        entity.setApprovedByUser(userRepository.findById(request.approvedByUserId()).orElseThrow(() -> new IllegalArgumentException("User not found")));
        mapper.updateEntity(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    /**
     * Tác dụng: Xóa hoặc vô hiệu hóa bản ghi theo định danh đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    public void delete(UUID id) {
        var entity = find(id);
        entity.setStatus(WalletTransactionStatus.REJECTED);
    }

    /**
     * Tác dụng: Tìm và trả về dữ liệu nội bộ theo điều kiện đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về WalletTransaction theo kết quả xử lý.
     */
    private WalletTransaction find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("WalletTransaction not found"));
    }
}


