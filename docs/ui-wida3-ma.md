# UI Foundation: Wida3.ma
**UX Reference**: docs/ux-wida3-ma.md
**Version**: 1.0 | **Date**: 2026-07-21 | **Author**: UI Designer

## 1. Design Approach
- **Strategy**: UI framework — Tailwind CSS + a small headless component set (Radix UI primitives) rather than a full custom design system.
- **Rationale**: YAGNI — no brand system exists yet; a utility framework gets a consistent, accessible UI shipped fast without building a component library from scratch. Revisit a full design system only if brand identity work demands bespoke visuals.

## 2. Design Tokens
```css
/* Colors — placeholder palette, swap once brand is defined */
--color-primary:     #1D4ED8;  /* trust-blue, CTA */
--color-secondary:   #F59E0B;  /* warm accent */
--color-background:  #FFFFFF;
--color-surface:     #F8FAFC;
--color-error:       #DC2626;
--color-success:     #16A34A;
--color-text:        #0F172A;
--color-text-muted:  #64748B;

/* Typography */
--font-family:   "Inter", system-ui, sans-serif;
--font-size-sm:  0.875rem;
--font-size-md:  1rem;
--font-size-lg:  1.25rem;
--font-size-xl:  1.75rem;

/* Spacing scale */
--spacing-xs: 0.25rem;  --spacing-sm: 0.5rem;
--spacing-md: 1rem;     --spacing-lg: 1.5rem;
--spacing-xl: 2.5rem;
```

## 3. Component Inventory
| Component | Reuse Existing | Build New | Notes |
|---|---|---|---|
| Button | Tailwind + Radix Slot | No | primary / secondary / destructive variants |
| Input / Select | Radix primitives + Tailwind | No | Used in search filters, listing form |
| Card (Listing) | — | Yes | photo, title, city/size, price/wk |
| Badge (status) | Radix + Tailwind | No | PENDING_APPROVAL / ACTIVE / REJECTED colors |
| Modal/Dialog | Radix Dialog | No | booking confirmation, reject-reason entry |
| DateRange Picker | react-day-picker | No | week-based booking selection |
| Toast/Notification | Radix Toast | No | booking success/error feedback |

## 4. Responsive Breakpoints
| Breakpoint | Width | Layout Notes |
|---|---|---|
| Mobile | < 768px | Single-column search results, filters collapse into a sheet |
| Tablet | 768–1024px | 2-column results grid |
| Desktop | > 1024px | 3-column results grid, persistent filter sidebar |

## 5. Accessibility Baseline
- Color contrast: AA minimum (4.5:1 normal text, 3:1 large text) — verify primary/secondary against white and surface backgrounds
- Focus indicators: visible on all interactive elements (Radix primitives provide this by default; don't strip with `outline-none` without a replacement)
- Semantic HTML first; ARIA only where native semantics are insufficient (Radix handles most dialog/toast ARIA automatically)
