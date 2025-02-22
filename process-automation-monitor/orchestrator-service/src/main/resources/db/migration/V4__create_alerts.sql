CREATE TABLE alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID REFERENCES jobs(id),
    triggered_at TIMESTAMP NOT NULL,
    reason TEXT,
    severity VARCHAR(50) DEFAULT 'ERROR',
    acknowledged BOOLEAN DEFAULT false,
    acknowledged_at TIMESTAMP,
    acknowledged_by UUID REFERENCES users(id)
);

CREATE INDEX idx_alerts_job_id ON alerts(job_id);
CREATE INDEX idx_alerts_acknowledged ON alerts(acknowledged);
CREATE INDEX idx_alerts_triggered_at ON alerts(triggered_at);
