# UX Foundation: Wida3.ma
**PRD Reference**: docs/prd-wida3-ma.md
**Version**: 1.0 | **Date**: 2026-07-21 | **Author**: UX Designer

## 1. User Personas (minimal — YAGNI)
| Persona | Role | Goal | Pain Point |
|---|---|---|---|
| Hassan | Warehouse Owner | Earn revenue from unused space with minimal effort | Doesn't want to negotiate/manage bookings by phone |
| Salma | Business Renter (logistics SME) | Find flexible short-term storage near her routes | Long-term leases lock up cash she doesn't need to spend |
| Admin (internal) | Marketplace Admin | Keep listings trustworthy, resolve disputes | Needs visibility across all listings/bookings in one place |

## 2. Information Architecture / Site Map
```
[App Root]
├── [Public] Search & Browse
│   ├── Search results (city/type/size filters)
│   └── Listing detail
├── [Auth] Login / Register
├── [Owner Dashboard]
│   ├── My Listings (create/edit/deactivate)
│   └── My Bookings (incoming)
├── [Renter Dashboard]
│   ├── My Bookings
│   └── Booking detail (access code once confirmed)
└── [Admin Dashboard]
    ├── Pending Listings (approve/reject)
    └── All Bookings
```

## 3. Core User Flows (top 3 journeys)

### Flow 1: Renter searches and books
```
[Search page] → [Enter city/type/size] → [Results list] → [Select listing]
   → [Listing detail] → [Choose weeks] → [Confirm & pay] → [Payment succeeds?]
        ↓ No                                                    ↓ Yes
   [Payment error, retry]                          [Booking confirmed + access code shown]
```

### Flow 2: Owner lists a warehouse
```
[Owner Dashboard] → [Add Listing] → [Fill details: city, type, size, price, photos]
   → [Submit] → [Status: Pending Approval] → [Admin approves?]
        ↓ No                                        ↓ Yes
   [Rejected, owner notified with reason]     [Status: Active, visible in search]
```

### Flow 3: Admin approves listings
```
[Admin Dashboard] → [Pending Listings queue] → [Review listing details/photos]
   → [Approve or Reject] → [Owner notified]
```

## 4. Key Screen Wireframes (text-based)

### Screen: Search Results
```
┌─────────────────────────────────────────┐
│ [Logo]      [City ▾] [Type ▾] [Size ▾]  │
├─────────────────────────────────────────┤
│ ┌───────────┐ ┌───────────┐ ┌─────────┐ │
│ │ [Photo]   │ │ [Photo]   │ │ [Photo] │ │
│ │ Title      │ │ Title     │ │ Title   │ │
│ │ City · m²  │ │ City · m² │ │ City·m² │ │
│ │ Price/wk   │ │ Price/wk  │ │ Price/wk│ │
│ └───────────┘ └───────────┘ └─────────┘ │
└─────────────────────────────────────────┘
```

### Screen: Booking Confirmation
```
┌─────────────────────────────┐
│ [Listing summary]           │
├─────────────────────────────┤
│ Weeks: [start] – [end]      │
│ Total: XXX MAD              │
│  [Pay & Confirm Booking]    │
├─────────────────────────────┤
│ Booked? → Access Code: XXXX │
└─────────────────────────────┘
```

## 5. Screen States
| Screen | Empty State | Loading | Error | Success |
|---|---|---|---|---|
| Search results | "No warehouses match — try widening your filters" | Skeleton cards | "Search failed, try again" | Results grid |
| Listing detail | N/A | Skeleton | "Listing not found" | Full detail + booking CTA |
| Booking confirm | N/A | Spinner on submit | "Payment failed — try again" (booking not created) | Access code shown + emailed |
| Owner listings | "You haven't listed a warehouse yet — [Add Listing]" | Skeleton rows | "Couldn't load listings" | List with status badges |
| Admin queue | "No listings pending review" | Skeleton rows | "Couldn't load queue" | List with Approve/Reject actions |
