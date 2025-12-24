import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgChartsModule } from 'ng2-charts';
import { ChartConfiguration, ChartType } from 'chart.js';
import { DailyVisitorData } from '../../../services/active-users.service';

/**
 * Visitors Chart Component
 * Displays a line chart showing visitor data over the last 7 days.
 */
@Component({
  selector: 'app-visitors-chart',
  standalone: true,
  imports: [CommonModule, NgChartsModule],
  template: `
    <div class="chart-container">
      <canvas baseChart [data]="chartData" [options]="chartOptions" [type]="chartType"> </canvas>
    </div>
  `,
  styles: [
    `
      .chart-container {
        position: relative;
        height: 200px;
        width: 100%;
      }
    `,
  ],
})
export class VisitorsChartComponent implements OnChanges {
  @Input() dailyData: DailyVisitorData[] = [];

  chartType: ChartType = 'line';

  chartData: ChartConfiguration['data'] = {
    labels: [],
    datasets: [
      {
        label: 'Visitors',
        data: [],
        borderColor: 'rgb(13, 110, 253)',
        backgroundColor: 'rgba(13, 110, 253, 0.1)',
        fill: true,
        tension: 0.4,
        pointRadius: 2,
        pointHoverRadius: 5,
      },
    ],
  };

  chartOptions: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: false,
      },
      tooltip: {
        mode: 'index',
        intersect: false,
      },
    },
    scales: {
      x: {
        grid: {
          display: false,
        },
        ticks: {
          maxRotation: 0,
          autoSkip: true,
          maxTicksLimit: 6,
        },
      },
      y: {
        beginAtZero: true,
        grid: {
          color: 'rgba(0,0,0,0.05)',
        },
        ticks: {
          stepSize: 1,
        },
      },
    },
    interaction: {
      mode: 'nearest',
      axis: 'x',
      intersect: false,
    },
  };

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['dailyData'] && this.dailyData.length > 0) {
      this.updateChart();
    }
  }

  private updateChart(): void {
    this.chartData = {
      ...this.chartData,
      labels: this.dailyData.map((d) => {
        const date = new Date(d.date);
        return date.toLocaleDateString([], { weekday: 'short', day: 'numeric' });
      }),
      datasets: [
        {
          ...this.chartData.datasets[0],
          data: this.dailyData.map((d) => d.count),
        },
      ],
    };
  }
}
