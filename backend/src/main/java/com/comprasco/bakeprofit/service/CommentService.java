package com.comprasco.bakeprofit.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comprasco.bakeprofit.dto.CommentResponse;
import com.comprasco.bakeprofit.entity.Comment;
import com.comprasco.bakeprofit.entity.CommentReport;
import com.comprasco.bakeprofit.entity.CommentStatus;
import com.comprasco.bakeprofit.entity.Product;
import com.comprasco.bakeprofit.entity.ReportReason;
import com.comprasco.bakeprofit.entity.Role;
import com.comprasco.bakeprofit.entity.Store;
import com.comprasco.bakeprofit.entity.User;
import com.comprasco.bakeprofit.exception.CommentNotAllowedException;
import com.comprasco.bakeprofit.exception.CommentNotFoundException;
import com.comprasco.bakeprofit.exception.ForbiddenRoleException;
import com.comprasco.bakeprofit.exception.InvalidCommentRequestException;
import com.comprasco.bakeprofit.exception.PriceNotFoundException;
import com.comprasco.bakeprofit.repository.CommentReportRepository;
import com.comprasco.bakeprofit.repository.CommentRepository;
import com.comprasco.bakeprofit.repository.PriceRepository;

@Service
@Transactional(readOnly = true)
public class CommentService {

    private static final int MAX_PAGE_SIZE = 50;

    private final CommentRepository commentRepository;
    private final CommentReportRepository commentReportRepository;
    private final PriceRepository priceRepository;
    private final ProductService productService;
    private final StoreService storeService;

    public CommentService(CommentRepository commentRepository,
                          CommentReportRepository commentReportRepository,
                          PriceRepository priceRepository,
                          ProductService productService,
                          StoreService storeService) {
        this.commentRepository = commentRepository;
        this.commentReportRepository = commentReportRepository;
        this.priceRepository = priceRepository;
        this.productService = productService;
        this.storeService = storeService;
    }

    /**
     * Publica un comentario sobre un producto en una tienda (RF14, RF16).
     * Orden: rol (403) -> existencia (404) -> activos (409) -> precio del par (404).
     */
    @Transactional
    public CommentResponse create(User user, Long productId, Long storeId, String text) {
        if (user.getRole() != Role.USER) {
            throw new ForbiddenRoleException("Solo los usuarios pueden comentar.");
        }
        Product product = productService.findById(productId);
        Store store = storeService.findById(storeId);

        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new CommentNotAllowedException(
                    "No se puede comentar el producto '" + product.getName() + "' porque está inactivo.");
        }
        if (!Boolean.TRUE.equals(store.getActive())) {
            throw new CommentNotAllowedException(
                    "No se puede comentar en la tienda '" + store.getName() + "' porque está inactiva.");
        }
        if (!priceRepository.existsByProductIdAndStoreId(productId, storeId)) {
            throw new PriceNotFoundException(
                    "El producto '" + product.getName() + "' no tiene precio registrado en la tienda '"
                    + store.getName() + "'.");
        }
        return CommentResponse.from(commentRepository.save(Comment.of(user, product, store, text)));
    }

    /**
     * Lista los comentarios visibles (todo menos OCULTO), del más reciente al más antiguo.
     * Con productId y storeId: el producto en esa tienda. Solo uno: producto global o tienda completa.
     */
    public PagedModel<CommentResponse> list(Long productId, Long storeId, int page, int size) {
        if (productId == null && storeId == null) {
            throw new InvalidCommentRequestException("Debes indicar productId, storeId o ambos.");
        }
        if (productId != null) productService.findById(productId);
        if (storeId != null) storeService.findById(storeId);

        // El cliente no elige el orden: un sort libre permitiría ordenar por columnas internas
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Order.desc("registrationDate"), Sort.Order.desc("id")));

        Page<Comment> result;
        if (productId != null && storeId != null) {
            result = commentRepository.findByProductIdAndStoreIdAndStatusNot(
                    productId, storeId, CommentStatus.OCULTO, pageable);
        } else if (productId != null) {
            result = commentRepository.findByProductIdAndStatusNot(productId, CommentStatus.OCULTO, pageable);
        } else {
            result = commentRepository.findByStoreIdAndStatusNot(storeId, CommentStatus.OCULTO, pageable);
        }
        return new PagedModel<>(result.map(CommentResponse::from));
    }

    /**
     * Reporta un comentario (RF17). Un reporte por usuario y comentario: repetirlo no hace nada (204).
     * Un reporte nuevo pasa el comentario a REPORTADO.
     */
    @Transactional
    public void report(User reporter, Long commentId, ReportReason reason, String details) {
        Comment comment = commentRepository.findByIdAndStatusNot(commentId, CommentStatus.OCULTO)
                .orElseThrow(() -> new CommentNotFoundException(commentId));

        if (reason == ReportReason.OTRO && details == null) {
            throw new InvalidCommentRequestException("Describe el motivo cuando eliges 'Otro'.");
        }
        if (comment.getUser().getId().equals(reporter.getId())) {
            throw new InvalidCommentRequestException("No puedes reportar tu propio comentario.");
        }
        if (commentReportRepository.existsByCommentIdAndUserId(commentId, reporter.getId())) {
            return; // idempotente: ya reportó este comentario
        }

        try {
            commentReportRepository.saveAndFlush(CommentReport.of(comment, reporter, reason, details));
        } catch (DataIntegrityViolationException e) {
            // Dos clics simultáneos: gana la única de la BD. No se traga la excepción
            // (la transacción ya quedó marcada para rollback); se responde 409.
            throw new CommentNotAllowedException("Ya reportaste este comentario.");
        }
        comment.markAsReported();
    }
}
