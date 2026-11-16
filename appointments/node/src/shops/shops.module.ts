import { Module } from '@nestjs/common';
import { ShopRepository } from './repositories/shop.repository';
import { ShopsController } from './shops.controller';
import { ShopsService } from './shops.service';

@Module({
  controllers: [ShopsController],
  providers: [ShopRepository, ShopsService],
  exports: [ShopsService],
})
export class ShopsModule {}
