package com.dmg.movieticketing.notification.application;

import com.dmg.movieticketing.booking.application.BookingConfirmedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringJUnitConfig(BookingNotificationTransactionTest.TestConfiguration.class)
class BookingNotificationTransactionTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void resetService() {
        reset(notificationService);
    }

    @Test
    void invokesListenerOnlyAfterTransactionCommits() {
        BookingConfirmedEvent event = confirmedEvent();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        transaction.executeWithoutResult(status -> {
            eventPublisher.publishEvent(event);
            verifyNoInteractions(notificationService);
        });

        verify(notificationService).send(event);
    }

    @Test
    void skipsListenerWhenTransactionRollsBack() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        transaction.executeWithoutResult(status -> {
            eventPublisher.publishEvent(confirmedEvent());
            status.setRollbackOnly();
        });

        verifyNoInteractions(notificationService);
    }

    @Test
    void skipsListenerWithoutTransactionBecauseFallbackIsDisabled() {
        eventPublisher.publishEvent(confirmedEvent());

        verifyNoInteractions(notificationService);
    }

    private BookingConfirmedEvent confirmedEvent() {
        return new BookingConfirmedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2026-09-27T10:00:00Z"),
                new BigDecimal("250.00"),
                "INR",
                1
        );
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    static class TestConfiguration {

        @Bean
        NotificationService notificationService() {
            return mock(NotificationService.class);
        }

        @Bean
        BookingNotificationListener bookingNotificationListener(NotificationService notificationService) {
            return new BookingNotificationListener(notificationService);
        }

        @Bean
        PlatformTransactionManager transactionManager() {
            return new InMemoryTransactionManager();
        }
    }

    private static final class InMemoryTransactionManager extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
