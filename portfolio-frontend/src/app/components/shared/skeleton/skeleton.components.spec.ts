import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SkeletonProjectCardComponent } from './skeleton-project-card.component';
import { SkeletonArticleCardComponent } from './skeleton-article-card.component';
import { SkeletonTimelineItemComponent } from './skeleton-timeline-item.component';
import { SkeletonTableRowComponent } from './skeleton-table-row.component';
import { SkeletonSkillCardComponent } from './skeleton-skill-card.component';
import { SkeletonProjectDetailComponent } from './skeleton-project-detail.component';
import { SkeletonArticleDetailComponent } from './skeleton-article-detail.component';

// ========== SkeletonProjectCardComponent Tests ==========

describe('SkeletonProjectCardComponent', () => {
  let component: SkeletonProjectCardComponent;
  let fixture: ComponentFixture<SkeletonProjectCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SkeletonProjectCardComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(SkeletonProjectCardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should_renderCardStructure_when_displayed', () => {
    // Arrange / Act
    const cardElement = fixture.nativeElement.querySelector('.skeleton-project-card');

    // Assert
    expect(cardElement).toBeTruthy();
  });

  it('should_renderImageSkeleton_when_displayed', () => {
    // Arrange / Act
    const imageElement = fixture.nativeElement.querySelector('.skeleton-image');

    // Assert
    expect(imageElement).toBeTruthy();
  });

  it('should_renderTitleSkeleton_when_displayed', () => {
    // Arrange / Act
    const titleElement = fixture.nativeElement.querySelector('.skeleton-title');

    // Assert
    expect(titleElement).toBeTruthy();
  });

  it('should_renderTextSkeletons_when_displayed', () => {
    // Arrange / Act
    const textElements = fixture.nativeElement.querySelectorAll('.skeleton-text');

    // Assert
    expect(textElements.length).toBeGreaterThan(0);
  });

  it('should_renderBadgeSkeletons_when_displayed', () => {
    // Arrange / Act
    const badgeElements = fixture.nativeElement.querySelectorAll('.skeleton-badge');

    // Assert
    expect(badgeElements.length).toBeGreaterThan(0);
  });

  it('should_haveAnimatedElements_when_displayed', () => {
    // Arrange / Act
    const animatedElements = fixture.nativeElement.querySelectorAll('.skeleton--animated');

    // Assert
    expect(animatedElements.length).toBeGreaterThan(0);
  });
});

// ========== SkeletonArticleCardComponent Tests ==========

describe('SkeletonArticleCardComponent', () => {
  let component: SkeletonArticleCardComponent;
  let fixture: ComponentFixture<SkeletonArticleCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SkeletonArticleCardComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(SkeletonArticleCardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should_renderCardStructure_when_displayed', () => {
    // Arrange / Act
    const cardElement = fixture.nativeElement.querySelector('.skeleton-article-card');

    // Assert
    expect(cardElement).toBeTruthy();
  });

  it('should_renderTitleSkeleton_when_displayed', () => {
    // Arrange / Act
    const titleElement = fixture.nativeElement.querySelector('.skeleton-title');

    // Assert
    expect(titleElement).toBeTruthy();
  });

  it('should_renderMetaSkeleton_when_displayed', () => {
    // Arrange / Act
    const metaElement = fixture.nativeElement.querySelector('.skeleton-meta');

    // Assert
    expect(metaElement).toBeTruthy();
  });

  it('should_renderDateSkeleton_when_displayed', () => {
    // Arrange / Act
    const dateElement = fixture.nativeElement.querySelector('.skeleton-date');

    // Assert
    expect(dateElement).toBeTruthy();
  });

  it('should_renderButtonSkeleton_when_displayed', () => {
    // Arrange / Act
    const buttonElement = fixture.nativeElement.querySelector('.skeleton-button');

    // Assert
    expect(buttonElement).toBeTruthy();
  });

  it('should_haveAnimatedElements_when_displayed', () => {
    // Arrange / Act
    const animatedElements = fixture.nativeElement.querySelectorAll('.skeleton--animated');

    // Assert
    expect(animatedElements.length).toBeGreaterThan(0);
  });
});

// ========== SkeletonTimelineItemComponent Tests ==========

