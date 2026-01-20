import { Injectable, isDevMode } from '@angular/core';
import * as Sentry from '@sentry/angular';
import { environment } from '../../environments/environment';

export enum LogLevel {
  TRACE = 0,
  DEBUG = 1,
  INFO = 2,
  WARN = 3,
  ERROR = 4,
  FATAL = 5,
}

/**
 * Logging service with structured logging support
 * Automatically adjusts log level based on build mode (development vs production)
 * - Development: DEBUG level (all logs visible)
 * - Production: WARN level (only warnings and errors)
 */
@Injectable({
  providedIn: 'root',
})
export class LoggerService {
  private get minLevel(): LogLevel {
    if (isDevMode()) {
      return LogLevel.DEBUG;
    }
    return LogLevel.WARN;
  }

  /**
   * Log trace message
   * @param message Log message
   * @param context Optional context data
   */
  trace(message: string, context?: unknown): void {
    this.log(LogLevel.TRACE, message, context);
  }

  /**
   * Log debug message
   * @param message Log message
   * @param context Optional context data
   */
  debug(message: string, context?: unknown): void {
    this.log(LogLevel.DEBUG, message, context);
  }

  /**
   * Log info message
   * @param message Log message
   * @param context Optional context data
   */
  info(message: string, context?: unknown): void {
    this.log(LogLevel.INFO, message, context);
  }

  /**
   * Log warning message
   * @param message Log message
   * @param context Optional context data
   */
  warn(message: string, context?: unknown): void {
    this.log(LogLevel.WARN, message, context);
  }

  /**
   * Log error message
   * @param message Log message
   * @param context Optional context data
   */
  error(message: string, context?: unknown): void {
    this.log(LogLevel.ERROR, message, context);
  }

  /**
   * Log fatal error message
   * @param message Log message
   * @param context Optional context data
   */
  fatal(message: string, context?: unknown): void {
    this.log(LogLevel.FATAL, message, context);
  }

  private log(level: LogLevel, message: string, context?: unknown): void {
    if (level < this.minLevel) {
      return;
    }

    const timestamp = new Date().toISOString();
    const levelName = LogLevel[level];
    const contextStr = context ? ` ${JSON.stringify(context)}` : '';
    const formattedMessage = `[${timestamp}] [${levelName}] ${message}${contextStr}`;

    this.logToConsole(level, formattedMessage);

    if (environment.production && level >= LogLevel.ERROR) {
      this.logToServer(level, message, context);
    }
  }

  /* eslint-disable no-console -- Logger service intentionally uses all console methods */
  private logToConsole(level: LogLevel, message: string): void {
    switch (level) {
      case LogLevel.TRACE:
      case LogLevel.DEBUG:
        console.debug(message);
        break;
      case LogLevel.INFO:
        console.info(message);
        break;
      case LogLevel.WARN:
        console.warn(message);
        break;
      case LogLevel.ERROR:
      case LogLevel.FATAL:
        console.error(message);
        break;
    }
  }
  /* eslint-enable no-console */

  private logToServer(level: LogLevel, message: string, context?: unknown): void {
    if (!environment.sentry.enabled) {
      return;
    }

    const sentryLevel = this.mapToSentryLevel(level);

    Sentry.captureMessage(message, {
      level: sentryLevel,
      extra: context as Record<string, unknown>,
    });
  }

  private mapToSentryLevel(level: LogLevel): Sentry.SeverityLevel {
    switch (level) {
      case LogLevel.FATAL:
        return 'fatal';
      case LogLevel.ERROR:
        return 'error';
      case LogLevel.WARN:
        return 'warning';
      case LogLevel.INFO:
        return 'info';
      default:
        return 'debug';
    }
  }
}
