package com.comprasco.bakeprofit.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comprasco.bakeprofit.dto.RatingSummaryResponse;
import com.comprasco.bakeprofit.dto.ReviewCommentResponse;
import com.comprasco.bakeprofit.dto.ReviewResponse;
import com.comprasco.bakeprofit.entity.Comment;
import com.comprasco.bakeprofit.entity.CommentReport;
import com.comprasco.bakeprofit.entity.CommentStatus;
import com.comprasco.bakeprofit.entity.Product;
import com.comprasco.bakeprofit.entity.Rating;
import com.comprasco.bakeprofit.entity.ReportReason;
import com.comprasco.bakeprofit.entity.Role;
import com.comprasco.bakeprofit.entity.Store;
import com.comprasco.bakeprofit.entity.User;
import com.comprasco.bakeprofit.exception.ForbiddenActionException;
import com.comprasco.bakeprofit.exception.InvalidReviewRequestException;
import com.comprasco.bakeprofit.exception.PriceNotFoundException;
import com.comprasco.bakeprofit.exception.ReviewAlreadyExistsException;
import com.comprasco.bakeprofit.exception.ReviewNotAllowedException;
import com.comprasco.bakeprofit.exception.ReviewNotFoundException;
import com.comprasco.bakeprofit.repository.CommentReportRepository;
import com.comprasco.bakeprofit.repository.CommentRepository;
import com.comprasco.bakeprofit.repository.PriceRepository;
import com.comprasco.bakeprofit.repository.RatingRepository;
import com.comprasco.bakeprofit.repository.RatingStats;

/**
 * Orquesta la acción "calificar y comentar" (RN-30) sobre dos entidades independientes:
 * Rating (calificación) y Comment (comentario). El id de una opinión es el de su calificación.
 */
