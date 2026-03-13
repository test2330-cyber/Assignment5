package org.example.Amazon;

import org.example.Amazon.Cost.DeliveryPrice;
import org.example.Amazon.Cost.ExtraCostForElectronics;
import org.example.Amazon.Cost.ItemType;
import org.example.Amazon.Cost.RegularCost;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration Tests for the Amazon package.
 * Tests how multiple components (Amazon, ShoppingCartAdaptor, Database, PriceRules) work together.
 */
public class AmazonIntegrationTest {

    private static Database database;
    private ShoppingCartAdaptor cart;
    private Amazon amazon;

    @BeforeAll
    static void setUpDatabase() {
        database = new Database();
    }

    @BeforeEach
    void setUp() {
        // Reset DB before each test to ensure a clean state
        database.resetDatabase();
        cart = new ShoppingCartAdaptor(database);
        amazon = new Amazon(cart, List.of(
                new RegularCost(),
                new DeliveryPrice(),
                new ExtraCostForElectronics()
        ));
    }

    @AfterAll
    static void tearDownDatabase() {
        database.close();
    }

    // ─────────────────────────────────────────────────────────────
    // Specification-Based Integration Tests
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("specification-based")
    void emptyCart_shouldReturnZeroTotal() {
        // Spec: An empty cart with all pricing rules applied should cost $0
        double total = amazon.calculate();
        assertThat(total).isEqualTo(0.0);
    }

    @Test
    @DisplayName("specification-based")
    void singleOtherItem_shouldApplyRegularCostAndDelivery() {
        // Spec: 1 OTHER item → RegularCost + $5 delivery (1–3 items tier), no electronics surcharge
        Item book = new Item(ItemType.OTHER, "Java Book", 1, 30.00);
        amazon.addToCart(book);

        double total = amazon.calculate();
        // 30.00 (regular) + 5.00 (delivery: 1 item) + 0 (no electronics) = 35.00
        assertThat(total).isEqualTo(35.00);
    }

    @Test
    @DisplayName("specification-based")
    void singleElectronicItem_shouldApplyElectronicsSurcharge() {
        // Spec: An ELECTRONIC item triggers the $7.50 surcharge
        Item laptop = new Item(ItemType.ELECTRONIC, "Laptop", 1, 500.00);
        amazon.addToCart(laptop);

        double total = amazon.calculate();
        // 500.00 (regular) + 5.00 (delivery) + 7.50 (electronics) = 512.50
        assertThat(total).isEqualTo(512.50);
    }

    @Test
    @DisplayName("specification-based")
    void cartWith4To10Items_shouldCharge12Dot50Delivery() {
        // Spec: 4–10 items → $12.50 delivery tier
        for (int i = 0; i < 4; i++) {
            amazon.addToCart(new Item(ItemType.OTHER, "Item " + i, 1, 10.00));
        }

        double total = amazon.calculate();
        // 40.00 (regular) + 12.50 (delivery) + 0 (no electronics) = 52.50
        assertThat(total).isEqualTo(52.50);
    }

    @Test
    @DisplayName("specification-based")
    void cartWithMoreThan10Items_shouldCharge20Delivery() {
        // Spec: >10 items → $20.00 delivery tier
        for (int i = 0; i < 11; i++) {
            amazon.addToCart(new Item(ItemType.OTHER, "Item " + i, 1, 5.00));
        }

        double total = amazon.calculate();
        // 55.00 (regular) + 20.00 (delivery) + 0 (no electronics) = 75.00
        assertThat(total).isEqualTo(75.00);
    }

    @Test
    @DisplayName("specification-based")
    void mixedCart_withElectronicAndOtherItems_shouldApplyAllRules() {
        // Spec: Mixed cart with electronics triggers all three pricing rules
        amazon.addToCart(new Item(ItemType.ELECTRONIC, "Headphones", 1, 100.00));
        amazon.addToCart(new Item(ItemType.OTHER, "Phone Case", 2, 15.00));

        double total = amazon.calculate();
        // (100 + 30) regular + 5.00 delivery (2 items) + 7.50 electronics = 142.50
        assertThat(total).isEqualTo(142.50);
    }

