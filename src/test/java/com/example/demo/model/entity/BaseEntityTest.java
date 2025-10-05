package com.example.demo.model.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class BaseEntityTest {

    private static class TestEntity extends BaseEntity {
        void triggerCreate() { onCreate(); }
        void triggerUpdate() { onUpdate(); }
    }

    @Test
    void onCreateInitializesCreatedAndUpdatedAtToSameInstant() {
        TestEntity e = new TestEntity();
        e.triggerCreate();
        assertThat(e.getCreatedAt()).isNotNull();
        assertThat(e.getUpdatedAt()).isNotNull();
        assertThat(e.getCreatedAt()).isEqualTo(e.getUpdatedAt());
    }

    @Test
    void onUpdateChangesOnlyUpdatedAtAndKeepsCreatedAt() throws InterruptedException {
        TestEntity e = new TestEntity();
        e.triggerCreate();
        Instant created = e.getCreatedAt();
        Instant firstUpdated = e.getUpdatedAt();
        Thread.sleep(2); // ensure clock moves forward a bit
        e.triggerUpdate();
        assertThat(e.getCreatedAt()).isEqualTo(created);
        assertThat(e.getUpdatedAt()).isAfter(firstUpdated);
    }

    @Test
    void multipleUpdatesAdvanceUpdatedAtMonotonically() throws InterruptedException {
        TestEntity e = new TestEntity();
        e.triggerCreate();
        Instant prev = e.getUpdatedAt();
        for (int i = 0; i < 3; i++) {
            Thread.sleep(2);
            e.triggerUpdate();
            assertThat(e.getUpdatedAt()).isAfter(prev);
            prev = e.getUpdatedAt();
        }
    }
}

