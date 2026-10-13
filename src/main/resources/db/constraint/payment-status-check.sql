-- payment.status CHECK 제약 갱신.
--
-- Hibernate 6은 EnumType.STRING 컬럼에 허용 값 목록을 CHECK 제약으로 만든다.
-- ddl-auto: update는 이미 있는 제약을 고치지 않으므로, PaymentStatus에 값을 추가하면
-- 새 값으로 저장할 때 제약 위반이 난다. 아래 문장을 운영 DB에서 직접 한 번 실행한다.
--   psql "$DB_URL" -f src/main/resources/db/constraint/payment-status-check.sql
--
-- PaymentStatus를 고치면 이 파일도 같이 고쳐야 한다.

ALTER TABLE payment DROP CONSTRAINT IF EXISTS payment_status_check;

ALTER TABLE payment ADD CONSTRAINT payment_status_check CHECK (status IN (
    'READY',
    'IN_PROGRESS',
    'DONE',
    'CHAIN_RECORDED',
    'CANCELED_BY_CHAIN_FAILURE',
    'CANCEL_PENDING',
    'CANCEL_FAILED',
    'ABANDONED',
    'PARTIAL_CANCELED',
    'CANCELED'
));
