package com.b0ss.crm.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.seed-demo", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private static final String[][] COMPANIES = {
            {"Chan Ka Ming", "Golden Harbour Trading Ltd", "chan.km@example.com", "+852 9123 4501"},
            {"Wong Mei Ling", "Victoria Accounting Services", "wong.ml@example.com", "+852 9123 4502"},
            {"Lee Chun Ho", "Kowloon Auto Parts", "lee.ch@example.com", "+852 9123 4503"},
            {"Ho Wing Yan", "Peak Digital Solutions", "ho.wy@example.com", "+852 9123 4504"},
            {"Lam Tsz Kit", "Harbourview Logistics", "lam.tk@example.com", "+852 9123 4505"},
            {"Cheung Siu Fung", "New Territories Foods", "cheung.sf@example.com", "+852 9123 4506"},
            {"Ng Ka Yee", "Bright Star Education", "ng.ky@example.com", "+852 9123 4507"},
            {"Tsang Wai Man", "Diamond Electrical", "tsang.wm@example.com", "+852 9123 4508"},
            {"Yeung Hoi Ching", "Silver Bay Property", "yeung.hc@example.com", "+852 9123 4509"},
            {"Lau Kin Pong", "Central Legal Chambers", "lau.kp@example.com", "+852 9123 4510"},
            {"Kwok Sin Yee", "Jade Medical Clinic", "kwok.sy@example.com", "+852 9123 4511"},
            {"Tang Chi Wai", "Star Ferry Catering", "tang.cw@example.com", "+852 9123 4512"},
            {"Fung Pui Shan", "Tsim Sha Tsui Hotels Group", "fung.ps@example.com", "+852 9123 4513"},
            {"Ip Ho Yin", "Green Valley Recycling", "ip.hy@example.com", "+852 9123 4514"},
            {"Choi Man Kit", "Metro Print and Design", "choi.mk@example.com", "+852 9123 4515"},
            {"So Lai Na", "Ocean Park Souvenirs", "so.ln@example.com", "+852 9123 4516"},
            {"Ma Tsz Hin", "Skyline Construction", "ma.th@example.com", "+852 9123 4517"},
            {"Chu Wing Sze", "Harmony Beauty Salon", "chu.ws@example.com", "+852 9123 4518"},
            {"Yip Ka Lok", "Canton Trading Co", "yip.kl@example.com", "+852 9123 4519"},
            {"Shek Hiu Tung", "Blue Mountain Coffee", "shek.ht@example.com", "+852 9123 4520"}
    };

    private static final String[] FOLLOW_UP_NOTES = {
            "Initial discovery call. Interested in the annual support plan.",
            "Sent quotation. Waiting for internal approval.",
            "Followed up by phone; asked to call back next week.",
            "Demo completed. Team raised questions about data migration.",
            "Contract signed. Handover scheduled with operations.",
            "Requested a discount for a two-year commitment.",
            "Referred us to their sister company in Kwun Tong.",
            "Meeting postponed due to typhoon signal number eight.",
            "Shared case study and pricing sheet by email.",
            "Renewal discussion started; current plan expires in two months."
    };

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Integer existing = jdbcTemplate.queryForObject("select count(*) from users", Integer.class);
        if (existing != null && existing > 0) {
            log.info("Demo seed skipped: users table already has data");
            return;
        }

        String hash = passwordEncoder.encode("Passw0rd!");
        Long adminId = jdbcTemplate.queryForObject("""
                insert into users (username, password, display_name, role)
                values (?, ?, ?, ?) returning id
                """, Long.class, "admin", hash, "Admin User", "ADMIN");
        Long salesId = jdbcTemplate.queryForObject("""
                insert into users (username, password, display_name, role)
                values (?, ?, ?, ?) returning id
                """, Long.class, "sales", hash, "Sam Lee", "SALES");

        List<Long> customerIds = new ArrayList<>();
        for (int i = 0; i < COMPANIES.length; i++) {
            String[] company = COMPANIES[i];
            int daysAgo = i * 5;
            Long id = jdbcTemplate.queryForObject("""
                    insert into customers
                        (name, phone, email, company, note, created_by, created_at, updated_at)
                    values (?, ?, ?, ?, ?, ?, now() - make_interval(days => cast(? as int)),
                            now() - make_interval(days => cast(? as int)))
                    returning id
                    """, Long.class,
                    company[0], company[3], company[2], company[1],
                    "Imported from the demo dataset", salesId, daysAgo, daysAgo);
            customerIds.add(id);
        }

        for (int i = 0; i < 30; i++) {
            Long customerId = customerIds.get(i % customerIds.size());
            int daysAgo = (i * 3) % 90;
            jdbcTemplate.update("""
                    insert into follow_ups (customer_id, content, created_by, created_at)
                    values (?, ?, ?, now() - make_interval(days => cast(? as int)))
                    """, customerId, FOLLOW_UP_NOTES[i % FOLLOW_UP_NOTES.length], salesId, daysAgo);
        }

        log.info("Demo seed complete: admin id={}, sales id={}, {} customers, 30 follow-ups",
                adminId, salesId, customerIds.size());
    }
}
