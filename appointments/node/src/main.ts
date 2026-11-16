import 'dotenv/config';
import { join } from 'node:path';
import { Logger, ValidationPipe } from '@nestjs/common';
import { NestFactory } from '@nestjs/core';
import { NestExpressApplication } from '@nestjs/platform-express';
import type { NextFunction, Request, Response } from 'express';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.create<NestExpressApplication>(AppModule);
  const logger = new Logger('HTTP');
  const port = Number(process.env.PORT ?? 3000);

  app.use((req: Request, res: Response, next: NextFunction) => {
    const startedAt = performance.now();
    res.on('finish', () => {
      const ms = Math.round(performance.now() - startedAt);
      logger.log(`${req.method} ${req.originalUrl} ${res.statusCode} ${ms}ms`);
    });
    next();
  });
  app.useGlobalPipes(new ValidationPipe({ transform: true, whitelist: true, forbidNonWhitelisted: true }));
  app.useStaticAssets(join(__dirname, '..', 'public'));
  app.enableShutdownHooks();

  await app.listen(port, '0.0.0.0');
  logger.log(`Listening on http://localhost:${port}`);
}

void bootstrap();
