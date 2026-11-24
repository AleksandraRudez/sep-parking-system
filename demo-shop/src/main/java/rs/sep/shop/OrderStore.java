package rs.sep.shop;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Narudzbine se cuvaju u memoriji; za test prodavca baza nije potrebna. */
@Component
public class OrderStore {

    private final Map<String, ShopOrder> orders = new ConcurrentHashMap<>();

    public ShopOrder save(ShopOrder order) {
        orders.put(order.getOrderId(), order);
        return order;
    }

    public Optional<ShopOrder> find(String orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }

    public List<ShopOrder> all() {
        return orders.values().stream().sorted(Comparator.comparing(ShopOrder::getCreatedAt).reversed()).toList();
    }
}
