# SE333 Assignment 5 — Reflection: Manual vs AI-Assisted UI Testing

**Hamza Patal | DePaul University | SE333 Software Testing**

---

## Manual UI Testing (Java + Playwright — `BookstorePlaywrightTraditional`)

Writing the traditional Playwright tests in Java was a detailed, hands-on
process that required intimate familiarity with both the Playwright API and
the structure of the DePaul bookstore's HTML. Every selector — from the
`input.facet__list__checkbox[alt='JBL'] ~ span.facet__list__label` filter
approach to the `a.guestCheckoutBtn` guest checkout link — had to be
discovered manually by inspecting the live page source, running the test,
reading failure messages, and iterating. This cycle repeated many times
across multiple runs before all 7 test cases passed reliably.

The main challenge was that the site uses Bootstrap's collapse system and
screen-reader-only (`sr-only`) checkboxes for its filter panel. Standard
selectors like `label:has-text('JBL')` failed repeatedly because the
visible element turned out to be a `span.facet__list__label` sibling to
the hidden checkbox. No AI tool would have known this without seeing the
actual page HTML. Manual inspection was essential. Similarly, the promo code
input had no stable placeholder text, and the cart removal button required
an `aria-label` scan fallback. These kinds of site-specific quirks took
significant debugging time but produced highly accurate, stable tests once
resolved.

On the positive side, manually written tests give the developer complete
control. Every assertion is intentional, every fallback selector is
reasoned, and the test logic is fully transparent. The resulting test suite
— 7 ordered JUnit 5 test cases covering search, filters, cart operations,
checkout flow, contact info, pickup selection, and cart removal — passed
with zero failures. Maintenance is straightforward: when the site changes,
the developer knows exactly which selectors to update and why.

---

## AI-Assisted UI Testing (Playwright MCP — `BookstorePlaywrightLLM`)

The AI-assisted approach using Playwright MCP was a fundamentally different
experience. Rather than writing Java code line by line, the workflow involved
describing the desired test behavior in natural language and allowing the
agent to generate the corresponding code. The prompt described the full
purchase pathway — searching for earbuds, applying three filters, verifying
product details, adding to cart, selecting in-store pickup, checking totals,
entering a promo code, going through guest checkout, filling contact info,
and finally removing the item. The agent produced a complete, runnable
JUnit 5 test class much faster than the manual approach would have for a
first draft.

The AI agent was effective at generating boilerplate code — the
`@BeforeAll`/`@AfterAll` setup, the `@Test` method structure, video
recording configuration, and standard assertion patterns. It correctly
inferred patterns like using `page.waitForTimeout()` for network delays and
`LoadState.NETWORKIDLE` for page stability. The generated code was
immediately compilable and structurally sound, saving significant time on
scaffolding. For a developer unfamiliar with Playwright's Java API, this
acceleration is especially valuable.

However, the AI agent had notable limitations when it came to site-specific
behavior. Without live browser access to inspect the actual DOM, it could
not know that filter checkboxes are `sr-only` and require clicking a sibling
`span` instead. It could not know that the brand filter panel uses
`#facet-brand` while the color filter uses `#facet-Color` (mixed case). It
generated reasonable fallback selector chains, but these required the same
manual corrections discovered during the traditional testing phase. In
practice, the LLM-generated code served as an excellent first draft that
still needed a human expert to validate and refine the selectors against the
real page. This mirrors the experience of many teams adopting AI code
generation: the AI handles structure and patterns well, but domain-specific
details still require human oversight.

---

## Comparison Summary

**Ease of writing:** AI-assisted testing was faster for initial code
generation. The manual approach required more upfront investment but
produced more precise selectors immediately.

**Accuracy and reliability:** Manually written tests achieved 7/7 passing
on the first fully corrected run. AI-generated tests required the same
selector corrections, meaning the raw AI output would not have passed
without human review. The corrected LLM file performs identically because
the underlying Playwright logic is the same.

**Maintenance effort:** Manual tests are easier to maintain because every
decision is explicit and documented. AI-generated tests can drift toward
fragile selectors (e.g., text-based rather than structural) that break when
the site's copy changes. However, re-prompting the AI with updated page
structure could accelerate maintenance in future cycles.

**Limitations:** The most significant limitation of the AI approach is its
inability to inspect a live page in real time. Playwright MCP addresses
this by giving the agent actual browser access, which substantially closes
this gap. Even so, the agent's generated code benefited greatly from the
selector knowledge already discovered during the manual testing phase. A
fully autonomous agent starting from scratch on this site would have needed
multiple correction iterations before achieving a passing test run.

**Overall:** Manual and AI-assisted testing are complementary rather than
competing approaches. The manual approach builds deep understanding of the
application under test and produces the most reliable initial output. The
AI approach dramatically reduces boilerplate and scaffolding time and
serves as an excellent starting point for developers who understand enough
to review and correct the output. The ideal workflow is to use AI to
generate the structure and first-pass selectors, then apply manual
expertise to validate and harden the tests — exactly the workflow followed
in this assignment.
