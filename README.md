# Assignment 5 – SE333 Software Testing

![CI](https://github.com/test2330-cyber/Assignment5/actions/workflows/SE333_CI.yml/badge.svg)

## Project Overview

This project contains unit and integration tests for an Amazon shopping cart system written in Java, as part of SE333 at DePaul University.

### Package: `org.example.Amazon`

The system models a shopping cart with:
- **Item** – an immutable product with a name, type (ELECTRONIC or OTHER), quantity, and price per unit
- **ShoppingCart / ShoppingCartAdaptor** – manages cart items using an in-memory HSQLDB database
- **Amazon** – calculates total cost by applying a list of `PriceRule` objects
- **PriceRules:**
  - `RegularCost` – quantity × unit price for all items
  - `DeliveryPrice` – $0 (0 items), $5.00 (1–3), $12.50 (4–10), $20.00 (11+)
  - `ExtraCostForElectronics` – flat +$7.50 if any ELECTRONIC item is present

---

## Tests

### Unit Tests (`AmazonUnitTest.java`)
- All external dependencies are mocked using Mockito
- Tests cover: `Amazon`, `RegularCost`, `DeliveryPrice`, `ExtraCostForElectronics`, `Item`
- Includes both specification-based and structural-based tests

### Integration Tests (`AmazonIntegrationTest.java`)
- Uses the real `Database` and `ShoppingCartAdaptor` (no mocks)
- Tests end-to-end cart and pricing behavior including all delivery tiers and electronics surcharge

**Total: 45 tests, 0 failures**

---

## GitHub Actions CI Workflow

The CI pipeline (`.github/workflows/SE333_CI.yml`) runs on every push to `main` and:

1. ✅ **Checkstyle** – static analysis runs during the `validate` phase (does not fail the build on violations)
2. ✅ **JUnit Tests** – all 45 tests run via Maven Surefire
3. ✅ **JaCoCo** – code coverage report generated and uploaded as an artifact

### Artifacts produced:
- `checkstyle-report` → `target/checkstyle-result.xml`
- `jacoco-report` → `target/site/jacoco/jacoco.xml`

All GitHub Actions steps pass successfully. ✅

---

## Commits
- `YourName + added tests`
- `YourName + fixed java version`
- `YourName + added Workflow`
