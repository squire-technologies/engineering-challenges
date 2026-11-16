import { Injectable, NotFoundException } from '@nestjs/common';
import { Service, Shop, TimeOff } from '../generated/prisma/client';
import { ShopRepository } from './repositories/shop.repository';
import { ShopWithCatalog, StaffWithHours } from './shops.types';

@Injectable()
export class ShopsService {
  constructor(private readonly shops: ShopRepository) {}

  listWithCatalog(): Promise<ShopWithCatalog[]> {
    return this.shops.findAllWithCatalog();
  }

  async getShop(shopId: string): Promise<Shop> {
    const shop = await this.shops.findShop(shopId);
    if (!shop) {
      throw new NotFoundException('Shop not found');
    }
    return shop;
  }

  async getService(shopId: string, serviceId: string): Promise<Service> {
    const service = await this.shops.findService(shopId, serviceId);
    if (!service) {
      throw new NotFoundException('Service not found in this shop');
    }
    return service;
  }

  /** All staff of the shop, or only `staffId` when it is given. */
  async getStaffWithHours(shopId: string, staffId?: string): Promise<StaffWithHours[]> {
    const staff = await this.shops.findStaffWithHours(shopId, staffId);
    if (staffId && staff.length === 0) {
      throw new NotFoundException('Staff member not found in this shop');
    }
    return staff;
  }

  findTimeOff(staffIds: string[], from: Date, to: Date): Promise<TimeOff[]> {
    return this.shops.findTimeOff(staffIds, from, to);
  }
}
