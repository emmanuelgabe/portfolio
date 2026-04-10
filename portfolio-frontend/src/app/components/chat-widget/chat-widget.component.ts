import {
  Component,
  OnInit,
  OnDestroy,
  inject,
  ViewChild,
  ElementRef,
  ChangeDetectorRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ChatMessage, ChatContactFormData, ChatRateLimit } from '../../models';
import { ChatService, ChatSseEvent } from '../../services/chat.service';
import { SiteConfigurationService } from '../../services/site-configuration.service';
import { LoggerService } from '../../services/logger.service';

/** Threshold at which the rate limit warning is displayed. */
const RATE_LIMIT_WARNING_THRESHOLD = 3;

/** Tool name to i18n key mapping. */
const TOOL_LABEL_KEYS: Record<string, string> = {
  searchProjects: 'chat.tools.searchProjects',
  listProjects: 'chat.tools.listProjects',
  getFeaturedProjects: 'chat.tools.getFeaturedProjects',
  getProjectDetails: 'chat.tools.getProjectDetails',
  listArticles: 'chat.tools.listArticles',
  getArticle: 'chat.tools.getArticle',
  listExperiences: 'chat.tools.listExperiences',
  getRecentExperiences: 'chat.tools.getRecentExperiences',
  listSkills: 'chat.tools.listSkills',
  getSiteConfig: 'chat.tools.getSiteConfig',
  getCurrentCV: 'chat.tools.getCurrentCV',
  prepareContact: 'chat.tools.prepareContact',
  searchPortfolio: 'chat.tools.searchPortfolio',
};

@Component({
  selector: 'app-chat-widget',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule],
  templateUrl: './chat-widget.component.html',
  styleUrls: ['./chat-widget.component.css'],
})
export class ChatWidgetComponent implements OnInit, OnDestroy {
  private readonly chatService = inject(ChatService);
  private readonly siteConfigService = inject(SiteConfigurationService);
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);
  private readonly logger = inject(LoggerService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroy$ = new Subject<void>();

  @ViewChild('messagesContainer') messagesContainer: ElementRef<HTMLElement> | undefined;
  @ViewChild('messageInput') messageInput: ElementRef<HTMLTextAreaElement> | undefined;

  isOpen = false;
  userInput = '';
  messages: ChatMessage[] = [];
  isStreaming = false;
  rateLimit: ChatRateLimit = { limit: 0, remaining: 0 };
  chatbotInfoUrl: string | undefined;

  /** Conversation starters shown when no messages exist. */
  readonly starters = [
    'chat.starters.projects',
    'chat.starters.skills',
    'chat.starters.experience',
    'chat.starters.contact',
  ];

  /** Contextual follow-up suggestions shown after assistant replies. */
  readonly suggestions = [
    'chat.suggestions.moreDetails',
    'chat.suggestions.otherProjects',
    'chat.suggestions.contactMe',
  ];

  ngOnInit(): void {
    this.chatService.rateLimit$.pipe(takeUntil(this.destroy$)).subscribe((rl) => {
      this.rateLimit = rl;
      this.cdr.markForCheck();
    });

    this.siteConfigService
      .getSiteConfiguration()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (config) => {
          this.chatbotInfoUrl = config.chatbotInfoUrl;
        },
        error: () => {
          this.logger.warn('[CHAT_WIDGET] Failed to load site config for chatbot info URL');
        },
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.chatService.abort();
  }

  toggleChat(): void {
    this.isOpen = !this.isOpen;
    if (this.isOpen) {
      setTimeout(() => this.focusInput(), 100);
    }
  }

  closeChat(): void {
    this.isOpen = false;
    this.chatService.abort();
  }

  sendMessage(content?: string): void {
    const text = (content || this.userInput).trim();
    if (!text || this.isStreaming) return;

    this.userInput = '';

    const userMsg: ChatMessage = {
      role: 'user',
      content: text,
      toolCalls: undefined,
      contactForm: undefined,
    };
    this.messages.push(userMsg);

    const assistantMsg: ChatMessage = {
      role: 'assistant',
      content: '',
      toolCalls: undefined,
      contactForm: undefined,
    };
    this.messages.push(assistantMsg);
    this.isStreaming = true;
    this.scrollToBottom();

    this.chatService
      .streamChat(text)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (event) => this.handleSseEvent(event, assistantMsg),
        error: (err) => {
          this.logger.error('[CHAT_WIDGET] Stream error', { status: err.status });
          assistantMsg.content = this.translate.instant('chat.error');
          this.isStreaming = false;
          this.cdr.markForCheck();
          this.scrollToBottom();
        },
        complete: () => {
          this.isStreaming = false;
          this.cdr.markForCheck();
          this.scrollToBottom();
          this.focusInput();
        },
      });
  }

  sendStarter(key: string): void {
    const text = this.translate.instant(key);
    this.sendMessage(text);
  }

  sendSuggestion(key: string): void {
    const text = this.translate.instant(key);
    this.sendMessage(text);
  }

  navigateToContact(contactForm: ChatContactFormData): void {
    this.router.navigate(['/contact'], {
      state: { subject: contactForm.subject, message: contactForm.message },
    });
    this.closeChat();
  }

  onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  getToolLabel(toolName: string): string {
    const key = TOOL_LABEL_KEYS[toolName];
    return key ? this.translate.instant(key) : toolName;
  }

  get showRateLimitWarning(): boolean {
    return (
      this.rateLimit.limit > 0 &&
      this.rateLimit.remaining <= RATE_LIMIT_WARNING_THRESHOLD &&
      this.rateLimit.remaining > 0
    );
  }

  get isRateLimitExhausted(): boolean {
    return this.rateLimit.limit > 0 && this.rateLimit.remaining <= 0;
  }

  get showStarters(): boolean {
    return this.messages.length === 0 && !this.isStreaming;
  }

  get showSuggestions(): boolean {
    return this.messages.length > 0 && !this.isStreaming && this.lastAssistantMessage !== undefined;
  }

  get lastAssistantMessage(): ChatMessage | undefined {
    for (let i = this.messages.length - 1; i >= 0; i--) {
      if (this.messages[i].role === 'assistant') {
        return this.messages[i];
      }
    }
    return undefined;
  }

  // ========== Private ==========

  private handleSseEvent(event: ChatSseEvent, assistantMsg: ChatMessage): void {
    switch (event.event) {
      case 'tool_calls':
        assistantMsg.toolCalls = this.chatService.parseToolCalls(event.data);
        break;
      case 'token':
        assistantMsg.content += event.data;
        break;
      case 'contact_form':
        assistantMsg.contactForm = this.chatService.parseContactFormData(event.data);
        break;
      case 'done':
        break;
    }
    this.cdr.markForCheck();
    this.scrollToBottom();
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      const el = this.messagesContainer?.nativeElement;
      if (el) {
        el.scrollTop = el.scrollHeight;
      }
    }, 0);
  }

  private focusInput(): void {
    this.messageInput?.nativeElement.focus();
  }
}
