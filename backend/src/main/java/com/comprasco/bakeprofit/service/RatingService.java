package com.comprasco.bakeprofit.service;

import com.comprasco.bakeprofit.dto.RatingRequest;
import com.comprasco.bakeprofit.dto.RatingSummaryResponse;
import com.comprasco.bakeprofit.entity.*;
import com.comprasco.bakeprofit.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RatingService {

    private final RatingRepository ratingRepository;
    private final CommentRepository commentRepository;
    private final PriceRepository priceRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;

    public RatingService(RatingRepository ratingRepository, CommentRepository commentRepository,
                         PriceRepository priceRepository, UserRepository userRepository,
                         ProductRepository productRepository, StoreRepository storeRepository) {
        this.ratingRepository = ratingRepository;
        this.commentRepository = commentRepository;
        this.priceRepository = priceRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
    }

    @Transactional
    public void createRating(Long userId, RatingRequest request) {
        // 1. Validar que exista al menos un precio registrado para el par (producto, tienda)
        if (!priceRepository.existsByProductIdAndStoreId(request.getProductId(), request.getStoreId())) {
            throw new IllegalArgumentException("No se puede opinar sobre un producto/tienda sin precio registrado.");
        }

        // 2. Validar que el usuario no haya opinado previamente sobre este par
        if (ratingRepository.existsByUserIdAndProductIdAndStoreId(userId, request.getProductId(), request.getStoreId())) {
            throw new IllegalStateException("Ya has registrado una opinión para este producto en esta tienda.");
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        Product product = productRepository.findById(request.getProductId()).orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        Store store = storeRepository.findById(request.getStoreId()).orElseThrow(() -> new IllegalArgumentException("Tienda no encontrada"));

        // 3. Crear Rating
        Rating rating = new Rating();
        rating.setUser(user);
        rating.setProduct(product);
        rating.setStore(store);
        rating.setScore(request.getScore());
        rating = ratingRepository.save(rating);

        // 4. Crear Comment (Inserción en pareja estricta)
        Comment comment = new Comment();
        comment.setRating(rating);
        comment.setText(request.getComment());
        comment.setStatus(Comment.CommentStatus.PUBLICADO);
        commentRepository.save(comment);
    }

    public RatingSummaryResponse getSummary(Long productId, Long storeId) {
        Object[] result = (Object[]) ratingRepository.getSummaryByProductAndStore(productId, storeId);
        if (result == null || result.length == 0 || result[0] == null) {
            return new RatingSummaryResponse(0.0, 0L);
        }
        Double avg = (Double) result[0];
        Long total = (Long) result[1];
        return new RatingSummaryResponse(avg, total);
    }
}