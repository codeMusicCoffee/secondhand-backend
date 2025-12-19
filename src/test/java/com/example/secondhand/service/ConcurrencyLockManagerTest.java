package com.example.secondhand.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConcurrencyLockManagerTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private ConcurrencyLockManager lockManager;

    @BeforeEach
    void setUp() {
        // Setup will be done in individual tests as needed
    }

    @Test
    void testAcquireLock_Success() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);

        // When
        String lockValue = lockManager.acquireLock("test-key");

        // Then
        assertNotNull(lockValue);
        verify(valueOperations).setIfAbsent(eq("inventory_lock:test-key"), anyString(), eq(30L), eq(TimeUnit.SECONDS));
    }

    @Test
    void testAcquireLock_Failed() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(false);

        // When
        String lockValue = lockManager.acquireLock("test-key");

        // Then
        assertNull(lockValue);
        verify(valueOperations).setIfAbsent(eq("inventory_lock:test-key"), anyString(), eq(30L), eq(TimeUnit.SECONDS));
    }

    @Test
    void testAcquireLockWithTimeout() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);

        // When
        String lockValue = lockManager.acquireLock("test-key", Duration.ofSeconds(60));

        // Then
        assertNotNull(lockValue);
        verify(valueOperations).setIfAbsent(eq("inventory_lock:test-key"), anyString(), eq(60L), eq(TimeUnit.SECONDS));
    }

    @Test
    void testIsLocked_True() {
        // Given
        when(redisTemplate.hasKey("inventory_lock:test-key")).thenReturn(true);

        // When
        boolean isLocked = lockManager.isLocked("test-key");

        // Then
        assertTrue(isLocked);
        verify(redisTemplate).hasKey("inventory_lock:test-key");
    }

    @Test
    void testIsLocked_False() {
        // Given
        when(redisTemplate.hasKey("inventory_lock:test-key")).thenReturn(false);

        // When
        boolean isLocked = lockManager.isLocked("test-key");

        // Then
        assertFalse(isLocked);
        verify(redisTemplate).hasKey("inventory_lock:test-key");
    }

    @Test
    void testGetProductLockKey() {
        // When
        String lockKey = ConcurrencyLockManager.getProductLockKey(123L);

        // Then
        assertEquals("product:123", lockKey);
    }

    @Test
    void testGetOrderLockKey() {
        // When
        String lockKey = ConcurrencyLockManager.getOrderLockKey("ORDER123");

        // Then
        assertEquals("order:ORDER123", lockKey);
    }
}