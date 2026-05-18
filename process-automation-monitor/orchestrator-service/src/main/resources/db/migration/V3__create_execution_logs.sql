CREATE TABLE execution_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID REFERENCES jobs(id),
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP,
    status VARCHAR(50) NOT NULL,
    output TEXT,
    error_message TEXT,
    attempt INT DEFAULT 1,
    correlation_id VARCHAR(36)
);

CREATE INDEX idx_execution_logs_job_id ON execution_logs(job_id);
CREATE INDEX idx_execution_logs_status ON execution_logs(status);
CREATE INDEX idx_execution_logs_started_at ON execution_logs(started_at);