describe('SkeletonTimelineItemComponent', () => {
  let component: SkeletonTimelineItemComponent;
  let fixture: ComponentFixture<SkeletonTimelineItemComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SkeletonTimelineItemComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(SkeletonTimelineItemComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should_defaultToLeftPosition_when_created', () => {
    // Arrange / Act - component is created with defaults

    // Assert
    expect(component.position).toBe('left');
  });

  it('should_renderTimelineItem_when_displayed', () => {
    // Arrange / Act
    const timelineItem = fixture.nativeElement.querySelector('.timeline-item');

    // Assert
    expect(timelineItem).toBeTruthy();
  });

  it('should_applyLeftClass_when_positionIsLeft', () => {
    // Arrange
    component.position = 'left';

    // Act
    fixture.detectChanges();
    const timelineItem = fixture.nativeElement.querySelector('.timeline-item');

    // Assert
    expect(timelineItem.classList.contains('timeline-left')).toBeTrue();
  });

  it('should_applyRightClass_when_positionIsRight', () => {
    // Arrange
    component.position = 'right';

    // Act
    fixture.detectChanges();
    const timelineItem = fixture.nativeElement.querySelector('.timeline-item');

    // Assert
    expect(timelineItem.classList.contains('timeline-right')).toBeTrue();
  });

  it('should_renderTimelineMarker_when_displayed', () => {
    // Arrange / Act
    const marker = fixture.nativeElement.querySelector('.timeline-marker');

    // Assert
    expect(marker).toBeTruthy();
  });

  it('should_renderTimelineCard_when_displayed', () => {
    // Arrange / Act
    const card = fixture.nativeElement.querySelector('.timeline-card');

    // Assert
    expect(card).toBeTruthy();
  });

  it('should_renderRoleSkeleton_when_displayed', () => {
    // Arrange / Act
    const roleElement = fixture.nativeElement.querySelector('.skeleton-role');

    // Assert
    expect(roleElement).toBeTruthy();
  });

  it('should_renderCompanySkeleton_when_displayed', () => {
    // Arrange / Act
    const companyElement = fixture.nativeElement.querySelector('.skeleton-company');

    // Assert
    expect(companyElement).toBeTruthy();
  });

  it('should_haveAnimatedElements_when_displayed', () => {
    // Arrange / Act
    const animatedElements = fixture.nativeElement.querySelectorAll('.skeleton--animated');

    // Assert
    expect(animatedElements.length).toBeGreaterThan(0);
  });
});

// ========== SkeletonTableRowComponent Tests ==========

describe('SkeletonTableRowComponent', () => {
  let component: SkeletonTableRowComponent;
  let fixture: ComponentFixture<SkeletonTableRowComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SkeletonTableRowComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(SkeletonTableRowComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should_haveDefaultColumns_when_created', () => {
    // Arrange / Act - component is created with defaults

    // Assert
    expect(component.columns).toBeDefined();
    expect(component.columns.length).toBe(4);
  });

  it('should_renderTableRow_when_displayed', () => {
    // Arrange / Act
    const tableRow = fixture.nativeElement.querySelector('.skeleton-table-row');

    // Assert
    expect(tableRow).toBeTruthy();
  });

  it('should_renderCorrectNumberOfCells_when_defaultColumns', () => {
    // Arrange / Act
    const cells = fixture.nativeElement.querySelectorAll('td');

    // Assert
    expect(cells.length).toBe(4);
  });

  it('should_renderCustomNumberOfCells_when_customColumnsProvided', () => {
    // Arrange
    component.columns = [{ width: '50%' }, { width: '50%' }];

    // Act
    fixture.detectChanges();
    const cells = fixture.nativeElement.querySelectorAll('td');

    // Assert
    expect(cells.length).toBe(2);
  });

  it('should_applyColumnWidths_when_widthsSpecified', () => {
    // Arrange
    component.columns = [{ width: '100px' }];

    // Act
    fixture.detectChanges();
    const cell = fixture.nativeElement.querySelector('.skeleton-cell');

    // Assert
    expect(cell.style.width).toBe('100px');
  });

  it('should_haveAnimatedCells_when_displayed', () => {
    // Arrange / Act
    const animatedElements = fixture.nativeElement.querySelectorAll('.skeleton--animated');

    // Assert
    expect(animatedElements.length).toBe(4);
  });
});

// ========== SkeletonSkillCardComponent Tests ==========

describe('SkeletonSkillCardComponent', () => {
  let component: SkeletonSkillCardComponent;
  let fixture: ComponentFixture<SkeletonSkillCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SkeletonSkillCardComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(SkeletonSkillCardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should_renderSkillCard_when_displayed', () => {
    // Arrange / Act
    const skillCard = fixture.nativeElement.querySelector('.skeleton-skill-card');

    // Assert
    expect(skillCard).toBeTruthy();
  });

  it('should_renderIconSkeleton_when_displayed', () => {
    // Arrange / Act
    const iconElement = fixture.nativeElement.querySelector('.skeleton-icon');

    // Assert
    expect(iconElement).toBeTruthy();
  });

  it('should_renderNameSkeleton_when_displayed', () => {
    // Arrange / Act
    const nameElement = fixture.nativeElement.querySelector('.skeleton-name');

    // Assert
    expect(nameElement).toBeTruthy();
  });

  it('should_haveAnimatedElements_when_displayed', () => {
    // Arrange / Act
    const animatedElements = fixture.nativeElement.querySelectorAll('.skeleton--animated');

    // Assert
    expect(animatedElements.length).toBe(2);
  });
});

