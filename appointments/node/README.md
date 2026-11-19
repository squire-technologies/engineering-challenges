# Appointments Challenge: Salon Booking (Node.js)

Thank you for your time. This challenge is the main part of the interview.

You will add features to a small booking service during the interview.
We care about your decisions and your reasons more than the amount of code.
You can use AI tools. We use them every day.

## Before the interview

Set up the project before the interview. Setup time counts against your build time.

You need Docker with Docker Compose, and Node.js 22.12 or later (Node.js 24 is recommended).

```bash
cd appointments/node
make setup            # installs packages and generates the Prisma client
make seed             # starts PostgreSQL, applies the migrations, and loads the seed data
make dev              # starts the API and the UI on http://localhost:3000
make check-bookings   # in a second terminal: sends concurrent bookings to the API
```

On the starting code, `make check-bookings` shows that 1 of 4 checks passes. This is expected.

Open http://localhost:3000. You can book an appointment and see it in the day view.
If all of this works, you are ready.

## Context

Squire is software for barbershops. Customers book online, and staff manage their day in a calendar.
Squire is now expanding to salons. Salon services are longer, and some have processing time.
For example, hair color must sit for 45 minutes after the stylist applies it.
During that time, the stylist can serve another client.

This repository is a small model of our booking service. It works, but it was built for barbershops.

## The code

The stack is NestJS, Prisma 7, and PostgreSQL 17.

| Path | Contents |
|---|---|
| `prisma/schema.prisma` | The data model. |
| `prisma/migrations/` | SQL migrations. Prisma creates them, and you can edit them by hand. |
| `prisma/seed.ts` | The seed data: Corner Barbers (3 staff) and Luxe Salon (30 staff, about 14,000 appointments). |
| `src/shops/` | The shops module: shops, staff, working hours, time off, and services. |
| `src/booking/` | The booking module: availability, booking, cancellation, and the schedule of one day. |
| `src/common/time.ts` | Time zone helpers. Working hours are local wall-clock times in the shop's time zone. |
| `public/index.html` | A booking page and a staff day view. It is plain HTML with no build step. |
| `scripts/check-bookings.ts` | The `make check-bookings` checks. |

Each module has the same layers. A controller calls a service, and a service calls a repository.
Only repositories use Prisma. A module reads another module's data only through that module's exported service.
Keep these rules in your changes.

### API

| Method and path | Purpose |
|---|---|
| `GET /shops` | All shops, with their staff and services. |
| `GET /shops/:shopId/availability?serviceId=&date=&days=&staffId=` | Free start times for `days` days from `date` (`YYYY-MM-DD`, shop time). Leave out `staffId` to include all staff. |
| `POST /appointments` | Books an appointment. Body: `shopId`, `serviceId`, `staffId` (optional), `start` (ISO 8601), `customerName`, `customerPhone`. Returns `201`, or `409` when the time is not available. |
| `POST /appointments/:id/cancel` | Cancels an appointment. |
| `GET /shops/:shopId/schedule?date=` | The staff, working hours, time off, and appointments of one day. |

The server log shows the duration of each request.

## The task

The interviewer will give you a few minutes to read the code.
Then tell the interviewer your plan before you write the first prompt.
Tell them what can go wrong, too.

### Part 1: processing time (required)

A service can have steps. Each step is `busy` or `free`, and has a duration in minutes.
During a `free` step, the staff member can serve another client.

- Color (in both shops) becomes 15 minutes busy, 45 minutes free, and 20 minutes busy.
- Availability and booking must use only the busy time.
- The shop can change the steps of a service later. Appointments that already exist must not change.

### Part 2: no double booking (required)

- The busy time of two appointments must never overlap for the same staff member.
- This must stay true when many requests arrive at the same time.
- A cancelled appointment frees its time.
- `make check-bookings` must pass all 4 checks.

### Part 3: checkout hold and deposit (stretch)

Salon services are expensive, so the shop takes a deposit at booking.

- Color takes a deposit of 2000 cents. A booking for a service with a deposit starts as `held`.
- A hold lasts 10 minutes. Make this value configurable, so that you can test it.
- The payment provider calls `POST /webhooks/payments` with `eventId`, `appointmentId`, and `amountCents`. The payment confirms the hold.
- The provider can send the same event more than once, and at the same time.
- A hold that expires frees its time.
- A payment can arrive after its hold expired. If that happens, record that the deposit needs a refund. The refund itself is out of scope.

There is no payment provider. You build the webhook endpoint, and you send the webhook yourself with `curl`:

```bash
curl -X POST localhost:3000/webhooks/payments -H 'content-type: application/json' \
  -d '{"eventId":"evt_1","appointmentId":"<id of the held booking>","amountCents":2000}'
```

To send a duplicate, send the same `eventId` again. `make check-bookings` does not test this part.

### Part 4: frontend (optional)

Do one of these:

- Show processing time in the day view.
- When a booking returns `409`, show a clear message and refresh the free times.

## Rules

- Keep the fields `id`, `start`, `end`, and `status` in the appointment response. `make check-bookings` reads them.
- Keep the shape of the schedule response. `make check-bookings` reads it to clean up its bookings.
- Keep the names of the seed shops, staff, and services. `make check-bookings` uses them.
- You can change everything else: the schema, the code, the libraries, and the structure.
- Use AI as you want. You must understand and explain every line that you submit.

## What we look at

- How you model time, steps, and the appointment lifecycle.
- How you keep data correct when requests run at the same time.
- How the system performs on a large salon.
- How you direct the AI, and how you check its work.
- How clearly you explain your decisions.
