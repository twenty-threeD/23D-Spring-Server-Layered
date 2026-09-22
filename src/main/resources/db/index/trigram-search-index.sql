-- LIKE '%키워드%' 검색용 pg_trgm GIN 인덱스.
--
-- 선행 와일드카드가 붙은 LIKE는 B-tree를 타지 못해 매번 전체 스캔이 된다.
-- GIN + gin_trgm_ops 인덱스는 이런 패턴에서도 동작한다.
--
-- 이 프로젝트는 ddl-auto: update로 스키마를 맞추는데 Hibernate는 B-tree 인덱스만
-- 만들 수 있으므로, 아래 문장은 운영 DB에서 직접 한 번 실행한다.
--   psql "$DB_URL" -f src/main/resources/db/index/trigram-search-index.sql
--
-- 인덱스 표현식은 JPQL이 만들어내는 조건식과 문자 단위로 일치해야 쓰인다.
-- 검색 쿼리를 고치면 이 파일도 같이 고쳐야 한다.

CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- PostRepository.searchPostsByTitle / searchPostsByTitleAndCategoryIds
--   조건식: lower(p.title) like lower('%키워드%')
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_post_title_trgm
    ON post USING gin (lower(title) gin_trgm_ops);

-- CommunityJobPostRepository.searchJobPostIds
--   조건식: coalesce(lower(p.title), '') like ... (content, username도 같은 형태)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_community_job_post_title_trgm
    ON community_job_post USING gin (coalesce(lower(title), '') gin_trgm_ops);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_community_job_post_content_trgm
    ON community_job_post USING gin (coalesce(lower(content), '') gin_trgm_ops);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_community_job_post_username_trgm
    ON community_job_post USING gin (coalesce(lower(username), '') gin_trgm_ops);
