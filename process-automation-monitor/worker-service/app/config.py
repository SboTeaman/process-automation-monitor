from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    # Include SSL/TLS: append ?ssl_ca=<path>&ssl_verify_cert=true for encrypted connections
    # Example: mysql+pymysql://user:password@localhost:3306/worker_db?ssl_ca=/certs/mysql-ca.crt&ssl_verify_cert=true
    DATABASE_URL: str = "mysql+pymysql://user:password@localhost:3306/worker_db?ssl_ca=${DB_SSL_CA:}"
    WORKER_TIMEOUT_DEFAULT: int = 30
    LOG_LEVEL: str = "INFO"
    SERVICE_NAME: str = "worker-service"

    # All file operations (CSV read/write, reports) are sandboxed to this directory.
    DATA_DIR: str = "/data"

    # Shared secret sent by the orchestrator in X-Worker-Api-Key header.
    WORKER_API_KEY: str = ""

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")


settings = Settings()