    @Test
    @DisplayName("specification-based")
    void itemQuantityGreaterThanOne_shouldMultiplyPriceCorrectly() {
        // Spec: Quantity multiplies the per-unit price in RegularCost
        Item item = new Item(ItemType.OTHER, "Notebook", 3, 10.00);
        amazon.addToCart(item);

        double total = amazon.calculate();
        // 3 * 10 = 30 (regular) + 5.00 (delivery: 1 item in cart) = 35.00
        assertThat(total).isEqualTo(35.00);
    }

    @Test
    @DisplayName("specification-based")
    void addToCart_shouldPersistItemInDatabase() {
        // Spec: addToCart stores item so it can be retrieved from DB
        Item item = new Item(ItemType.OTHER, "Pen", 1, 1.99);
        amazon.addToCart(item);

        List<Item> items = cart.getItems();
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getName()).isEqualTo("Pen");
        assertThat(items.get(0).getPricePerUnit()).isEqualTo(1.99);
    }

    // ─────────────────────────────────────────────────────────────
    // Structural-Based Integration Tests
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("structural-based")
    void calculate_iteratesAllPriceRules_andSumsResults() {
        // Structural: Amazon.calculate() loops over all rules and sums their return values
        amazon.addToCart(new Item(ItemType.ELECTRONIC, "Tablet", 1, 200.00));

        // RegularCost = 200, DeliveryPrice = 5, ExtraCostForElectronics = 7.5
        double total = amazon.calculate();
        assertThat(total).isEqualTo(212.50);
    }

    @Test
    @DisplayName("structural-based")
    void databaseReset_clearsPreviousData() {
        // Structural: resetDatabase() clears all rows so each test starts fresh
        amazon.addToCart(new Item(ItemType.OTHER, "Book", 1, 20.00));
        assertThat(cart.getItems()).hasSize(1);

        database.resetDatabase();
        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    @DisplayName("structural-based")
    void multipleElectronicsInCart_shouldOnlyChargeSurchargeOnce() {
        // Structural: ExtraCostForElectronics uses anyMatch — surcharge is flat $7.50 regardless of count
        amazon.addToCart(new Item(ItemType.ELECTRONIC, "Phone", 1, 300.00));
        amazon.addToCart(new Item(ItemType.ELECTRONIC, "Tablet", 1, 400.00));

        double total = amazon.calculate();
        // 700 (regular) + 5.00 (delivery: 2 items) + 7.50 (electronics, only once) = 712.50
        assertThat(total).isEqualTo(712.50);
    }

    @Test
    @DisplayName("structural-based")
    void shoppingCartAdaptor_getItems_returnsAllStoredItems() {
        // Structural: getItems() executes SELECT * and maps all rows back to Item objects
        amazon.addToCart(new Item(ItemType.OTHER, "Eraser", 2, 0.50));
        amazon.addToCart(new Item(ItemType.ELECTRONIC, "USB Hub", 1, 25.00));

        List<Item> items = cart.getItems();
        assertThat(items).hasSize(2);

        boolean hasEraser = items.stream().anyMatch(i -> i.getName().equals("Eraser"));
        boolean hasUsb = items.stream().anyMatch(i -> i.getName().equals("USB Hub"));
        assertThat(hasEraser).isTrue();
        assertThat(hasUsb).isTrue();
    }

    @Test
    @DisplayName("structural-based")
    void deliveryPrice_boundaryAt3Items_returns5Dollars() {
        // Structural: DeliveryPrice boundary — exactly 3 items stays in the 1–3 tier ($5)
        amazon.addToCart(new Item(ItemType.OTHER, "A", 1, 1.00));
        amazon.addToCart(new Item(ItemType.OTHER, "B", 1, 1.00));
        amazon.addToCart(new Item(ItemType.OTHER, "C", 1, 1.00));

        double total = amazon.calculate();
        // 3.00 regular + 5.00 delivery (exactly 3) = 8.00
        assertThat(total).isEqualTo(8.00);
    }

    @Test
    @DisplayName("structural-based")
    void deliveryPrice_boundaryAt4Items_returns12Dot50() {
        // Structural: DeliveryPrice boundary — exactly 4 items jumps to the 4–10 tier ($12.50)
        for (int i = 0; i < 4; i++) {
            amazon.addToCart(new Item(ItemType.OTHER, "Item" + i, 1, 1.00));
        }

        double total = amazon.calculate();
        // 4.00 regular + 12.50 delivery = 16.50
        assertThat(total).isEqualTo(16.50);
    }
}
