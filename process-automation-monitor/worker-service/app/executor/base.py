from abc import ABC, abstractmethod


class BaseExecutor(ABC):
    """Abstract base class for all task executors."""

    @abstractmethod
    async def execute(self, config: dict) -> dict:
        """Execute the task with the given configuration.

        Args:
            config: Task-specific configuration dictionary.

        Returns:
            A dictionary containing the execution result.

        Raises:
            Exception: On execution failure.
        """
        pass
