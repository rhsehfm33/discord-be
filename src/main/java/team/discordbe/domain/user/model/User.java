package team.discordbe.domain.user.model;

import jakarta.persistence.*;
import team.discordbe.global.base.BaseEntity;

@Entity
@Table(name= "USERS") //User로 하려했으나 H2-console과 예약어로 충돌이 남.
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

/*
    private String email;
    private String hashedPassword;
    friends, groups, createdAt, updatedAt
*/
    @Column(name = "nick_name", unique = true, nullable = false, length = 50)
    private String nickName;
}
