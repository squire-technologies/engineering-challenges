import { Injectable } from '@nestjs/common';
import { Service, Shop, TimeOff } from '../../generated/prisma/client';
import { PrismaService } from '../../prisma/prisma.service';
import { ShopWithCatalog, StaffWithHours } from '../shops.types';

@Injectable()
export class ShopRepository {
  constructor(private readonly prisma: PrismaService) {}

  findAllWithCatalog(): Promise<ShopWithCatalog[]> {
    return this.prisma.shop.findMany({
      orderBy: { name: 'asc' },
      include: {
        staff: { select: { id: true, name: true }, orderBy: { name: 'asc' } },
        services: { select: { id: true, name: true, durationMin: true, priceCents: true }, orderBy: { name: 'asc' } },
      },
    });
  }

  findShop(shopId: string): Promise<Shop | null> {
    return this.prisma.shop.findUnique({ where: { id: shopId } });
  }

  findService(shopId: string, serviceId: string): Promise<Service | null> {
    return this.prisma.service.findFirst({ where: { id: serviceId, shopId } });
  }

  /** All staff of the shop, or only `staffId` when it is given. */
  findStaffWithHours(shopId: string, staffId?: string): Promise<StaffWithHours[]> {
    return this.prisma.staff.findMany({
      where: { shopId, ...(staffId && { id: staffId }) },
      include: { workingHours: true },
      orderBy: { name: 'asc' },
    });
  }

  findTimeOff(staffIds: string[], from: Date, to: Date): Promise<TimeOff[]> {
    return this.prisma.timeOff.findMany({
      where: { staffId: { in: staffIds }, startAt: { lt: to }, endAt: { gt: from } },
      orderBy: { startAt: 'asc' },
    });
  }
}
