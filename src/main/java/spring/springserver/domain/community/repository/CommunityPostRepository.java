package spring.springserver.domain.community.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import spring.springserver.domain.community.entity.CommunityPost;

import java.util.List;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {

    @Query("""
            select c from CommunityPost c
            where c.title like concat('%', :title, '%')
            """)
    List<CommunityPost> findByTitleLike(@Param("title") String title);

    @Query("""
            select c from CommunityPost c
            where c.username like concat('%', :username, '%')
            """)
    List<CommunityPost> findByUsernameLike(@Param("username") String username);
}
