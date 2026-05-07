package spring.springserver.domain.community.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import spring.springserver.domain.community.entity.CommunityComment;

public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Long> {
}
