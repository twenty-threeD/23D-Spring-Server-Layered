-- payment.chain_check_attempt_count 컬럼 추가.
--
-- 체인 커밋 결과가 미확정(CHAIN_UNCONFIRMED)인 결제를 원장에서 몇 번 다시 확인했는지 센다.
-- 엔티티에서는 not null이지만 ddl-auto: update가 기존 행이 있는 테이블에 not null 컬럼을
-- 바로 붙이면 실패하므로, 배포 전에 기본값과 함께 먼저 만들어 둔다.
--   psql "$DB_URL" -f src/main/resources/db/migration/payment-chain-check-attempt-count.sql

ALTER TABLE payment
    ADD COLUMN IF NOT EXISTS chain_check_attempt_count integer NOT NULL DEFAULT 0;
