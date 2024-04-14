package team.discordbe.domain.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import team.discordbe.domain.user.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

}
