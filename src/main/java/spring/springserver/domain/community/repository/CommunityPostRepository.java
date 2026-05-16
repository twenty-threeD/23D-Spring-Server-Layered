package spring.springserver.domain.community.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import spring.springserver.domain.community.entity.CommunityPost;

import java.util.List;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {

    @Query("""
            select c
            from CommunityPost c
            left join c.member m
            where coalesce(lower(c.title), '') like lower(concat('%', :keyword, '%'))
               or coalesce(lower(c.username), '') like lower(concat('%', :keyword, '%'))
               or coalesce(lower(m.username), '') like lower(concat('%', :keyword, '%'))
            order by c.createdAt desc
            """)
    List<CommunityPost> searchPosts(@Param("keyword") String keyword);
}
