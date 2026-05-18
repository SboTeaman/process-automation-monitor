"""create analytics tables

Revision ID: 001
Revises:
Create Date: 2024-01-01 00:00:00.000000
"""
from alembic import op
import sqlalchemy as sa

revision = "001"
down_revision = None
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "daily_stats",
        sa.Column("id", sa.Integer(), autoincrement=True, nullable=False),
        sa.Column("date", sa.Date(), nullable=False),
        sa.Column("total_runs", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("success_count", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("fail_count", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("avg_duration_ms", sa.BigInteger(), nullable=True),
        sa.Column("calculated_at", sa.DateTime(), nullable=True),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("date"),
    )
    op.create_index("idx_daily_stats_date", "daily_stats", ["date"])

    op.create_table(
        "job_performance",
        sa.Column("id", sa.Integer(), autoincrement=True, nullable=False),
        sa.Column("job_id", sa.String(36), nullable=False),
        sa.Column("job_name", sa.String(255), nullable=False),
        sa.Column("avg_duration_ms", sa.BigInteger(), nullable=True),
        sa.Column("success_rate", sa.Numeric(5, 2), nullable=True),
        sa.Column("total_runs", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("last_calculated_at", sa.DateTime(), nullable=True),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("idx_job_performance_job_id", "job_performance", ["job_id"])


def downgrade() -> None:
    op.drop_index("idx_job_performance_job_id", table_name="job_performance")
    op.drop_table("job_performance")
    op.drop_index("idx_daily_stats_date", table_name="daily_stats")
    op.drop_table("daily_stats")
