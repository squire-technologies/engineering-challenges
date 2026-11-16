import { Prisma } from '../generated/prisma/client';

export type StaffWithHours = Prisma.StaffGetPayload<{ include: { workingHours: true } }>;

export type ShopWithCatalog = Prisma.ShopGetPayload<{
  include: {
    staff: { select: { id: true; name: true } };
    services: { select: { id: true; name: true; durationMin: true; priceCents: true } };
  };
}>;
