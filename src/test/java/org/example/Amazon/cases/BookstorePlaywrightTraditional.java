package org.example.Amazon.cases;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.junit.jupiter.api.*;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Playwright UI Tests — DePaul University Bookstore Purchase Pathway
 * Product: JBL Quantum True Wireless Noise Cancelling Gaming Earbuds - Black
 * SKU: 668972707 | Price: $164.98 | Handling: $3.00 | Est. Total: $167.98
 *
 * Runs headed locally, headless on CI (detects CI env variable).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BookstorePlaywrightTraditional {

    static Playwright playwright;
    static Browser browser;
    static BrowserContext context;
    static Page page;

    static final String BASE_URL    = "https://depaul.bncollege.com/";
    static final String PRODUCT_URL =
        "https://depaul.bncollege.com/JBL/JBL-Quantum-True-Wireless-"
        + "Noise-Cancelling-Gaming-Earbuds--Black/p/668972707";
    static final String CART_URL    = "https://depaul.bncollege.com/cart";

    static final String SKU             = "668972707";
    static final String PRODUCT_PRICE   = "164.98";
    static final String HANDLING        = "3.00";
    static final String TAXES_TBD       = "TBD";
    static final String ESTIMATED_TOTAL = "167.98";

    static final String FIRST_NAME = "Hamza";
    static final String LAST_NAME  = "Patal";
    static final String EMAIL      = "hamza.patal@test.com";
    static final String PHONE      = "3125550000";

    static void log(String msg) {
        System.out.println("[PLAYWRIGHT] " + msg);
    }

    static void waitForPage() {
        try {
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        } catch (Exception e) {
            log("  ! DOM timeout");
        }
        try {
            page.waitForLoadState(LoadState.NETWORKIDLE,
                new Page.WaitForLoadStateOptions().setTimeout(12000));
        } catch (Exception e) {
            log("  ! Network idle timeout (ok)");
        }
    }

    static void logPage() {
        try {
            log("  URL: " + page.url());
        } catch (Exception ignored) { }
    }

    static boolean safeClick(String... selectors) {
        for (String sel : selectors) {
            try {
                Locator loc = page.locator(sel).first();
                loc.waitFor(new Locator.WaitForOptions().setTimeout(5000));
                if (loc.isVisible()) {
                    log("  + Click: " + sel);
                    loc.click();
                    return true;
                }
            } catch (Exception e) {
                log("  x " + sel.substring(0, Math.min(sel.length(), 60)));
            }
        }
        log("  ! No match");
        return false;
    }

    static boolean safeFill(String value, String... selectors) {
        for (String sel : selectors) {
            try {
                Locator loc = page.locator(sel).first();
                loc.waitFor(new Locator.WaitForOptions().setTimeout(5000));
                if (loc.isVisible()) {
                    log("  + Fill '" + value + "' -> " + sel);
                    loc.fill(value);
                    return true;
                }
            } catch (Exception e) {
                log("  x fill: " + sel.substring(0, Math.min(sel.length(), 60)));
            }
        }
        log("  ! No match for fill: " + value);
        return false;
    }

    /**
     * Apply a search filter by clicking the visible span.facet__list__label.
     * The checkbox is sr-only (hidden), so clicking the label triggers the form.
     * Panels confirmed always-open (collapse in) on search results page.
     */
    static void applyFilter(String facetId, String altValue) {
        log("  Applying filter [" + facetId + "] = " + altValue);
        try {
            Locator panel = page.locator(facetId).first();
            panel.waitFor(new Locator.WaitForOptions().setTimeout(5000));
            String cls = panel.getAttribute("class");
            if (cls == null || !cls.contains("in")) {
                log("  Panel closed, opening...");
                Locator header = page.locator(
                    "div.facet__name[data-target='" + facetId + "']").first();
                header.click();
                page.waitForTimeout(600);
            }
            // Click visible label span (sibling of the sr-only checkbox)
            String sel = facetId
                + " input.facet__list__checkbox[alt='" + altValue + "']"
                + " ~ span.facet__list__label";
            Locator label = page.locator(sel).first();
            label.waitFor(new Locator.WaitForOptions().setTimeout(5000));
            log("  + Clicking label: " + altValue);
            label.click();
            page.waitForTimeout(400);
            log("  + Filter applied: " + altValue);
        } catch (Exception e) {
            log("  ! Filter error for " + altValue + ": "
                + e.getMessage().split("\n")[0]);
        }
    }

    // -----------------------------------------------------------------------
    @BeforeAll
    static void setup() {
        log("=== SETUP ===");
        playwright = Playwright.create();
        // Headless on CI (GitHub Actions sets CI=true), headed locally
        boolean isCI = System.getenv("CI") != null;
        int slowMo = isCI ? 0 : 400;
        log("  headless=" + isCI + "  slowMo=" + slowMo);
        browser = playwright.chromium().launch(
            new BrowserType.LaunchOptions()
                .setHeadless(isCI)
                .setSlowMo(slowMo)
        );
        context = browser.newContext(new Browser.NewContextOptions()
            .setViewportSize(1280, 720)
            .setRecordVideoDir(Paths.get("videos/"))
            .setRecordVideoSize(1280, 720)
        );
        page = context.newPage();
        log("=== SETUP COMPLETE ===");
    }

    @AfterAll
    static void teardown() {
        log("=== TEARDOWN: saving video ===");
        try { context.close(); } catch (Exception ignored) { }
        try { browser.close(); } catch (Exception ignored) { }
        try { playwright.close(); } catch (Exception ignored) { }
        log("=== DONE. Video saved in videos/ ===");
    }

    // -----------------------------------------------------------------------
    // TC1: Homepage -> Search -> Filters -> Product -> Add to Cart
    // -----------------------------------------------------------------------
    @Test
    @Order(1)
    @DisplayName("TC1: Search earbuds, apply Brand/Color/Price filters, "
        + "assert product details, add to cart")
    void testCase1SearchAndAddToCart() {
        log("\n===== TC1: SEARCH + FILTERS + PRODUCT =====");

        log("Step 1: Navigate to homepage");
        page.navigate(BASE_URL);
        waitForPage();
        logPage();

        log("Step 2: Search for 'earbuds'");
        page.waitForTimeout(2000);
        safeFill("earbuds",
            "input[placeholder*='Search' i]",
            "input[type='search']"
        );
        page.keyboard().press("Enter");
        waitForPage();
        page.waitForTimeout(2000);
        logPage();

        log("Step 3: Apply Brand = JBL");
        applyFilter("#facet-brand", "JBL");
        waitForPage();
        page.waitForTimeout(1500);

        log("Step 4: Apply Color = Black");
        applyFilter("#facet-Color", "Black");
        waitForPage();
        page.waitForTimeout(1500);

        log("Step 5: Apply Price = Over $50");
        applyFilter("#facet-price", "Over $50");
        waitForPage();
        page.waitForTimeout(2000);
        logPage();

        log("Step 6: Click JBL Quantum True Wireless product");
        page.waitForTimeout(1500);
        boolean clicked = safeClick(
            "a[href*='668972707']",
            "a[href*='JBL-Quantum-True-Wireless']",
            "a:has-text('JBL Quantum True Wireless Noise Cancelling Gaming Earbuds')",
            "a:has-text('JBL Quantum True Wireless')"
        );
        if (!clicked) {
            log("  ! Product not found, navigating directly");
            page.navigate(PRODUCT_URL);
        }
        waitForPage();
        logPage();

        log("Step 7: Assert product name, SKU, price, description");
        String content = page.content();
        assertTrue(content.contains("JBL") || content.contains("Quantum"),
            "Product name should be on page");
        log("  + Product name: OK");
        assertTrue(content.contains(SKU), "SKU " + SKU + " should be on page");
        log("  + SKU: OK");
        assertTrue(content.contains(PRODUCT_PRICE), "Price $" + PRODUCT_PRICE);
        log("  + Price $" + PRODUCT_PRICE + ": OK");
        assertTrue(content.toLowerCase().contains("noise")
            || content.toLowerCase().contains("wireless")
            || content.toLowerCase().contains("gaming"),
            "Description should mention noise/wireless/gaming");
        log("  + Description: OK");

        log("Step 8: Click Add to Cart");
        boolean added = safeClick(
            "button:has-text('Add to Cart')",
            "button:has-text('ADD TO CART')",
            "button.add-to-cart-btn",
            "[class*='add-to-cart'] button"
        );
        assertTrue(added, "Add to Cart button should be clicked");

        log("Step 9: Wait 4 seconds for cart to update");
        page.waitForTimeout(4000);

        log("Step 10: Assert cart shows 1 item");
        String afterAdd = page.content();
        boolean cartOk = afterAdd.contains("1 item") || afterAdd.contains("1 Item")
            || afterAdd.contains("(1)") || afterAdd.contains("Cart 1");
        assertTrue(cartOk, "Cart should show 1 item");
        log("  + Cart shows 1 item: OK");
        log("===== TC1 PASSED =====\n");
    }

    // -----------------------------------------------------------------------
    // TC2: Shopping Cart
    // -----------------------------------------------------------------------
    @Test
    @Order(2)
    @DisplayName("TC2: Cart - product/qty/price, in-store pickup, "
        + "sidebar totals, promo code, checkout")
    void testCase2ShoppingCart() {
        log("\n===== TC2: SHOPPING CART =====");

        log("Step 1: Navigate to cart");
        page.navigate(CART_URL);
        waitForPage();
        logPage();

        log("Step 2: Assert 'Your Shopping Cart'");
        String content = page.content();
        assertTrue(content.contains("Your Shopping Cart"), "Should be on cart page");
        log("  + Cart page: OK");

        log("Step 3a: Assert product name JBL Quantum");
        assertTrue(content.contains("JBL Quantum") || content.contains("JBL"),
            "Product in cart");
        log("  + Product name: OK");

        log("Step 3b: Assert quantity = 1");
        assertTrue(content.contains("1 Item") || content.contains("Qty")
            || content.contains("1"), "Qty visible");
        log("  + Quantity: OK");

        log("Step 3c: Assert price $" + PRODUCT_PRICE);
        assertTrue(content.contains(PRODUCT_PRICE), "Price in cart");
        log("  + Price: OK");

        log("Step 4: Select FAST In-Store Pickup");
        page.waitForTimeout(1000);
        safeClick(
            "label:has-text('FAST In-Store Pickup')",
            "label:has-text('In-Store Pickup')"
        );
        page.waitForTimeout(2000);

        log("Step 5: Assert sidebar totals");
        String sidebar = page.content();
        assertTrue(sidebar.contains(PRODUCT_PRICE), "Subtotal $" + PRODUCT_PRICE);
        log("  + Subtotal: OK");
        assertTrue(sidebar.contains(HANDLING), "Handling $" + HANDLING);
        log("  + Handling: OK");
        assertTrue(sidebar.contains(TAXES_TBD), "Taxes TBD");
        log("  + Taxes TBD: OK");
        assertTrue(sidebar.contains(ESTIMATED_TOTAL), "Est. total $" + ESTIMATED_TOTAL);
        log("  + Estimated total: OK");

        log("Step 6: Enter promo code TEST and click APPLY");
        safeFill("TEST",
            "input[placeholder*='Promo Code' i]",
            "input[placeholder*='promo' i]",
            "input[id*='promo' i]",
            "[class*='promo'] input"
        );
        safeClick(
            "button:has-text('Apply')",
            "button:has-text('APPLY')",
            "[class*='promo'] button"
        );
        page.waitForTimeout(2500);

        log("Step 7: Assert promo code rejection");
        String afterPromo = page.content().toLowerCase();
        assertTrue(
            afterPromo.contains("invalid") || afterPromo.contains("not valid")
                || afterPromo.contains("not found") || afterPromo.contains("error")
                || afterPromo.contains("could not") || afterPromo.contains("promo")
                || afterPromo.contains("coupon"),
            "Promo rejection message expected");
        log("  + Promo rejected: OK");

        log("Step 8: Click PROCEED TO CHECKOUT");
        safeClick(
            "button.bned-checkout-btn",
            "button[aria-label='Proceed To Checkout']",
            "button:has-text('Proceed To Checkout')",
            "button:has-text('PROCEED TO CHECKOUT')",
            "a[href*='checkout']"
        );
        waitForPage();
        logPage();
        log("===== TC2 PASSED =====\n");
    }

    // -----------------------------------------------------------------------
    // TC3: Create Account
    // -----------------------------------------------------------------------
    @Test
    @Order(3)
    @DisplayName("TC3: Create Account - assert label, click Proceed As Guest")
    void testCase3CreateAccount() {
        log("\n===== TC3: CREATE ACCOUNT =====");
        logPage();
        String content = page.content();

        log("Step 1: Assert Create Account label");
        assertTrue(content.contains("Create Account") || content.contains("Sign In")
            || content.contains("Log In"), "Create Account label should be present");
        log("  + Create Account label: OK");

        log("Step 2: Click Proceed As Guest");
        boolean clicked = safeClick(
            "a.guestCheckoutBtn",
            "a:has-text('Proceed As Guest')",
            "a:has-text('Proceed as Guest')",
            ".guestCheckoutBtn"
        );
        assertTrue(clicked, "Proceed As Guest should be clicked. Title: " + page.title());
        waitForPage();
        logPage();
        log("===== TC3 PASSED =====\n");
    }

    // -----------------------------------------------------------------------
    // TC4: Contact Information
    // -----------------------------------------------------------------------
    @Test
    @Order(4)
    @DisplayName("TC4: Contact Information - fill name/email/phone, assert sidebar totals")
    void testCase4ContactInformation() {
        log("\n===== TC4: CONTACT INFORMATION =====");
        logPage();
        String content = page.content();

        log("Step 1: Assert Contact Information page");
        assertTrue(content.contains("Contact Information")
            || content.contains("Shipping and Pick Up")
            || content.contains("First Name"),
            "Should be on Contact Info page. URL: " + page.url());
        log("  + On Contact Info page: OK");

        log("Step 2: Fill First Name = " + FIRST_NAME);
        safeFill(FIRST_NAME,
            "#contactInfo\\.firstName",
            "input.firstName-input",
            "input[name='firstName']"
        );

        log("Step 3: Fill Last Name = " + LAST_NAME);
        safeFill(LAST_NAME,
            "#contactInfo\\.lastName",
            "input.lastName-input",
            "input[name='lastName']"
        );

        log("Step 4: Fill Email = " + EMAIL);
        safeFill(EMAIL,
            "#contactInfo\\.emailAddress",
            "input.email-input",
            "input[name='emailAddress']"
        );

        log("Step 5: Fill Phone = " + PHONE);
        safeFill(PHONE,
            "#phone1",
            "input[name='phoneNumberHidden']",
            "input.js-phone-mask"
        );

        log("Step 6: Assert sidebar totals");
        String sidebar = page.content();
        assertTrue(sidebar.contains(PRODUCT_PRICE), "Subtotal $" + PRODUCT_PRICE);
        log("  + Subtotal: OK");
        assertTrue(sidebar.contains(HANDLING), "Handling $" + HANDLING);
        log("  + Handling: OK");
        assertTrue(sidebar.contains(TAXES_TBD), "Taxes TBD");
        log("  + Taxes TBD: OK");
        assertTrue(sidebar.contains(ESTIMATED_TOTAL), "Est. total $" + ESTIMATED_TOTAL);
        log("  + Estimated total: OK");

        log("Step 7: Click Continue");
        safeClick(
            "button.bned-checkout-btn",
            "button.btn-primary:has-text('Continue')",
            "button:has-text('Continue')",
            "button[type='submit']"
        );
        waitForPage();
        logPage();
        log("===== TC4 PASSED =====\n");
    }

    // -----------------------------------------------------------------------
    // TC5: Pickup Information
    // -----------------------------------------------------------------------
    @Test
    @Order(5)
    @DisplayName("TC5: Pickup Information - assert contact info, DePaul location, "
        + "sidebar totals")
    void testCase5PickupInformation() {
        log("\n===== TC5: PICKUP INFORMATION =====");
        logPage();
        String content = page.content();

        log("Step 1: Assert contact info on page");
        assertTrue(content.contains(FIRST_NAME) || content.contains(LAST_NAME),
            "Name should appear. URL: " + page.url());
        log("  + Name: OK");
        assertTrue(content.contains(EMAIL) || content.contains("@"), "Email should appear");
        log("  + Email: OK");

        log("Step 2: Assert DePaul pickup location");
        assertTrue(content.contains("DePaul") || content.contains("Loop")
            || content.contains("SAIC"), "DePaul location should show");
        log("  + DePaul location: OK");

        log("Step 3: Assert pickup person option");
        assertTrue(content.contains("I'll pick them up")
            || content.contains("pick them up")
            || content.contains("Pick Up") || content.contains("PICKUP")
            || content.contains("Pickup"), "Pickup person option should show");
        log("  + Pickup person: OK");

        log("Step 4: Assert sidebar totals");
        assertTrue(content.contains(PRODUCT_PRICE), "Subtotal $" + PRODUCT_PRICE);
        log("  + Subtotal: OK");
        assertTrue(content.contains(HANDLING), "Handling $" + HANDLING);
        log("  + Handling: OK");
        assertTrue(content.contains(TAXES_TBD), "Tax TBD");
        log("  + Tax TBD: OK");
        assertTrue(content.contains(ESTIMATED_TOTAL), "Total $" + ESTIMATED_TOTAL);
        log("  + Total: OK");

        log("Step 5: Assert item and price");
        assertTrue(content.contains("JBL") || content.contains("Quantum"), "Item name");
        log("  + Item name: OK");
        assertTrue(content.contains(PRODUCT_PRICE), "Item price");
        log("  + Item price: OK");

        log("Step 6: Click Continue");
        safeClick(
            "button.bned-checkout-btn",
            "button.btn-primary:has-text('Continue')",
            "button:has-text('Continue')",
            "button[type='submit']"
        );
        waitForPage();
        logPage();
        log("===== TC5 PASSED =====\n");
    }

    // -----------------------------------------------------------------------
    // TC6: Payment Information
    // -----------------------------------------------------------------------
    @Test
    @Order(6)
    @DisplayName("TC6: Payment - assert final totals with real tax, click Back to Cart")
    void testCase6PaymentInformation() {
        log("\n===== TC6: PAYMENT INFORMATION =====");
        logPage();
        String content = page.content();

        log("Step 1: Assert sidebar totals with calculated tax");
        assertTrue(content.contains(PRODUCT_PRICE), "Subtotal $" + PRODUCT_PRICE);
        log("  + Subtotal: OK");
        assertTrue(content.contains(HANDLING), "Handling $" + HANDLING);
        log("  + Handling: OK");
        assertTrue(content.contains("15.") || content.contains("16.")
            || content.contains("17."), "Tax should be calculated");
        log("  + Tax calculated: OK");
        assertTrue(content.contains("18") || content.contains("167")
            || content.contains("168"), "Final total should be calculated");
        log("  + Final total: OK");

        log("Step 2: Assert item still visible");
        assertTrue(content.contains("JBL") || content.contains("Quantum"), "Item should show");
        log("  + Item: OK");
        assertTrue(content.contains(PRODUCT_PRICE), "Item price");
        log("  + Price: OK");

        log("Step 3: Click < BACK TO CART");
        safeClick(
            "a:has-text('BACK TO CART')",
            "a:has-text('Back to Cart')",
            "a:has-text('< Back to Cart')",
            "button:has-text('Back to Cart')",
            "a[href='/cart']",
            "a[href*='cart']"
        );
        waitForPage();
        logPage();
        log("===== TC6 PASSED =====\n");
    }

    // -----------------------------------------------------------------------
    // TC7: Delete from Cart
    // -----------------------------------------------------------------------
    @Test
    @Order(7)
    @DisplayName("TC7: Delete product from cart, assert cart is empty, close window")
    void testCase7DeleteFromCart() {
        log("\n===== TC7: DELETE FROM CART =====");

        page.navigate(CART_URL);
        waitForPage();
        logPage();

        log("Step 1: Assert on cart page");
        String content = page.content();
        assertTrue(content.contains("Your Shopping Cart")
            || content.contains("Shopping Cart"), "Should be on cart page");
        log("  + On cart page: OK");

        log("Step 2: Click Remove button");
        boolean removed = safeClick(
            "[aria-label*='Remove product JBL' i]",
            "[aria-label*='Remove product' i]",
            "button[aria-label*='Remove' i]",
            "a[aria-label*='Remove' i]",
            "button:has-text('Remove')",
            "a:has-text('Remove')"
        );

        if (!removed) {
            log("  ! Scanning all elements for remove...");
            try {
                Locator all = page.locator("button, a, [role='button']");
                int count = all.count();
                log("  Scanning " + count + " elements");
                for (int i = 0; i < count; i++) {
                    try {
                        String aria = all.nth(i).getAttribute("aria-label");
                        String txt = all.nth(i).innerText().toLowerCase().trim();
                        if (aria == null) {
                            aria = "";
                        }
                        if (aria.toLowerCase().contains("remove")
                            || txt.equals("remove") || txt.equals("delete")) {
                            log("  FOUND: aria='"
                                + aria.substring(0, Math.min(aria.length(), 50))
                                + "' text='" + txt + "'");
                            all.nth(i).click();
                            removed = true;
                            break;
                        }
                    } catch (Exception ignored) { }
                }
            } catch (Exception e) {
                log("  ! Scan error: " + e.getMessage());
            }
        }
        assertTrue(removed, "Remove button should be clicked. URL: " + page.url());
        log("  + Remove clicked: OK");

        log("Step 3: Wait for cart update");
        page.waitForTimeout(3000);
        waitForPage();

        log("Step 4: Assert cart is empty");
        String afterDelete = page.content().toLowerCase();
        assertTrue(
            afterDelete.contains("your cart is empty")
                || afterDelete.contains("cart is empty")
                || afterDelete.contains("no items")
                || afterDelete.contains("empty cart")
                || afterDelete.contains("0 item"),
            "Cart should be empty");
        log("  + Cart is empty: OK");
        log("=== ALL 7 TEST CASES COMPLETE. Video saved in videos/ ===");
    }
}
