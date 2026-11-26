package com.comprasco.bakeprofit.auth.repository;

import java.time.OffsetDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.comprasco.bakeprofit.auth.entity.Role;
import com.comprasco.bakeprofit.auth.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndActiveTrue(String email);

    List<User> findByActiveTrue();

    List<User> findByActiveFalse();

    List<User> findByRole(Role role);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE users
            SET ai_queries_used = 0,
                ai_queries_reset_at = ai_queries_reset_at + INTERVAL '1 month'
            WHERE ai_queries_reset_at <= :now
            """, nativeQuery = true)
    int resetDueAiQuotas(@Param("now") OffsetDateTime now);

    /**
     * Incrementa en 1 el contador de consultas IA del usuario.
     * Se usa después de cada consulta exitosa al chat de IA.
     */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE users
            SET ai_queries_used = ai_queries_used + 1
            WHERE id = :userId
            """, nativeQuery = true)
    int incrementAiQueriesUsed(@Param("userId") Long userId);

    /**
     * Consume una consulta IA solo si el usuario no ha llegado al límite.
     * Es atómico: dos peticiones concurrentes no pueden pasarse del límite.
     *
     * @return 1 si se consumió, 0 si la cuota estaba agotada
     */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE users
            SET ai_queries_used = ai_queries_used + 1
            WHERE id = :userId AND ai_queries_used < :limit
            """, nativeQuery = true)
    int tryConsumeAiQuery(@Param("userId") Long userId, @Param("limit") int limit);

    /** Devuelve una consulta IA (cuando la llamada a la IA falló). */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE users
            SET ai_queries_used = GREATEST(ai_queries_used - 1, 0)
            WHERE id = :userId
            """, nativeQuery = true)
    int refundAiQuery(@Param("userId") Long userId);

    /** Lee el contador actual de consultas IA directamente de la BD. */
    @Query(value = "SELECT ai_queries_used FROM users WHERE id = :userId", nativeQuery = true)
    int findAiQueriesUsedById(@Param("userId") Long userId);

}