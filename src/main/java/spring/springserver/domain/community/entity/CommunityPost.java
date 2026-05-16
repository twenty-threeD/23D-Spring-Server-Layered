package spring.springserver.domain.community.entity;

import jakarta.persistence.*;
import jdk.jfr.Timestamp;
import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import spring.springserver.domain.member.entity.Member;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "community_post")
public class CommunityPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "member_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;

    private String username;

    @Column(length = 255, nullable = false)
    private String title;

    @Column(length = 2000)
    private String content;

    @Column(length = 500)
    private String fileUrl;

    private int viewCount;

    private Boolean isEdited;

    @Timestamp
    private LocalDateTime isDeleted;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
