package io.github.elr.blog.common.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.elr.blog.TestcontainersConfiguration;
import io.github.elr.blog.config.JpaAuditingConfig;
import io.github.elr.blog.testsupport.MutableClock;
import jakarta.persistence.OptimisticLockException;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class, BaseEntityIT.ClockConfig.class})
class BaseEntityIT {

    static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

    @TestConfiguration(proxyBeanMethods = false)
    static class ClockConfig {
        @Bean
        MutableClock clock() {
            return new MutableClock(T0);
        }
    }

    @Autowired
    TestEntityManager em;

    @Autowired
    MutableClock clock;

    @BeforeEach
    void resetClock() {
        clock.set(T0);
    }

    @Test
    void persist_assigns_a_uuid_v7_and_audit_timestamps() {
        AuditedWidget widget = em.persistFlushFind(new AuditedWidget("first"));
        assertThat(widget.getId()).isNotNull();
        assertThat(widget.getId().version()).isEqualTo(7);
        assertThat(widget.getCreatedAt()).isEqualTo(T0);
        assertThat(widget.getUpdatedAt()).isEqualTo(T0);
        assertThat(widget.getVersion()).isZero();
    }

    @Test
    void update_bumps_version_and_updated_at_but_not_created_at() {
        AuditedWidget widget = em.persistFlushFind(new AuditedWidget("first"));
        clock.advance(Duration.ofMinutes(5));
        widget.rename("second");
        em.flush();
        assertThat(widget.getVersion()).isEqualTo(1);
        assertThat(widget.getCreatedAt()).isEqualTo(T0);
        assertThat(widget.getUpdatedAt()).isEqualTo(T0.plus(Duration.ofMinutes(5)));
    }

    @Test
    void a_stale_update_is_rejected_instead_of_overwriting_a_newer_one() {
        AuditedWidget widget = em.persistFlushFind(new AuditedWidget("first"));
        // Someone else updates the row in the meantime, which bumps its version to 1...
        em.getEntityManager()
                .createNativeQuery(
                        "UPDATE test_audited_widgets SET name = 'theirs', version = version + 1 WHERE id = :id")
                .setParameter("id", widget.getId())
                .executeUpdate();
        widget.rename("ours");
        // ...so our UPDATE ... WHERE version = 0 matches no row, and Hibernate refuses to overwrite theirs.
        assertThatThrownBy(em::flush).isInstanceOf(OptimisticLockException.class);
    }

    @Test
    void ids_are_time_ordered() {
        AuditedWidget a = em.persistAndFlush(new AuditedWidget("a"));
        AuditedWidget b = em.persistAndFlush(new AuditedWidget("b"));
        assertThat(a.getId().toString()).isLessThan(b.getId().toString());
    }

    @Test
    void equality_is_by_id_and_survives_reloading() {
        AuditedWidget saved = em.persistFlushFind(new AuditedWidget("x"));
        em.clear();
        AuditedWidget reloaded = em.find(AuditedWidget.class, saved.getId());
        assertThat(reloaded).isEqualTo(saved).hasSameHashCodeAs(saved).isNotSameAs(saved);
        assertThat(new AuditedWidget("y")).isNotEqualTo(new AuditedWidget("y")); // unsaved: no id yet
    }
}
