# Stories: Wida3.ma
**PRD**: docs/prd-wida3-ma.md
**Architecture**: docs/architecture-wida3-ma.md
**Test Strategy**: docs/test-strategy-wida3-ma.md

## Epic 1: Auth & Roles
Users can register, log in, and hold OWNER/RENTER/ADMIN roles.

### Story 1.1: User registration & login
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev

**Description**: As a user, I want to register and log in, so that I can access role-specific features.

**Acceptance Criteria**:
```gherkin
Given a new email not already registered
When I submit registration with a valid password
Then my account is created and I receive a JWT + refresh cookie
```

**Technical Notes**: Uses `/api/v1/auth/register`, `/api/v1/auth/login` (Architecture §5). Touches `users`, `roles`, `user_roles`. Security: BCrypt hashing, breach-list password check (Security Baseline §3).

**Dependencies**: None (foundation story)

---

### Story 1.2: JWT refresh & logout
**Priority**: Must | **Size**: S | **Specialist**: Backend Dev + Frontend Dev

**Description**: As a logged-in user, I want my session to refresh silently, so that I'm not logged out mid-task.

**Acceptance Criteria**:
```gherkin
Given a valid refresh cookie and an expired access token
When the frontend calls /api/v1/auth/refresh
Then a new access token is issued without requiring re-login
```

**Dependencies**: 1.1

## Epic 2: Warehouse Listings
Owners can create and manage listings; renters and the public can search them.

### Story 2.1: Owner creates a listing
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev + Frontend Dev

**Description**: As an Owner, I want to create a listing with city, type, size, price, and photos, so that renters can find my space.

**Acceptance Criteria**:
```gherkin
Given I am authenticated as an Owner
When I submit a listing with all required fields
Then it is saved with status PENDING_APPROVAL
```

**Technical Notes**: Uses `/api/v1/listings` POST, `listings` + `listing_photos` tables (Database doc §3). File upload uses pre-signed URL flow (Architecture §5) — blocked on storage provider decision.

**Dependencies**: 1.1

---

### Story 2.2: Public search by city/type/size
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev + Frontend Dev

**Acceptance Criteria**: see Test Strategy Feature "Listing approval" scenarios (only ACTIVE listings appear).

**Technical Notes**: Uses `idx_listings_city_status` (Database doc §4).

**Dependencies**: 2.1

---

### Story 2.3: Admin approves/rejects listings
**Priority**: Must | **Size**: S | **Specialist**: Backend Dev + Frontend Dev

**Acceptance Criteria**: see Test Strategy Feature "Listing approval".

**Dependencies**: 2.1

## Epic 3: Booking & Payment
Renters book by the week and pay online; access codes are issued on confirmation.

### Story 3.1: Renter books available weeks
**Priority**: Must | **Size**: L | **Specialist**: Backend Dev + Frontend Dev

**Acceptance Criteria**: see Test Strategy Feature "Booking a warehouse" (all 3 scenarios).

**Technical Notes**: Server-calculates `total_price` (never trust client — Security Baseline §2 Tampering row). Blocked on payment provider decision (Architecture SDR-4).

**Dependencies**: 2.2, payment provider decision

---

### Story 3.2: Access code generation on confirmed booking
**Priority**: Must | **Size**: S | **Specialist**: Backend Dev

**Acceptance Criteria**: Given a booking transitions to CONFIRMED, an access_codes row is created and returned only to the renter and listing owner.

**Dependencies**: 3.1

## Epic 4: Admin Oversight

### Story 4.1: Admin views all bookings
**Priority**: Should | **Size**: S | **Specialist**: Backend Dev + Frontend Dev

**Dependencies**: 3.1

## Sprint Allocation
| Sprint | Stories | Estimated Effort |
|---|---|---|
| Sprint 1 | Foundation docs only (this sprint — no code) | Done |
| Sprint 2 | 1.1, 1.2, 2.1 | ~5-6 days (incl. payment/storage provider decision) |
| Sprint 3 | 2.2, 2.3, 3.1 | ~5-6 days |
| Sprint 4 | 3.2, 4.1 + hardening/test-gap closure | ~3-4 days |
