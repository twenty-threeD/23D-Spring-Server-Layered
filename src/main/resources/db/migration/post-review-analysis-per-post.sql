-- 리뷰 분석 기준점을 전문가(reviewee)에서 게시글(post)로 바꾼 변경에 따른 스키마 정리.
--
-- 이 프로젝트는 ddl-auto: update로 스키마를 맞추는데, Hibernate는 이미 있는 컬럼과
-- 제약을 지우지 않는다. 그래서 기존 post_review_analysis에는 reviewee_id NOT NULL과
-- uk_post_review_analysis_reviewee가 그대로 남아 post_id 기반 insert가 전부 실패한다.
--
-- 분석 결과는 리뷰 원문으로 언제든 다시 만들 수 있는 캐시 성격이고, 기존 행이 어느
-- 게시글에 귀속되는지 알 수 없어 옮길 방법도 없다. 테이블을 지우고 애플리케이션이
-- 올라올 때 ddl-auto가 새 모양으로 다시 만들게 한다.
--
-- 배포 전에 한 번 실행한다(키워드 테이블이 analysis_id로 참조하므로 먼저 지운다).
drop table if exists post_review_analysis_keyword;
drop table if exists post_review_analysis;
