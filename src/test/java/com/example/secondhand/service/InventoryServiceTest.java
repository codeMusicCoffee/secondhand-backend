package com.example.secondhand.service;

import com.example.secondhand.entity.OrderItem;
import com.example.secondhand.entity.Product;
import com.example.secondhand.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ConcurrencyLockManager lockManager;

    @InjectMocks
    private InventoryService inventoryService;

    private Product testProduct;
    private OrderItem testOrderItem;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("测试商品");
        testProduct.setQuantity(10);
        testProduct.setStatus(1);

        testOrderItem = new OrderItem();
        testOrderItem.setProductId(1L);
        testOrderItem.setQuantity(2);
    }

    @Test
    void testCheckInventory_SufficientStock() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // When
        boolean result = inventoryService.checkInventory(1L, 5);

        // Then
        assertTrue(result);
        verify(productRepository).findById(1L);
    }

    @Test
    void testCheckInventory_InsufficientStock() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // When
        boolean result = inventoryService.checkInventory(1L, 15);

        // Then
        assertFalse(result);
        verify(productRepository).findById(1L);
    }

    @Test
    void testCheckInventory_ProductNotFound() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        // When
        boolean result = inventoryService.checkInventory(1L, 5);

        // Then
        assertFalse(result);
        verify(productRepository).findById(1L);
    }

    @Test
    void testGetCurrentInventory() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // When
        Integer inventory = inventoryService.getCurrentInventory(1L);

        // Then
        assertEquals(10, inventory);
        verify(productRepository).findById(1L);
    }

    @Test
    void testGetCurrentInventory_ProductNotFound() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        // When
        Integer inventory = inventoryService.getCurrentInventory(1L);

        // Then
        assertEquals(0, inventory);
        verify(productRepository).findById(1L);
    }
}