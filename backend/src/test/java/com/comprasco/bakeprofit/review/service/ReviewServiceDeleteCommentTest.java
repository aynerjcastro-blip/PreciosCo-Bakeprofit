package com.comprasco.bakeprofit.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.comprasco.bakeprofit.auth.entity.User;
import com.comprasco.bakeprofit.auth.exception.ForbiddenActionException;
import com.comprasco.bakeprofit.catalog.service.ProductService;
import com.comprasco.bakeprofit.price.repository.PriceRepository;
import com.comprasco.bakeprofit.review.entity.Comment;
import com.comprasco.bakeprofit.review.entity.CommentStatus;
import com.comprasco.bakeprofit.review.entity.Rating;
import com.comprasco.bakeprofit.review.exception.ReviewNotAllowedException;
import com.comprasco.bakeprofit.review.exception.ReviewNotFoundException;
import com.comprasco.bakeprofit.review.repository.CommentReportRepository;
import com.comprasco.bakeprofit.review.repository.CommentRepository;
import com.comprasco.bakeprofit.review.repository.RatingRepository;
import com.comprasco.bakeprofit.store.service.StoreService;

/**
 * Eliminación del comentario por su autor (SCRUM-151).
 * Orden de las comprobaciones: 404 (no existe) -> 403 (no es el autor) -> 409 (OCULTO por moderación).
 */
@ExtendWith(MockitoExtension.class)
class ReviewServiceDeleteCommentTest {

    private static final Long REVIEW_ID = 10L;
    private static final Long AUTHOR_ID = 1L;

    @Mock private RatingRepository ratingRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private CommentReportRepository commentReportRepository;
    @Mock private PriceRepository priceRepository;
    @Mock private ProductService productService;
    @Mock private StoreService storeService;

    private ReviewService service;
    private User author;
    private Rating rating;

    @BeforeEach
    void setUp() {
        service = new ReviewService(ratingRepository, commentRepository, commentReportRepository,
                priceRepository, productService, storeService, 24);
        author = userWithId(AUTHOR_ID);
        rating = mock(Rating.class);
    }

    @Test
    void ocultoRespondeConflictoYSigueOculto() {
        Comment comment = existingComment(CommentStatus.OCULTO);

        assertThatThrownBy(() -> service.deleteComment(author, REVIEW_ID))
                .isInstanceOf(ReviewNotAllowedException.class)   // 409 en GlobalExceptionHandler
                .hasMessageContaining("ocultó");
        assertThat(comment.getStatus()).isEqualTo(CommentStatus.OCULTO);
    }

    @ParameterizedTest
    @EnumSource(value = CommentStatus.class, names = { "PUBLICADO", "REPORTADO", "CONSERVADO" })
    void visibleQuedaEliminado(CommentStatus status) {
        Comment comment = existingComment(status);

        service.deleteComment(author, REVIEW_ID);

        assertThat(comment.getStatus()).isEqualTo(CommentStatus.ELIMINADO);
    }

    @Test
    void eliminadoRepetidoNoFallaNiCambia() {
        Comment comment = existingComment(CommentStatus.ELIMINADO);

        service.deleteComment(author, REVIEW_ID);

        assertThat(comment.getStatus()).isEqualTo(CommentStatus.ELIMINADO);
    }

    @Test
    void otroUsuarioRecibeProhibidoAunqueEsteOculto() {
        // El 403 va antes que el 409: quien no es el autor no se entera del estado del comentario
        when(ratingRepository.findById(REVIEW_ID)).thenReturn(Optional.of(rating));
        when(rating.isOwnedBy(2L)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteComment(userWithId(2L), REVIEW_ID))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void opinionInexistenteRespondeNoEncontrada() {
        when(ratingRepository.findById(REVIEW_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteComment(author, REVIEW_ID))
                .isInstanceOf(ReviewNotFoundException.class);
    }

    /** Opinión del autor con su comentario en el estado indicado. La moderación es manual, por eso el estado se fija por reflexión. */
    private Comment existingComment(CommentStatus status) {
        when(ratingRepository.findById(REVIEW_ID)).thenReturn(Optional.of(rating));
        when(rating.isOwnedBy(AUTHOR_ID)).thenReturn(true);
        when(rating.getId()).thenReturn(REVIEW_ID);
        Comment comment = Comment.of(rating, "Comentario de prueba suficientemente largo");
        ReflectionTestUtils.setField(comment, "status", status);
        when(commentRepository.findByRatingId(REVIEW_ID)).thenReturn(Optional.of(comment));
        return comment;
    }

    private static User userWithId(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }
}
