CREATE TABLE jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    config JSONB,
    schedule VARCHAR(100),
    timezone VARCHAR(100) DEFAULT 'UTC',
    enabled BOOLEAN DEFAULT true,
    deleted BOOLEAN DEFAULT false,
    deleted_at TIMESTAMP,
    timeout INT DEFAULT 30,
    max_retries INT DEFAULT 3,
    notification_channel VARCHAR(50),
    notification_target VARCHAR(500),
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    last_run_at TIMESTAMP,
    last_status VARCHAR(50) DEFAULT 'PENDING'
);

CREATE INDEX idx_jobs_type ON jobs(type);
CREATE INDEX idx_jobs_created_by ON jobs(created_by);
CREATE INDEX idx_jobs_enabled ON jobs(enabled);
CREATE INDEX idx_jobs_deleted ON jobs(deleted);
CREATE INDEX idx_jobs_last_status ON jobs(last_status);
