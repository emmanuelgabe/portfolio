import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ClickOutsideDirective } from './click-outside.directive';

@Component({
  template: `
    <div class="container">
      <div class="target" clickOutside (clickOutside)="onClickOutside()">
        <span class="inside">Inside content</span>
      </div>
      <div class="outside">Outside content</div>
    </div>
  `,
  standalone: true,
  imports: [ClickOutsideDirective],
})
class TestHostComponent {
  clickOutsideCount = 0;

  onClickOutside(): void {
    this.clickOutsideCount++;
  }
}

describe('ClickOutsideDirective', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let component: TestHostComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // ========== Basic Tests ==========

  it('should create the directive', () => {
    // Arrange
    const targetElement = fixture.nativeElement.querySelector('.target');

    // Assert
    expect(targetElement).toBeTruthy();
  });

  // ========== Click Inside Tests ==========

  it('should_notEmit_when_clickInsideTargetElement', () => {
    // Arrange
    const targetElement = fixture.nativeElement.querySelector('.target');

    // Act
    targetElement.click();
    fixture.detectChanges();

    // Assert
    expect(component.clickOutsideCount).toBe(0);
  });

  it('should_notEmit_when_clickOnChildInsideTarget', () => {
    // Arrange
    const insideElement = fixture.nativeElement.querySelector('.inside');

    // Act
    insideElement.click();
    fixture.detectChanges();

    // Assert
    expect(component.clickOutsideCount).toBe(0);
  });

  // ========== Click Outside Tests ==========

  it('should_emit_when_clickOutsideTargetElement', () => {
    // Arrange
    const outsideElement = fixture.nativeElement.querySelector('.outside');

    // Act
    outsideElement.click();
    fixture.detectChanges();

    // Assert
    expect(component.clickOutsideCount).toBe(1);
  });

  it('should_emit_when_clickOnContainer', () => {
    // Arrange
    const containerElement = fixture.nativeElement.querySelector('.container');

    // Act
    containerElement.click();
    fixture.detectChanges();

    // Assert
    expect(component.clickOutsideCount).toBe(1);
  });

  it('should_emit_when_clickOnDocument', () => {
    // Arrange / Act
    document.body.click();
    fixture.detectChanges();

    // Assert
    expect(component.clickOutsideCount).toBe(1);
  });

  // ========== Multiple Clicks Tests ==========

  it('should_emitMultipleTimes_when_multipleClicksOutside', () => {
    // Arrange
    const outsideElement = fixture.nativeElement.querySelector('.outside');

    // Act
    outsideElement.click();
    outsideElement.click();
    outsideElement.click();
    fixture.detectChanges();

    // Assert
    expect(component.clickOutsideCount).toBe(3);
  });

  it('should_notIncrementCount_when_mixedInsideAndOutsideClicks', () => {
    // Arrange
    const insideElement = fixture.nativeElement.querySelector('.inside');
    const outsideElement = fixture.nativeElement.querySelector('.outside');

    // Act
    insideElement.click(); // Should NOT emit
    outsideElement.click(); // Should emit
    insideElement.click(); // Should NOT emit
    outsideElement.click(); // Should emit
    fixture.detectChanges();

    // Assert
    expect(component.clickOutsideCount).toBe(2);
  });
});
