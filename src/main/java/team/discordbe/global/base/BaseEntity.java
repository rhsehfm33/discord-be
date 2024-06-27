package team.discordbe.global.base;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

@MappedSuperclass
@Getter
public abstract class BaseEntity {
    @Column(updatable = false, name = "created_at")
    private LocalDateTime createdAt;

    @Column(insertable = false, name = "updated_at")
    private LocalDateTime updatedAt;
}
