package com.comprasco.bakeprofit.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.comprasco.bakeprofit.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor 
@Table(name = "password_reset_tokens")

public class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    //*Relacion con el usuario que solicito el restablecimiento de la contraseña.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn (name = "user_id", nullable = false)
    private User user;

    @Column (nullable = false)
    private LocalDateTime expiryDate;

    public PasswordResetToken(User user){
        this.user = user;
        //*Almacena un String que tendra siempre 36 caracteres con numeros pseudoaleatorios */
        this.token = UUID.randomUUID().toString();
        //*HU-25: El enlace deja de servir a los 30 minutos */
        this.expiryDate = LocalDateTime.now().plusMinutes(30);
    }

    public boolean isExpired(){
        return LocalDateTime.now().isAfter(this.expiryDate);
    }

    
}
