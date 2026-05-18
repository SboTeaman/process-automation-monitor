from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )

    # Analytics MySQL (primary) — with optional SSL
    # Append: ?ssl_ca=/certs/mysql-ca.crt&ssl_verify_cert=true for encrypted connections
    ANALYTICS_DB_URL: str = "mysql+pymysql://analytics:analytics@localhost:3306/analytics?ssl_ca=${DB_ANALYTICS_SSL_CA:}"

    # Orchestrator PostgreSQL (ETL source, read-only) — with optional SSL
    # Format: postgresql://user:password@host/db?sslmode=require&sslrootcert=/certs/postgres.crt
    ORCHESTRATOR_DB_URL: str = "postgresql://postgres:postgres@localhost:5432/orchestrator?sslmode=${DB_ORCH_SSL_MODE:prefer}"

    SERVICE_NAME: str = "analytics-service"
    LOG_LEVEL: str = "INFO"

    # Shared secret sent by the orchestrator in X-Analytics-Api-Key header.
    ANALYTICS_API_KEY: str = ""


settings = Settings()