// ========== SkeletonProjectDetailComponent Tests ==========

describe('SkeletonProjectDetailComponent', () => {
  let component: SkeletonProjectDetailComponent;
  let fixture: ComponentFixture<SkeletonProjectDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SkeletonProjectDetailComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(SkeletonProjectDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should_renderDetailStructure_when_displayed', () => {
    // Arrange / Act
    const detailElement = fixture.nativeElement.querySelector('.skeleton-project-detail');

    // Assert
    expect(detailElement).toBeTruthy();
  });

  it('should_renderTitleSkeleton_when_displayed', () => {
    // Arrange / Act
    const titleElement = fixture.nativeElement.querySelector('.skeleton-title');

    // Assert
    expect(titleElement).toBeTruthy();
  });

  it('should_renderImageCarouselSkeleton_when_displayed', () => {
    // Arrange / Act
    const carouselElement = fixture.nativeElement.querySelector('.skeleton-image-carousel');

    // Assert
    expect(carouselElement).toBeTruthy();
  });

  it('should_renderCardTitleSkeletons_when_displayed', () => {
    // Arrange / Act
    const cardTitles = fixture.nativeElement.querySelectorAll('.skeleton-card-title');

    // Assert
    expect(cardTitles.length).toBeGreaterThan(0);
  });

  it('should_renderButtonSkeletons_when_displayed', () => {
    // Arrange / Act
    const buttons = fixture.nativeElement.querySelectorAll('.skeleton-btn');

    // Assert
    expect(buttons.length).toBeGreaterThan(0);
  });

  it('should_haveAnimatedElements_when_displayed', () => {
    // Arrange / Act
    const animatedElements = fixture.nativeElement.querySelectorAll('.skeleton--animated');

    // Assert
    expect(animatedElements.length).toBeGreaterThan(0);
  });
});

// ========== SkeletonArticleDetailComponent Tests ==========

describe('SkeletonArticleDetailComponent', () => {
  let component: SkeletonArticleDetailComponent;
  let fixture: ComponentFixture<SkeletonArticleDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SkeletonArticleDetailComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(SkeletonArticleDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should_renderDetailStructure_when_displayed', () => {
    // Arrange / Act
    const detailElement = fixture.nativeElement.querySelector('.skeleton-article-detail');

    // Assert
    expect(detailElement).toBeTruthy();
  });

  it('should_renderBackButtonSkeleton_when_displayed', () => {
    // Arrange / Act
    const backButton = fixture.nativeElement.querySelector('.skeleton-back-btn');

    // Assert
    expect(backButton).toBeTruthy();
  });

  it('should_renderTitleSkeletons_when_displayed', () => {
    // Arrange / Act
    const titles = fixture.nativeElement.querySelectorAll('.skeleton-title');

    // Assert
    expect(titles.length).toBeGreaterThan(0);
  });

  it('should_renderMetaItems_when_displayed', () => {
    // Arrange / Act
    const metaItems = fixture.nativeElement.querySelectorAll('.skeleton-meta-item');

    // Assert
    expect(metaItems.length).toBeGreaterThan(0);
  });

  it('should_renderExcerptSkeletons_when_displayed', () => {
    // Arrange / Act
    const excerpts = fixture.nativeElement.querySelectorAll('.skeleton-excerpt');

    // Assert
    expect(excerpts.length).toBeGreaterThan(0);
  });

  it('should_renderContentSkeletons_when_displayed', () => {
    // Arrange / Act
    const contentElement = fixture.nativeElement.querySelector('.skeleton-content');

    // Assert
    expect(contentElement).toBeTruthy();
  });

  it('should_renderHeadingSkeleton_when_displayed', () => {
    // Arrange / Act
    const heading = fixture.nativeElement.querySelector('.skeleton-heading');

    // Assert
    expect(heading).toBeTruthy();
  });

  it('should_renderCodeBlockSkeleton_when_displayed', () => {
    // Arrange / Act
    const codeBlock = fixture.nativeElement.querySelector('.skeleton-code-block');

    // Assert
    expect(codeBlock).toBeTruthy();
  });

  it('should_haveAnimatedElements_when_displayed', () => {
    // Arrange / Act
    const animatedElements = fixture.nativeElement.querySelectorAll('.skeleton--animated');

    // Assert
    expect(animatedElements.length).toBeGreaterThan(0);
  });
});
