package com.warp.exchange.service;

import com.warp.exchange.entity.trade.OrderEntity;
import com.warp.exchange.enums.AssetEnum;
import com.warp.exchange.enums.Direction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class OrderService {
    final AssetService assetService;
    
    final ConcurrentMap<Long, OrderEntity> activeOrders = new ConcurrentHashMap<>();  // 跟踪所有活动订单
    
    final ConcurrentMap<Long, ConcurrentHashMap<Long, OrderEntity>> userOrders = new ConcurrentHashMap<>();    // 跟踪用户活动订单

    public OrderService(@Autowired AssetService assetService) {
        this.assetService = assetService;
    }
    
    public OrderEntity getOrder(Long orderId) {
        return this.activeOrders.get(orderId);
    }
    
    public ConcurrentMap<Long, OrderEntity> getActiveOrders() {
        return this.activeOrders;
    }
    
    public ConcurrentMap<Long, OrderEntity> getUserOrders(Long userId) {
        return this.userOrders.get(userId);
    }

    /**
     * 创建订单
     */
    public OrderEntity createOrder(long sequenceId, long timeStamp, Long orderId, Long userId, Direction direction, BigDecimal price, BigDecimal quantity) {
        switch (direction) {
            case BUY -> {
                // 买入，需冻结USD：
                if (!assetService.tryFreeze(userId, AssetEnum.USD, price.multiply(quantity))) {
                    return null;
                }
            }
            case SELL -> {
                // 卖出，需冻结BTC：
                if (!assetService.tryFreeze(userId, AssetEnum.BTC, quantity)) {
                    return null;
                }
            }
            default -> throw new IllegalArgumentException("Invalid direction.");
        }
        OrderEntity order = new OrderEntity();
        order.id = orderId;
        order.userId = userId;
        order.sequenceId = sequenceId;
        order.price = price;
        order.direction = direction;
        order.quantity = quantity;
        order.unfilledQuantity = quantity;
        order.createTime = order.updateTime = timeStamp;
        
        this.activeOrders.put(order.id, order);
        ConcurrentHashMap<Long, OrderEntity> tmpUserOrders = this.userOrders.get(userId);
        if (tmpUserOrders == null) {
            tmpUserOrders = new ConcurrentHashMap<>();
            userOrders.put(userId, tmpUserOrders);
        }
        tmpUserOrders.put(order.id, order);
        return order;
    }

    /**
     * 删除订单
     */
    public void removeOrder(long orderId) {
        OrderEntity removed = this.activeOrders.remove(orderId);
        if (removed == null) {
            throw new IllegalArgumentException("OrderEntity not found by orderId in active orders: " + orderId);
        }
        ConcurrentHashMap<Long, OrderEntity> tmpUserOrders = this.userOrders.get(removed.userId);
        if (tmpUserOrders == null) {
            throw new IllegalArgumentException("User orders not found by userId: " + removed.userId);
        }
        if (tmpUserOrders.remove(orderId) == null) {
            throw new IllegalArgumentException("Order not found by orderId in user orders: " + orderId);
        }
    }
    
    public void debug() {
        System.out.println("---------- orders ----------");
        List<OrderEntity> orders = new ArrayList<>(this.activeOrders.values());
        Collections.sort(orders);
        for (OrderEntity order : orders) {
            System.out.println("  " + order.id + " " + order.direction + " price: " + order.price + " unfilled: "
                    + order.unfilledQuantity + " quantity: " + order.quantity + " sequenceId: " + order.sequenceId
                    + " userId: " + order.userId);
        }
        System.out.println("---------- // orders ----------");
    }
}
