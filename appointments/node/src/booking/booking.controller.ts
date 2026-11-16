import { Body, Controller, Get, HttpCode, Param, ParseUUIDPipe, Post, Query } from '@nestjs/common';
import { AppointmentsService } from './appointments.service';
import { AvailabilityService } from './availability.service';
import { ScheduleService } from './schedule.service';
import { AvailabilityQueryDto } from './dto/availability-query.dto';
import { BookAppointmentDto } from './dto/book-appointment.dto';
import { ScheduleQueryDto } from './dto/schedule-query.dto';
import { AppointmentResponse } from './dto/appointment.response';
import { DayAvailability, ScheduleDay } from './booking.types';

@Controller()
export class BookingController {
  constructor(
    private readonly appointments: AppointmentsService,
    private readonly availability: AvailabilityService,
    private readonly schedule: ScheduleService,
  ) {}

  @Get('shops/:shopId/availability')
  getAvailability(@Param('shopId', ParseUUIDPipe) shopId: string, @Query() query: AvailabilityQueryDto): Promise<DayAvailability[]> {
    return this.availability.getAvailability(shopId, query);
  }

  @Get('shops/:shopId/schedule')
  getSchedule(@Param('shopId', ParseUUIDPipe) shopId: string, @Query() { date }: ScheduleQueryDto): Promise<ScheduleDay> {
    return this.schedule.getDay(shopId, date);
  }

  @Post('appointments')
  book(@Body() dto: BookAppointmentDto): Promise<AppointmentResponse> {
    return this.appointments.book(dto);
  }

  @Post('appointments/:id/cancel')
  @HttpCode(200)
  cancel(@Param('id', ParseUUIDPipe) id: string): Promise<AppointmentResponse> {
    return this.appointments.cancel(id);
  }
}
