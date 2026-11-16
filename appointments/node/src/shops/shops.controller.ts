import { Controller, Get } from '@nestjs/common';
import { ShopsService } from './shops.service';
import { ShopWithCatalog } from './shops.types';

@Controller('shops')
export class ShopsController {
  constructor(private readonly shops: ShopsService) {}

  @Get()
  list(): Promise<ShopWithCatalog[]> {
    return this.shops.listWithCatalog();
  }
}