@Service
@Transactional(readOnly = true)
public class ReviewService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final List<CommentStatus> VISIBLE_STATUSES =
            List.of(CommentStatus.PUBLICADO, CommentStatus.REPORTADO, CommentStatus.CONSERVADO);

    private final RatingRepository ratingRepository;
    private final CommentRepository commentRepository;
    private final CommentReportRepository commentReportRepository;
    private final PriceRepository priceRepository;
    private final ProductService productService;
    private final StoreService storeService;
    private final long editWindowHours;

    public ReviewService(RatingRepository ratingRepository,
                         CommentRepository commentRepository,
                         CommentReportRepository commentReportRepository,
                         PriceRepository priceRepository,
                         ProductService productService,
                         StoreService storeService,
                         @Value("${app.reviews.edit-window-hours:24}") long editWindowHours) {
        this.ratingRepository = ratingRepository;
        this.commentRepository = commentRepository;
        this.commentReportRepository = commentReportRepository;
        this.priceRepository = priceRepository;
        this.productService = productService;
        this.storeService = storeService;
        this.editWindowHours = editWindowHours;
    }

    /**
     * Crea la calificación y el comentario en la misma transacción: o se guardan los dos o ninguno (RN-30).
     * Orden: rol (403) -> existencia (404) -> activos (409) -> precio del par (404) -> duplicado (409).
     */
    @Transactional
    public ReviewResponse create(User user, Long productId, Long storeId, Integer score, String text) {
        if (user.getRole() != Role.USER) {
            throw new ForbiddenActionException("Solo los usuarios pueden calificar y comentar.");
        }
        Product product = productService.findById(productId);
        Store store = storeService.findById(storeId);

        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new ReviewNotAllowedException(
                    "No se puede opinar sobre el producto '" + product.getName() + "' porque está inactivo.");
        }
        if (!Boolean.TRUE.equals(store.getActive())) {
            throw new ReviewNotAllowedException(
                    "No se puede opinar en la tienda '" + store.getName() + "' porque está inactiva.");
        }
        if (!priceRepository.existsByProductIdAndStoreId(productId, storeId)) {
            throw new PriceNotFoundException(
                    "El producto '" + product.getName() + "' no tiene precio registrado en la tienda '"
                    + store.getName() + "'.");
        }
        if (ratingRepository.existsByUserIdAndProductIdAndStoreId(user.getId(), productId, storeId)) {
            throw new ReviewAlreadyExistsException(product.getName(), store.getName());
        }

        try {
            Rating rating = ratingRepository.saveAndFlush(Rating.of(user, product, store, score));
            Comment comment = commentRepository.saveAndFlush(Comment.of(rating, text));
            return toResponse(rating, comment);
        } catch (DataIntegrityViolationException e) {
            // Dos peticiones simultáneas pasaron la pre-comprobación: gana la única de la BD
            throw new ReviewAlreadyExistsException(product.getName(), store.getName());
        }
    }

    /**
     * Edita calificación y comentario juntos. Solo el autor, dentro del plazo desde el registro,
     * y solo si el comentario sigue PUBLICADO (uno reportado no se puede reescribir).
     */
    @Transactional
    public ReviewResponse update(User user, Long reviewId, Integer score, String text) {
        Rating rating = findOwnedRating(user, reviewId);
        Comment comment = commentOf(rating);
        if (comment.getStatus() != CommentStatus.PUBLICADO) {
            throw new ReviewNotAllowedException(
                    "No se puede editar: el comentario fue reportado, ocultado o eliminado.");
        }
        if (!LocalDateTime.now().isBefore(editDeadline(rating))) {
            throw new ReviewNotAllowedException(
                    "El plazo de edición de " + editWindowHours + " horas ya terminó.");
        }
        rating.updateScore(score);
        comment.updateText(text);
        return toResponse(rating, comment);
    }

    /** Elimina el comentario (borrado lógico) en cualquier momento. La calificación se conserva. Repetirlo no hace nada. */
    @Transactional
    public void deleteComment(User user, Long reviewId) {
        commentOf(findOwnedRating(user, reviewId)).delete();
    }

    /**
     * Resumen (RF15). Con productId y storeId: producto en esa tienda.
     * Solo productId: producto en todas las tiendas. Solo storeId: derivado de la tienda.
     * Consulta solo calificaciones; no toca la tabla de comentarios.
     */
    public RatingSummaryResponse getSummary(Long productId, Long storeId) {
        requireAtLeastOneFilter(productId, storeId);
        RatingStats stats;
        if (productId != null && storeId != null) {
            stats = ratingRepository.statsByProductAndStore(productId, storeId);
        } else if (productId != null) {
            stats = ratingRepository.statsByProduct(productId);
        } else {
            stats = ratingRepository.statsByStore(storeId);
        }
        return RatingSummaryResponse.of(stats.getAverage(), stats.getTotal());
    }

    /** Opinión del usuario para ese par; vacío si aún no opinó (el controller responde 204). */
    public Optional<ReviewResponse> findMine(User user, Long productId, Long storeId) {
        productService.findById(productId);
        storeService.findById(storeId);
        return ratingRepository
                .findByUserIdAndProductIdAndStoreId(user.getId(), productId, storeId)
                .map(rating -> toResponse(rating, commentOf(rating)));
    }

    /** Opiniones con comentario visible (no ocultas ni eliminadas), de la más reciente a la más antigua. */
    public PagedModel<ReviewCommentResponse> list(Long productId, Long storeId, int page, int size) {
        requireAtLeastOneFilter(productId, storeId);

        // El cliente no elige el orden: un sort libre permitiría ordenar por columnas internas
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Order.desc("registrationDate"), Sort.Order.desc("id")));

        Page<Comment> result;
        if (productId != null && storeId != null) {
            result = commentRepository.findByRatingProductIdAndRatingStoreIdAndStatusIn(
                    productId, storeId, VISIBLE_STATUSES, pageable);
        } else if (productId != null) {
            result = commentRepository.findByRatingProductIdAndStatusIn(productId, VISIBLE_STATUSES, pageable);
        } else {
            result = commentRepository.findByRatingStoreIdAndStatusIn(storeId, VISIBLE_STATUSES, pageable);
        }
        return new PagedModel<>(result.map(ReviewCommentResponse::from));
    }

    /**
     * Reporta el comentario de una opinión (RF17). Un reporte por usuario y comentario: repetirlo no hace nada.
     * Un reporte nuevo pasa el comentario a REPORTADO.
     */
    @Transactional
    public void report(User reporter, Long reviewId, ReportReason reason, String details) {
        Comment comment = commentRepository.findByRatingIdAndStatusIn(reviewId, VISIBLE_STATUSES)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        if (reason == ReportReason.OTRO && details == null) {
            throw new InvalidReviewRequestException("Describe la razón cuando eliges 'Otro'.");
        }
        if (comment.getRating().isOwnedBy(reporter.getId())) {
            throw new InvalidReviewRequestException("No puedes reportar tu propio comentario.");
        }
        if (commentReportRepository.existsByCommentIdAndUserId(comment.getId(), reporter.getId())) {
            return; // idempotente: ya reportó este comentario
        }

        try {
            commentReportRepository.saveAndFlush(CommentReport.of(comment, reporter, reason, details));
        } catch (DataIntegrityViolationException e) {
            // Dos clics simultáneos: gana la única de la BD. No se traga la excepción
            // (la transacción ya quedó marcada para rollback); se responde 409.
            throw new ReviewNotAllowedException("Ya reportaste este comentario.");
        }
        comment.markAsReported();
    }

    // ---------- auxiliares ----------

    private void requireAtLeastOneFilter(Long productId, Long storeId) {
        if (productId == null && storeId == null) {
            throw new InvalidReviewRequestException("Debes indicar productId, storeId o ambos.");
        }
        if (productId != null) productService.findById(productId);
        if (storeId != null) storeService.findById(storeId);
    }

    private Rating findOwnedRating(User user, Long reviewId) {
        Rating rating = ratingRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));
        if (!rating.isOwnedBy(user.getId())) {
            throw new ForbiddenActionException("Solo el autor puede modificar su opinión.");
        }
        return rating;
    }

    // Toda calificación nace con su comentario (create es la única vía); si falta, es una inconsistencia de datos
    private Comment commentOf(Rating rating) {
        return commentRepository.findByRatingId(rating.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "La calificación " + rating.getId() + " no tiene comentario asociado."));
    }

    private LocalDateTime editDeadline(Rating rating) {
        return rating.getRegistrationDate().plusHours(editWindowHours);
    }

    private ReviewResponse toResponse(Rating rating, Comment comment) {
        LocalDateTime deadline = editDeadline(rating);
        boolean editable = comment.getStatus() == CommentStatus.PUBLICADO
                && LocalDateTime.now().isBefore(deadline);
        return ReviewResponse.from(rating, comment, editable ? deadline : null);
    }
}
