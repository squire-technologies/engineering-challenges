import { Module } from '@nestjs/common';
import { PrismaModule } from './prisma/prisma.module';
import { ShopsModule } from './shops/shops.module';
import { BookingModule } from './booking/booking.module';

@Module({
  imports: [PrismaModule, ShopsModule, BookingModule],
})
export class AppModule {}
