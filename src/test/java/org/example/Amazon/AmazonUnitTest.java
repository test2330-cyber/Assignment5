package org.example.Amazon;

import org.example.Amazon.Cost.*;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for the Amazon package.
 * Each class is tested in isolation. External dependencies (ShoppingCart, Database) are mocked.
 */
public class AmazonUnitTest {

    // ─────────────────────────────────────────────────────────────
    // Amazon class unit tests
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("specification-based")
    class AmazonSpecificationTests {

        private ShoppingCart mockCart;
        private Amazon amazon;

        @BeforeEach
        void setUp() {
            mockCart = Mockito.mock(ShoppingCart.class);
        }

        @Test
        @DisplayName("specification-based")
        void calculate_withNoRules_returnsZero() {
            // Spec: No pricing rules → total is always 0
            when(mockCart.getItems()).thenReturn(List.of());
            amazon = new Amazon(mockCart, List.of());

            assertThat(amazon.calculate()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("specification-based")
        void calculate_withSingleRule_returnsRuleResult() {
            // Spec: calculate() delegates to each rule and sums up
            Item item = new Item(ItemType.OTHER, "Book", 1, 50.00);
            when(mockCart.getItems()).thenReturn(List.of(item));

            PriceRule mockRule = Mockito.mock(PriceRule.class);
            when(mockRule.priceToAggregate(anyList())).thenReturn(50.0);

            amazon = new Amazon(mockCart, List.of(mockRule));
            assertThat(amazon.calculate()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("specification-based")
        void calculate_withMultipleRules_sumsAllRules() {
            // Spec: Multiple rules are each called and their results summed
            when(mockCart.getItems()).thenReturn(List.of());

            PriceRule rule1 = Mockito.mock(PriceRule.class);
            PriceRule rule2 = Mockito.mock(PriceRule.class);
            when(rule1.priceToAggregate(anyList())).thenReturn(10.0);
            when(rule2.priceToAggregate(anyList())).thenReturn(5.0);

            amazon = new Amazon(mockCart, List.of(rule1, rule2));
            assertThat(amazon.calculate()).isEqualTo(15.0);
        }

        @Test
        @DisplayName("specification-based")
        void addToCart_delegatesToCart() {
            // Spec: addToCart() should call cart.add() with the provided item
            amazon = new Amazon(mockCart, List.of());
            Item item = new Item(ItemType.OTHER, "Pen", 1, 2.00);

            amazon.addToCart(item);

            verify(mockCart, times(1)).add(item);
        }
    }

    @Nested
    @DisplayName("structural-based")
    class AmazonStructuralTests {

        @Test
        @DisplayName("structural-based")
        void calculate_callsGetItemsOnCart_exactlyOncePerRule() {
            // Structural: Each PriceRule.priceToAggregate() receives the list from cart.getItems()
            ShoppingCart mockCart = Mockito.mock(ShoppingCart.class);
            List<Item> items = List.of(new Item(ItemType.OTHER, "X", 1, 1.0));
            when(mockCart.getItems()).thenReturn(items);

            PriceRule rule1 = Mockito.mock(PriceRule.class);
            PriceRule rule2 = Mockito.mock(PriceRule.class);
            when(rule1.priceToAggregate(items)).thenReturn(1.0);
            when(rule2.priceToAggregate(items)).thenReturn(2.0);

            Amazon amazon = new Amazon(mockCart, List.of(rule1, rule2));
            amazon.calculate();

            // getItems() is called once per rule iteration
            verify(mockCart, times(2)).getItems();
        }

        @Test
        @DisplayName("structural-based")
        void addToCart_doesNotCallCalculate() {
            // Structural: addToCart() only delegates add — never triggers calculate logic
            ShoppingCart mockCart = Mockito.mock(ShoppingCart.class);
            PriceRule mockRule = Mockito.mock(PriceRule.class);
            Amazon amazon = new Amazon(mockCart, List.of(mockRule));

            amazon.addToCart(new Item(ItemType.OTHER, "Y", 1, 5.0));

            verify(mockRule, never()).priceToAggregate(anyList());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // RegularCost unit tests
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("specification-based")
    class RegularCostSpecificationTests {

        private final RegularCost rule = new RegularCost();

        @Test
        @DisplayName("specification-based")
        void emptyCart_returnsZero() {
            assertThat(rule.priceToAggregate(List.of())).isEqualTo(0.0);
        }

        @Test
        @DisplayName("specification-based")
        void singleItem_returnsQuantityTimesUnitPrice() {
            Item item = new Item(ItemType.OTHER, "Book", 3, 10.00);
            assertThat(rule.priceToAggregate(List.of(item))).isEqualTo(30.00);
        }

        @Test
        @DisplayName("specification-based")
        void multipleItems_returnsSumOfAllPrices() {
            List<Item> items = List.of(
                    new Item(ItemType.OTHER, "Pen", 2, 1.50),
                    new Item(ItemType.ELECTRONIC, "Mouse", 1, 25.00)
            );
            // 3.00 + 25.00 = 28.00
            assertThat(rule.priceToAggregate(items)).isEqualTo(28.00);
        }

        @Test
        @DisplayName("specification-based")
        void electronicItems_areIncludedInRegularCost() {
            // Spec: RegularCost doesn't discriminate by type
            Item e = new Item(ItemType.ELECTRONIC, "Keyboard", 1, 80.00);
            assertThat(rule.priceToAggregate(List.of(e))).isEqualTo(80.00);
        }
    }

    @Nested
    @DisplayName("structural-based")
    class RegularCostStructuralTests {

        private final RegularCost rule = new RegularCost();

        @Test
        @DisplayName("structural-based")
        void loopsOverAllItems_notJustFirst() {
            // Structural: the for-loop in RegularCost must visit every item
            List<Item> items = List.of(
                    new Item(ItemType.OTHER, "A", 1, 5.00),
                    new Item(ItemType.OTHER, "B", 1, 7.00),
                    new Item(ItemType.OTHER, "C", 1, 3.00)
            );
            assertThat(rule.priceToAggregate(items)).isEqualTo(15.00);
        }

        @Test
        @DisplayName("structural-based")
        void zeroUnitPrice_producesZeroContribution() {
            Item free = new Item(ItemType.OTHER, "Freebie", 5, 0.00);
            assertThat(rule.priceToAggregate(List.of(free))).isEqualTo(0.00);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // DeliveryPrice unit tests
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("specification-based")
    class DeliveryPriceSpecificationTests {

        private final DeliveryPrice rule = new DeliveryPrice();

        @Test
        @DisplayName("specification-based")
        void emptyCart_returnsZeroDelivery() {
            assertThat(rule.priceToAggregate(List.of())).isEqualTo(0.0);
        }

        @Test
        @DisplayName("specification-based")
        void oneItem_returns5Dollars() {
            List<Item> items = List.of(new Item(ItemType.OTHER, "A", 1, 1.0));
            assertThat(rule.priceToAggregate(items)).isEqualTo(5.0);
        }

        @Test
        @DisplayName("specification-based")
        void threeItems_returns5Dollars() {
            List<Item> items = buildItemList(3);
            assertThat(rule.priceToAggregate(items)).isEqualTo(5.0);
        }

        @Test
        @DisplayName("specification-based")
        void fourItems_returns12Dot50() {
            List<Item> items = buildItemList(4);
            assertThat(rule.priceToAggregate(items)).isEqualTo(12.5);
        }

        @Test
        @DisplayName("specification-based")
        void tenItems_returns12Dot50() {
            List<Item> items = buildItemList(10);
            assertThat(rule.priceToAggregate(items)).isEqualTo(12.5);
        }

        @Test
        @DisplayName("specification-based")
        void elevenItems_returns20Dollars() {
            List<Item> items = buildItemList(11);
            assertThat(rule.priceToAggregate(items)).isEqualTo(20.0);
        }
    }

    @Nested
    @DisplayName("structural-based")
    class DeliveryPriceStructuralTests {

        private final DeliveryPrice rule = new DeliveryPrice();

        @Test
        @DisplayName("structural-based")
        void boundary_exactlyZeroItems_returnsZero() {
            // Structural: first if-branch (totalItems == 0) → 0
            assertThat(rule.priceToAggregate(List.of())).isEqualTo(0.0);
        }

        @Test
        @DisplayName("structural-based")
        void boundary_exactlyOneItem_entersFirstTier() {
            // Structural: second branch (>= 1 && <= 3) → 5.0
            assertThat(rule.priceToAggregate(buildItemList(1))).isEqualTo(5.0);
        }

        @Test
        @DisplayName("structural-based")
        void boundary_exactlyFourItems_entersSecondTier() {
            // Structural: third branch (>= 4 && <= 10) → 12.5
            assertThat(rule.priceToAggregate(buildItemList(4))).isEqualTo(12.5);
        }

        @Test
        @DisplayName("structural-based")
        void boundary_exactlyElevenItems_entersFinalTier() {
            // Structural: falls through all if-blocks → 20.0
            assertThat(rule.priceToAggregate(buildItemList(11))).isEqualTo(20.0);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ExtraCostForElectronics unit tests
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("specification-based")
    class ExtraCostSpecificationTests {

        private final ExtraCostForElectronics rule = new ExtraCostForElectronics();

        @Test
        @DisplayName("specification-based")
        void cartWithNoElectronics_returnsZero() {
            List<Item> items = List.of(new Item(ItemType.OTHER, "Book", 1, 10.0));
            assertThat(rule.priceToAggregate(items)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("specification-based")
        void cartWithOneElectronic_returns7Dot50() {
            List<Item> items = List.of(new Item(ItemType.ELECTRONIC, "Laptop", 1, 800.0));
            assertThat(rule.priceToAggregate(items)).isEqualTo(7.50);
        }

        @Test
        @DisplayName("specification-based")
        void cartWithMixedItems_containingElectronic_returns7Dot50() {
            List<Item> items = List.of(
                    new Item(ItemType.OTHER, "Bag", 1, 20.0),
                    new Item(ItemType.ELECTRONIC, "Headset", 1, 60.0)
            );
            assertThat(rule.priceToAggregate(items)).isEqualTo(7.50);
        }

        @Test
        @DisplayName("specification-based")
        void emptyCart_returnsZero() {
            assertThat(rule.priceToAggregate(List.of())).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("structural-based")
    class ExtraCostStructuralTests {

        private final ExtraCostForElectronics rule = new ExtraCostForElectronics();

        @Test
        @DisplayName("structural-based")
        void multipleElectronics_stillReturns7Dot50_notMultiplied() {
            // Structural: anyMatch short-circuits — surcharge is flat regardless of count
            List<Item> items = List.of(
                    new Item(ItemType.ELECTRONIC, "Phone", 1, 300.0),
                    new Item(ItemType.ELECTRONIC, "Tablet", 1, 400.0)
            );
            assertThat(rule.priceToAggregate(items)).isEqualTo(7.50);
        }

        @Test
        @DisplayName("structural-based")
        void onlyOtherTypes_noSurcharge() {
            // Structural: anyMatch returns false when no ELECTRONIC item exists
            List<Item> items = List.of(
                    new Item(ItemType.OTHER, "Shirt", 2, 15.0),
                    new Item(ItemType.OTHER, "Pants", 1, 30.0)
            );
            assertThat(rule.priceToAggregate(items)).isEqualTo(0.0);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Item unit tests
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("specification-based")
    class ItemSpecificationTests {

        @Test
        @DisplayName("specification-based")
        void item_storesAllFieldsCorrectly() {
            Item item = new Item(ItemType.ELECTRONIC, "Camera", 2, 299.99);
            assertThat(item.getType()).isEqualTo(ItemType.ELECTRONIC);
            assertThat(item.getName()).isEqualTo("Camera");
            assertThat(item.getQuantity()).isEqualTo(2);
            assertThat(item.getPricePerUnit()).isEqualTo(299.99);
        }

        @Test
        @DisplayName("specification-based")
        void item_withOtherType_returnsOtherType() {
            Item item = new Item(ItemType.OTHER, "Eraser", 1, 0.99);
            assertThat(item.getType()).isEqualTo(ItemType.OTHER);
        }
    }

    @Nested
    @DisplayName("structural-based")
    class ItemStructuralTests {

        @Test
        @DisplayName("structural-based")
        void item_fieldsAreImmutable_gettersReturnConstructorValues() {
            // Structural: Item has final fields — getters always return constructor values
            Item item = new Item(ItemType.OTHER, "Pencil", 5, 0.50);
            assertThat(item.getName()).isEqualTo("Pencil");
            assertThat(item.getQuantity()).isEqualTo(5);
            assertThat(item.getPricePerUnit()).isEqualTo(0.50);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────

    private List<Item> buildItemList(int count) {
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            items.add(new Item(ItemType.OTHER, "Item" + i, 1, 1.0));
        }
        return items;
    }
}
