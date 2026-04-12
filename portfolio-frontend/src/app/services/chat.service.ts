import { Injectable, inject } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { environment } from '../../environments/environment';
import { ChatContactFormData, ChatRateLimit, ChatSseEventType } from '../models';
import { LoggerService } from './logger.service';

/**
 * Parsed SSE event from the chat streaming endpoint.
 */
export interface ChatSseEvent {
  event: ChatSseEventType;
  data: string;
}

/**
 * Service for communicating with the chat streaming endpoint.
 *
 * Uses fetch + ReadableStream instead of EventSource because the backend
 * requires POST with a JSON body. Exposes rate limit state from response
 * headers and manages conversation IDs via crypto.randomUUID.
 */
@Injectable({
  providedIn: 'root',
})
export class ChatService {
  private readonly logger = inject(LoggerService);
  private readonly streamUrl = `${environment.apiUrl}/api/chat/stream`;

  private conversationId = crypto.randomUUID();
  private abortController: AbortController | undefined;

  private readonly rateLimitSubject = new Subject<ChatRateLimit>();
  readonly rateLimit$ = this.rateLimitSubject.asObservable();

  private currentRateLimit: ChatRateLimit = { limit: 0, remaining: 0 };

  /**
   * Stream a chat message via SSE. Returns an Observable that emits parsed
   * SSE events (tool_calls, token, contact_form, done) and completes when
   * the stream closes.
   *
   * @param message the visitor's message
   * @returns Observable of parsed SSE events
   */
  streamChat(message: string): Observable<ChatSseEvent> {
    return new Observable<ChatSseEvent>((subscriber) => {
      this.abortController = new AbortController();

      this.logger.info('[CHAT] POST /api/chat/stream', {
        conversationId: this.conversationId,
      });

      fetch(this.streamUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          conversationId: this.conversationId,
          message,
        }),
        signal: this.abortController.signal,
      })
        .then((response) => {
          this.extractRateLimit(response);

          if (!response.ok) {
            const status = response.status;
            this.logger.error('[CHAT_ERROR] Stream request failed', { status });
            subscriber.error({ status });
            return;
          }

          if (!response.body) {
            subscriber.error(new Error('No response body'));
            return;
          }

          this.readSseStream(response.body, subscriber);
        })
        .catch((error) => {
          if (error.name === 'AbortError') {
            this.logger.debug('[CHAT] Stream aborted by user');
            subscriber.complete();
            return;
          }
          this.logger.error('[CHAT_ERROR] Stream connection failed', {
            message: error.message,
          });
          subscriber.error(error);
        });

      return () => {
        this.abortController?.abort();
      };
    });
  }

  /**
   * Reset the conversation by generating a new ID.
   */
  resetConversation(): void {
    this.conversationId = crypto.randomUUID();
    this.logger.debug('[CHAT] Conversation reset', {
      conversationId: this.conversationId,
    });
  }

  getConversationId(): string {
    return this.conversationId;
  }

  getRateLimit(): ChatRateLimit {
    return this.currentRateLimit;
  }

  /**
   * Abort any in-progress stream.
   */
  abort(): void {
    this.abortController?.abort();
  }

  /**
   * Parse the SSE event types from the contact_form event data.
   */
  parseContactFormData(data: string): ChatContactFormData | undefined {
    try {
      return JSON.parse(data) as ChatContactFormData;
    } catch {
      this.logger.warn('[CHAT] Failed to parse contact form data', { data });
      return undefined;
    }
  }

  /**
   * Parse tool call names from the tool_calls event data.
   */
  parseToolCalls(data: string): string[] {
    try {
      return JSON.parse(data) as string[];
    } catch {
      this.logger.warn('[CHAT] Failed to parse tool calls', { data });
      return [];
    }
  }

  // ========== Private ==========

  private extractRateLimit(response: Response): void {
    const limit = response.headers.get('X-RateLimit-Limit');
    const remaining = response.headers.get('X-RateLimit-Remaining');

    if (limit && remaining) {
      this.currentRateLimit = {
        limit: parseInt(limit, 10),
        remaining: parseInt(remaining, 10),
      };
      this.rateLimitSubject.next(this.currentRateLimit);
      this.logger.debug('[CHAT] Rate limit updated', this.currentRateLimit);
    }
  }

  private readSseStream(
    body: ReadableStream<Uint8Array>,
    subscriber: {
      next: (value: ChatSseEvent) => void;
      complete: () => void;
      error: (err: unknown) => void;
    }
  ): void {
    const reader = body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';

    const pump = (): void => {
      reader
        .read()
        .then(({ done, value }) => {
          if (done) {
            // Flush any remaining buffered event block on stream end.
            if (buffer.trim()) {
              this.parseEventBlock(buffer, subscriber);
            }
            subscriber.complete();
            return;
          }

          buffer += decoder.decode(value, { stream: true });

          // Events are delimited by a blank line (\n\n). Split on the
          // delimiter and keep the last, possibly-incomplete segment in
          // the buffer for the next chunk.
          const blocks = buffer.split('\n\n');
          buffer = blocks.pop() ?? '';

          for (const block of blocks) {
            if (block.length > 0) {
              this.parseEventBlock(block, subscriber);
            }
          }

          pump();
        })
        .catch((error) => {
          if (error.name !== 'AbortError') {
            subscriber.error(error);
          } else {
            subscriber.complete();
          }
        });
    };

    pump();
  }

  /**
   * Parse a single SSE event block. A block is a sequence of non-blank
   * lines with fields like {@code event:} and {@code data:}. Per the SSE
   * spec, multiple {@code data:} lines within one event are concatenated
   * with a newline separator. The leading space is not stripped because
   * the backend does not insert one and LLM tokens may start with a
   * significant space.
   */
  private parseEventBlock(
    block: string,
    subscriber: { next: (value: ChatSseEvent) => void }
  ): void {
    const lines = block.split('\n');
    let eventName = '';
    const dataParts: string[] = [];

    for (const line of lines) {
      if (line.startsWith('event:')) {
        eventName = line.slice(6).trim();
      } else if (line.startsWith('data:')) {
        dataParts.push(line.slice(5));
      }
    }

    if (!eventName) {
      return;
    }

    subscriber.next({
      event: eventName as ChatSseEventType,
      data: dataParts.join('\n'),
    });
  }
}
