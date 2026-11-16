import { Module } from '@nestjs/common';
import { ShopsModule } from '../shops/shops.module';
import { AppointmentRepository } from './repositories/appointment.repository';
import { AppointmentsService } from './appointments.service';
import { AvailabilityService } from './availability.service';
import { ScheduleService } from './schedule.service';
import { BookingController } from './booking.controller';

@Module({
  imports: [ShopsModule],
  controllers: [BookingController],
  providers: [AppointmentRepository, AppointmentsService, AvailabilityService, ScheduleService],
})
export class BookingModule {}
