/**
 * SSE event types emitted by the backend streaming endpoint.
 *
 * Event sequence: [tool_calls?] -> token* -> [contact_form?] -> done
 */
export type ChatSseEventType = 'tool_calls' | 'token' | 'contact_form' | 'done';

/**
 * A single chat message displayed in the widget.
 */
export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  toolCalls: string[] | undefined;
  contactForm: ChatContactFormData | undefined;
}

/**
 * Contact form data sent by the backend when the prepareContact tool fires.
 * Used to pre-fill the contact form via router state.
 */
export interface ChatContactFormData {
  subject: string;
  message: string;
}

/**
 * Rate limit state tracked from response headers.
 */
export interface ChatRateLimit {
  limit: number;
  remaining: number;
}
