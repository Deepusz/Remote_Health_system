import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiCallerService } from '../../services/apiCallerService.service';
import { Subscription } from 'rxjs';

interface Patient {
  name: string;
  status: string;
  location: string;
  lastUpdate: string;
  emergencyAlerts: number;
  battery: number;
}

interface VitalSigns {
  heartRate: number;
  temperature: number;
  spO2: number;
  fallDetected: boolean;
}

interface Alert {
  time: string;
  type: string;
  status: string;
  notes: string;
}

interface HealthMetric {
  value: number;
  timestamp: Date;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit, OnDestroy {
  private pollSub?: Subscription;

  // Patient data – start empty
  patient: Patient = {
    name: '',
    status: '',
    location: '',
    lastUpdate: '',
    emergencyAlerts: 0,
    battery: 45
  };

  // Vital signs – start empty
  vitalSigns: VitalSigns = {
    heartRate: 0,
    temperature: 0,
    spO2: 0,
    fallDetected: false
  };

  // Alert history – initially empty
  alerts: Alert[] = [];

  // Health metrics data
  healthMetrics: HealthMetric[] = [];
  selectedMetric: string = 'Heart Rate';
  selectedTimeRange: string = 'Last 24 Hours';

  // Navigation
  activeTab: string = 'Home';

  // Chart options
metricOptions: string[] = ['Heart Rate', 'SpO2', 'Temperature'];
timeRangeOptions: string[] = ['Last Hour', 'Last 24 Hours', 'Last Week', 'Last Month'];


  constructor(private router: Router, private api: ApiCallerService) {}

  ngOnInit(): void {
    // Stop any previous simulation interval if any
    this.pollSub = this.api.pollFeeds(10, 50000).subscribe({
      next: (data) => this.onFeedsReceived(data),
      error: (err) => console.error('Poll error', err)
    });
  }

  private onFeedsReceived(data: any) {
    const feeds = data?.feeds ?? data?.value ?? data;
    if (!feeds || !Array.isArray(feeds)) {
      console.warn('Unexpected feeds format', data);
      return;
    }

    const latest = feeds[feeds.length - 1];

    const hr = this.safeNumber(latest.field1, this.vitalSigns.heartRate);
    const sp = this.safeNumber(latest.field2, this.vitalSigns.spO2);
    const temp = this.safeNumber(latest.field3, this.vitalSigns.temperature);
    const fall = Boolean(Number(latest.field4 || 0));

    // Update vitals
    this.vitalSigns = { heartRate: hr, spO2: sp, temperature: temp, fallDetected: fall };

    // Update patient metadata
    this.patient.lastUpdate = latest.created_at ?? new Date().toISOString();
    this.patient.name = 'John Smith'; // if you have patient info in DB/backend, map here
    this.patient.status = 'Active'; // adjust based on backend response
    this.patient.battery = this.patient.battery > 0 ? this.patient.battery - 0.01 : 100;

    // Push into healthMetrics
    this.healthMetrics.push({
      value: hr,
      timestamp: new Date(latest.created_at ?? Date.now())
    });
    if (this.healthMetrics.length > 200) this.healthMetrics.shift();

    // Alerts
    if (fall) {
      this.alerts.unshift({
        time: this.patient.lastUpdate,
        type: 'Fall Detected',
        status: 'Pending',
        notes: 'Auto-detected from device'
      });
      this.patient.emergencyAlerts++;
    }
  }

  private safeNumber(value: any, fallback = 0): number {
    if (value === null || value === undefined) return fallback;
    const n = Number(value);
    return Number.isFinite(n) ? n : fallback;
  }

  ngOnDestroy(): void {
    if (this.pollSub) this.pollSub.unsubscribe();
  }

  onMetricChange(metric: string): void {
    this.selectedMetric = metric;
    this.healthMetrics = []; // clear and refill only from API
  }

  onTimeRangeChange(timeRange: string): void {
    this.selectedTimeRange = timeRange;
    this.healthMetrics = []; // clear and refill only from API
  }

  onTabClick(tab: string): void {
    this.activeTab = tab;
  }

  onLogout(): void {
    this.router.navigate(['/login']);
  }

  getMetricUnit(metric: string): string {
    switch (metric) {
      case 'Heart Rate':
        return 'bpm';
      case 'SpO2':
        return '%';
      case 'Temperature':
        return '°C';
      default:
        return '';
    }
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'Resolved':
        return 'success';
      case 'Pending':
        return 'warning';
      case 'Critical':
        return 'danger';
      default:
        return 'info';
    }
  }

  getAlertIcon(type: string): string {
    switch (type) {
      case 'Fall Detected':
        return '🫀';
      case 'High Temp':
        return '🌡️';
      case 'Low SpO2':
        return '💧';
      default:
        return '⚠️';
    }
  }
}
