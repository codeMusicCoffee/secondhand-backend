package com.example.secondhand.service;

import com.example.secondhand.entity.TimeoutTask;
import com.example.secondhand.repository.TimeoutTaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 超时任务调度器测试
 */
@ExtendWith(MockitoExtension.class)
class TimeoutTaskSchedulerTest {

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private TimeoutTaskRepository timeoutTaskRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private TimeoutTaskScheduler timeoutTaskScheduler;

    @Test
    void testScheduleTimeout_Success() {
        // Given
        String orderNo = "ORD20231217001";
        TimeoutTask.TaskType taskType = TimeoutTask.TaskType.ORDER_TIMEOUT;
        int timeoutMinutes = 15;

        when(timeoutTaskRepository.existsByOrderNoAndTaskType(orderNo, taskType)).thenReturn(false);
        when(timeoutTaskRepository.save(any(TimeoutTask.class))).thenAnswer(invocation -> {
            TimeoutTask task = invocation.getArgument(0);
            task.setTaskId("TASK_" + System.currentTimeMillis());
            return task;
        });
        when(taskScheduler.schedule(any(Runnable.class), any(java.util.Date.class))).thenReturn(mock(java.util.concurrent.ScheduledFuture.class));

        // When
        String taskId = timeoutTaskScheduler.scheduleTimeout(orderNo, taskType, timeoutMinutes);

        // Then
        assertNotNull(taskId);
        verify(timeoutTaskRepository).existsByOrderNoAndTaskType(orderNo, taskType);
        verify(timeoutTaskRepository).save(any(TimeoutTask.class));
        verify(taskScheduler).schedule(any(Runnable.class), any(java.util.Date.class));
    }

    @Test
    void testScheduleTimeout_TaskAlreadyExists() {
        // Given
        String orderNo = "ORD20231217001";
        TimeoutTask.TaskType taskType = TimeoutTask.TaskType.ORDER_TIMEOUT;
        int timeoutMinutes = 15;

        when(timeoutTaskRepository.existsByOrderNoAndTaskType(orderNo, taskType)).thenReturn(true);

        // When
        String taskId = timeoutTaskScheduler.scheduleTimeout(orderNo, taskType, timeoutMinutes);

        // Then
        assertNull(taskId);
        verify(timeoutTaskRepository).existsByOrderNoAndTaskType(orderNo, taskType);
        verify(timeoutTaskRepository, never()).save(any(TimeoutTask.class));
        verify(taskScheduler, never()).schedule(any(Runnable.class), any(java.util.Date.class));
    }

    @Test
    void testCancelTimeout_Success() {
        // Given
        String taskId = "TASK_123";
        String reason = "订单已支付";

        TimeoutTask timeoutTask = new TimeoutTask("ORD20231217001", TimeoutTask.TaskType.ORDER_TIMEOUT, 15);
        timeoutTask.setTaskId(taskId);
        timeoutTask.setStatus(TimeoutTask.TaskStatus.SCHEDULED);

        when(timeoutTaskRepository.findById(taskId)).thenReturn(Optional.of(timeoutTask));
        when(timeoutTaskRepository.save(any(TimeoutTask.class))).thenReturn(timeoutTask);

        // When
        boolean result = timeoutTaskScheduler.cancelTimeout(taskId, reason);

        // Then
        assertTrue(result);
        assertEquals(TimeoutTask.TaskStatus.CANCELLED, timeoutTask.getStatus());
        assertEquals(reason, timeoutTask.getCancelReason());
        verify(timeoutTaskRepository).findById(taskId);
        verify(timeoutTaskRepository).save(timeoutTask);
    }

    @Test
    void testCancelTimeout_TaskNotFound() {
        // Given
        String taskId = "TASK_123";
        String reason = "订单已支付";

        when(timeoutTaskRepository.findById(taskId)).thenReturn(Optional.empty());

        // When
        boolean result = timeoutTaskScheduler.cancelTimeout(taskId, reason);

        // Then
        assertFalse(result);
        verify(timeoutTaskRepository).findById(taskId);
        verify(timeoutTaskRepository, never()).save(any(TimeoutTask.class));
    }

    @Test
    void testCancelTimeoutByOrder_Success() {
        // Given
        String orderNo = "ORD20231217001";
        TimeoutTask.TaskType taskType = TimeoutTask.TaskType.ORDER_TIMEOUT;
        String reason = "订单已支付";

        TimeoutTask timeoutTask = new TimeoutTask(orderNo, taskType, 15);
        timeoutTask.setTaskId("TASK_123");
        timeoutTask.setStatus(TimeoutTask.TaskStatus.SCHEDULED);

        when(timeoutTaskRepository.findActiveTasksByOrderNo(orderNo)).thenReturn(java.util.Arrays.asList(timeoutTask));
        when(timeoutTaskRepository.findById("TASK_123")).thenReturn(Optional.of(timeoutTask));
        when(timeoutTaskRepository.save(any(TimeoutTask.class))).thenReturn(timeoutTask);

        // When
        boolean result = timeoutTaskScheduler.cancelTimeoutByOrder(orderNo, taskType, reason);

        // Then
        assertTrue(result);
        verify(timeoutTaskRepository).findActiveTasksByOrderNo(orderNo);
    }

    @Test
    void testGetTaskStatistics() {
        // Given
        when(timeoutTaskRepository.countByStatus(TimeoutTask.TaskStatus.SCHEDULED)).thenReturn(5L);
        when(timeoutTaskRepository.countByStatus(TimeoutTask.TaskStatus.EXECUTING)).thenReturn(2L);
        when(timeoutTaskRepository.countByStatus(TimeoutTask.TaskStatus.EXECUTED)).thenReturn(10L);
        when(timeoutTaskRepository.countByStatus(TimeoutTask.TaskStatus.CANCELLED)).thenReturn(3L);
        when(timeoutTaskRepository.countByStatus(TimeoutTask.TaskStatus.FAILED)).thenReturn(1L);
        when(timeoutTaskRepository.countByStatus(TimeoutTask.TaskStatus.RETRY)).thenReturn(0L);

        // When
        TimeoutTaskScheduler.TaskStatistics stats = timeoutTaskScheduler.getTaskStatistics();

        // Then
        assertNotNull(stats);
        assertEquals(5L, stats.getCurrentScheduled());
        assertEquals(2L, stats.getCurrentExecuting());
        assertEquals(10L, stats.getCurrentExecuted());
        assertEquals(3L, stats.getCurrentCancelled());
        assertEquals(1L, stats.getCurrentFailed());
        assertEquals(0L, stats.getCurrentRetry());
    }

    /**
     * 创建测试用的超时任务
     */
    private TimeoutTask createTimeoutTask(String orderNo, TimeoutTask.TaskType taskType) {
        TimeoutTask task = new TimeoutTask(orderNo, taskType, 15);
        task.setTaskId("TASK_" + System.currentTimeMillis());
        task.setStatus(TimeoutTask.TaskStatus.EXECUTING);
        task.setExecuteTime(LocalDateTime.now());
        return task;
    }


}