-- ─────────────────────────────────────────────────────────────────────────────
-- Create specialized database users (principle of least privilege)
-- ─────────────────────────────────────────────────────────────────────────────

-- Orchestrator service: full write access to its tables
CREATE ROLE orchestrator_app WITH PASSWORD :'ORCH_PASSWORD' LOGIN;
GRANT CONNECT ON DATABASE orchestrator_db TO orchestrator_app;
GRANT USAGE ON SCHEMA public TO orchestrator_app;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO orchestrator_app;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO orchestrator_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO orchestrator_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO orchestrator_app;

-- Analytics service: read-only access (pulls data for ETL)
CREATE ROLE analytics_app WITH PASSWORD :'ANALYTICS_PASSWORD' LOGIN;
GRANT CONNECT ON DATABASE orchestrator_db TO analytics_app;
GRANT USAGE ON SCHEMA public TO analytics_app;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO analytics_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO analytics_app;

-- ─────────────────────────────────────────────────────────────────────────────
-- Row-Level Security (RLS) on jobs table
-- Operators can only see/manage their own jobs; admins see all.
-- ─────────────────────────────────────────────────────────────────────────────

ALTER TABLE jobs ENABLE ROW LEVEL SECURITY;

-- Policy: operators see only their jobs; admins see all
CREATE POLICY jobs_user_isolation ON jobs
  USING (
    (SELECT role FROM users WHERE id = current_user_id) = 'ADMIN'
    OR created_by_id = current_user_id
  );

-- Allow inserts only if they're creating for themselves
CREATE POLICY jobs_insert_own ON jobs
  FOR INSERT
  WITH CHECK (created_by_id = current_user_id);

-- Update only own jobs (admins can update any)
CREATE POLICY jobs_update_own ON jobs
  FOR UPDATE
  USING (
    (SELECT role FROM users WHERE id = current_user_id) = 'ADMIN'
    OR created_by_id = current_user_id
  );

-- Delete only own jobs (admins can delete any)
CREATE POLICY jobs_delete_own ON jobs
  FOR DELETE
  USING (
    (SELECT role FROM users WHERE id = current_user_id) = 'ADMIN'
    OR created_by_id = current_user_id
  );

-- Analytics cannot modify jobs (read-only)
CREATE POLICY jobs_analytics_readonly ON jobs
  FOR UPDATE
  USING (false);

CREATE POLICY jobs_analytics_readonly_delete ON jobs
  FOR DELETE
  USING (false);

-- ─────────────────────────────────────────────────────────────────────────────
-- RLS on execution_logs table
-- ─────────────────────────────────────────────────────────────────────────────

ALTER TABLE execution_logs ENABLE ROW LEVEL SECURITY;

-- Users see only logs for their jobs
CREATE POLICY execution_logs_user_isolation ON execution_logs
  USING (
    (SELECT role FROM users WHERE id = current_user_id) = 'ADMIN'
    OR job_id IN (
      SELECT id FROM jobs
      WHERE created_by_id = current_user_id
        OR (SELECT role FROM users WHERE id = current_user_id) = 'ADMIN'
    )
  );

-- Analytics can only read
CREATE POLICY execution_logs_analytics_readonly ON execution_logs
  FOR UPDATE
  USING (false);

CREATE POLICY execution_logs_analytics_readonly_delete ON execution_logs
  FOR DELETE
  USING (false);

-- ─────────────────────────────────────────────────────────────────────────────
-- RLS on users table (don't expose other users' details)
-- ─────────────────────────────────────────────────────────────────────────────

ALTER TABLE users ENABLE ROW LEVEL SECURITY;

-- Users can only see themselves; admins see all
CREATE POLICY users_self_isolation ON users
  USING (
    id = current_user_id
    OR (SELECT role FROM users WHERE id = current_user_id) = 'ADMIN'
  );

-- Users can only update themselves; admins can update any
CREATE POLICY users_update_self ON users
  FOR UPDATE
  USING (
    id = current_user_id
    OR (SELECT role FROM users WHERE id = current_user_id) = 'ADMIN'
  );

-- No deletes by regular users
CREATE POLICY users_delete_admin_only ON users
  FOR DELETE
  USING ((SELECT role FROM users WHERE id = current_user_id) = 'ADMIN');

-- ─────────────────────────────────────────────────────────────────────────────
-- Revoked tokens (RLS not needed — internal table only)
-- ─────────────────────────────────────────────────────────────────────────────
-- Keep revoked_tokens accessible only to orchestrator_app

-- ─────────────────────────────────────────────────────────────────────────────
-- Helper function: set current_user_id from JWT claim
-- ─────────────────────────────────────────────────────────────────────────────

CREATE OR REPLACE FUNCTION public.get_current_user_id() RETURNS UUID AS $$
  SELECT nullif(current_setting('app.current_user_id', true), '')::uuid;
$$ LANGUAGE sql STABLE;

-- Set app.current_user_id in your application connection (right after auth):
-- SET app.current_user_id = '<user-id-from-jwt>';
