-- 工单 SLA 管理：添加 sla_breached 标记
ALTER TABLE anomaly_work_orders ADD COLUMN sla_breached BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_anomaly_wo_sla_breached ON anomaly_work_orders(sla_breached) WHERE sla_breached = TRUE;
