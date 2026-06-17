import { Component, Input, OnChanges, AfterViewInit, NgZone, HostListener } from '@angular/core';

export interface DonutSegment {
  label: string;
  value: number;
  color: string;
}

interface ComputedSeg extends DonutSegment {
  percentage: number;
  dasharray: string;
  dashoffset: number;
}

@Component({
  selector: 'app-donut-chart',
  standalone: true,
  templateUrl: './donut-chart.component.html',
  styleUrl: './donut-chart.component.css'
})
export class DonutChartComponent implements OnChanges, AfterViewInit {
  @Input() segments: DonutSegment[] = [];
  @Input() total: number = 0;
  @Input() centerLabel: string = 'Tổng';
  @Input() centerOverride: string = '';

  readonly C = 2 * Math.PI * 80; // 502.654

  computed: ComputedSeg[] = [];
  animated = false;
  hovered: ComputedSeg | null = null;

  // Tooltip position (fixed, below cursor)
  tipX = 0;
  tipY = 0;

  constructor(private zone: NgZone) {}

  ngOnChanges() { this.compute(); }

  ngAfterViewInit() {
    setTimeout(() => this.zone.run(() => { this.animated = true; }), 80);
  }

  private compute() {
    const total = this.total || this.segments.reduce((s, sg) => s + sg.value, 0);
    let cum = 0;
    this.computed = this.segments
      .filter(sg => sg.value > 0)
      .map(sg => {
        const pct = total > 0 ? sg.value / total : 0;
        const len = pct * this.C;
        const cs: ComputedSeg = {
          ...sg,
          percentage: pct * 100,
          dasharray: `${len.toFixed(2)} ${(this.C - len).toFixed(2)}`,
          dashoffset: -cum
        };
        cum += len;
        return cs;
      });
  }

  get totalAll(): number {
    return this.total || this.segments.reduce((s, sg) => s + sg.value, 0);
  }

  get centerCount(): string {
    return this.centerOverride || String(this.totalAll);
  }

  onSegEnter(seg: ComputedSeg, event: MouseEvent) {
    this.hovered = seg;
    this.moveTooltip(event);
  }

  onSegMove(event: MouseEvent) {
    if (this.hovered) this.moveTooltip(event);
  }

  onSegLeave() {
    this.hovered = null;
  }

  private moveTooltip(event: MouseEvent) {
    // Position tooltip 14px below and 8px right of cursor
    // Clamp so it doesn't go off right/bottom edge
    const pad = 12;
    const tipW = 160;
    const tipH = 64;
    let x = event.clientX + 8;
    let y = event.clientY + 14;
    if (x + tipW + pad > window.innerWidth)  x = event.clientX - tipW - 8;
    if (y + tipH + pad > window.innerHeight) y = event.clientY - tipH - 8;
    this.tipX = x;
    this.tipY = y;
  }
}
