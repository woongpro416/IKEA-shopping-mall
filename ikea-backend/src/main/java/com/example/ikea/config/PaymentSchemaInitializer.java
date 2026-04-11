package com.example.ikea.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        String constraintDefinition = jdbcTemplate.query(
                """
                        SELECT pg_get_constraintdef(oid)
                        FROM pg_constraint
                        WHERE conname = 'payments_payment_method_check'
                          AND conrelid = 'payments'::regclass
                        """,
                rs -> rs.next() ? rs.getString(1) : null
        );

        if (constraintDefinition == null || !constraintDefinition.contains("BANK_TRANSFER")) {
            jdbcTemplate.execute("ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_payment_method_check");
            jdbcTemplate.execute(
                    """
                            ALTER TABLE payments
                            ADD CONSTRAINT payments_payment_method_check
                            CHECK (payment_method IN ('TOSS', 'KAKAO', 'BANK_TRANSFER'))
                            """
            );

            log.info("payments.payment_method 체크 제약을 BANK_TRANSFER 포함 기준으로 갱신했습니다.");
        }

        int backfilledPaymentCount = jdbcTemplate.update(
                """
                        INSERT INTO payments (
                            order_id,
                            member_id,
                            payment_method,
                            amount,
                            payment_status,
                            response_data,
                            created_at,
                            paid_at
                        )
                        SELECT
                            o.order_id,
                            o.member_id,
                            'BANK_TRANSFER',
                            COALESCE(o.final_price, o.total_price, 0),
                            'OK',
                            'LEGACY_BANK_TRANSFER_BACKFILL',
                            CURRENT_TIMESTAMP,
                            CURRENT_TIMESTAMP
                        FROM orders o
                        LEFT JOIN payments p ON p.order_id = o.order_id
                        WHERE p.order_id IS NULL
                          AND o.order_status IN ('PAID', 'ORDERED', 'DELIVERING', 'COMPLETED')
                        """
        );

        if (backfilledPaymentCount > 0) {
            log.info("기존 무통장 입금 누락 결제 {}건을 보정했습니다.", backfilledPaymentCount);
        }
    }
}
