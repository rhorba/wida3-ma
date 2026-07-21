# Test Strategy: Wida3.ma
**Stories Reference**: docs/stories-wida3-ma.md
**Version**: 1.0 | **Date**: 2026-07-21 | **Author**: Test Architect

## 1. Risk Assessment
| Component | Impact | Frequency | Complexity | Test Level |
|---|---|---|---|---|
| Booking + payment flow | H | M | H | Maximum |
| Access code generation/delivery | H | M | L | High |
| Listing search (city/type/size) | M | H | M | High |
| Listing CRUD (owner) | M | M | L | Standard |
| Admin approval flow | M | L | L | Standard |
| Auth (register/login/JWT) | H | H | M | Maximum |

## 2. Test Pyramid Targets
| Layer | Coverage Target | Tooling |
|---|---|---|
| Unit | ≥ 60% of business logic | JUnit 5 + Mockito (backend), Vitest (frontend) |
| Integration | ≥ 40% of API + DB layer | Spring Boot Test + Testcontainers (Postgres), MSW (frontend API mocking) |
| E2E | Critical happy paths only | Playwright |
| **Combined gate** | **≥ 80%** — non-negotiable | CI blocks merge if below (JaCoCo backend, Vitest coverage frontend) |

## 3. ATDD Acceptance Scenarios (critical paths)
```gherkin
Feature: Booking a warehouse

  Scenario: Renter successfully books an available listing
    Given an active listing with no overlapping bookings for the chosen weeks
    When the renter selects dates and completes payment
    Then the booking status is CONFIRMED
    And an access code is generated and returned to the renter

  Scenario: Renter attempts to book already-booked weeks
    Given a listing with a CONFIRMED booking for weeks 1-2
    When another renter tries to book overlapping weeks
    Then the booking request is rejected with a conflict error
    And no payment is attempted

  Scenario: Payment fails during booking
    Given a renter has selected valid available weeks
    When the payment provider declines the charge
    Then the booking status remains PENDING_PAYMENT or is cancelled
    And no access code is generated

Feature: Listing approval

  Scenario: Admin approves a pending listing
    Given a listing with status PENDING_APPROVAL
    When the admin approves it
    Then the listing status becomes ACTIVE
    And it appears in public search results

  Scenario: Unapproved listing does not appear in search
    Given a listing with status PENDING_APPROVAL
    When a renter searches matching city/type
    Then the listing does not appear in results
```

## 4. Adversarial Checklist (high-risk components only)
- [ ] Input abuse: empty/oversized listing photos, malformed date ranges, negative/zero price or size
- [ ] Auth abuse: expired/tampered JWT, refresh-token replay, accessing another user's booking/listing by guessing IDs (IDOR)
- [ ] Race conditions: two renters booking the same weeks simultaneously (double-booking prevention via DB constraint/transaction)
- [ ] Business logic: booking end_date before start_date, non-week-multiple date ranges, booking a deactivated listing

## 5. Release Gate Criteria
- [ ] All acceptance scenarios pass
- [ ] Combined unit + integration coverage ≥ 80%
- [ ] No critical/high security findings open (Semgrep/Trivy — see DevOps doc)
- [ ] E2E happy path passes and is recorded (booking flow, per CLAUDE.md video-recording rule at version completion)
