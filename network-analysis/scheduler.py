"""
scheduler.py - convenience long-running wrapper around run_analysis.run().

This is the ONLY stateful part of the whole network-analysis component (it
remembers "when did I last run"), and even that state lives purely in this
loop's memory - each individual analysis run itself is still fully
stateless (see run_analysis.py). If this process dies, restarting it just
resumes polling; no run-in-progress state is lost since a run either
completes and writes its rows, or fails and marks itself FAILED.

Every POLL_INTERVAL_SECONDS it:
  1. Checks network_run_requests for a PENDING row (operator clicked
     "Run Analysis Now" in the UI) - if found, runs immediately with that
     request's lookback_days and marks the request DONE.
  2. Otherwise, runs on its own schedule if SCHEDULED_INTERVAL_MINUTES have
     elapsed since the last run.

For a simpler setup that doesn't need a long-running process at all, skip
this file entirely and just schedule `python run_analysis.py` directly via
Windows Task Scheduler / cron - functionally equivalent, minus the
"Run Analysis Now" button's near-immediate pickup.
"""
from __future__ import annotations

import time
from datetime import datetime, timedelta

import config
import db
import run_analysis


def main() -> None:
    engine = db.get_engine()
    last_scheduled_run: datetime | None = None

    print(f"[network-analysis] scheduler started - polling every {config.POLL_INTERVAL_SECONDS}s, "
          f"scheduled interval {config.SCHEDULED_INTERVAL_MINUTES}min")

    while True:
        try:
            pending = db.fetch_pending_run_request(engine)
            if pending is not None:
                print(f"[network-analysis] picked up manual run request id={pending['id']}")
                run_analysis.run(lookback_days=pending["lookback_days"], trigger_type="MANUAL")
                db.mark_run_request_done(engine, pending["id"])
                last_scheduled_run = datetime.utcnow()
            elif last_scheduled_run is None or \
                    datetime.utcnow() - last_scheduled_run >= timedelta(minutes=config.SCHEDULED_INTERVAL_MINUTES):
                run_analysis.run(lookback_days=config.DEFAULT_LOOKBACK_DAYS, trigger_type="SCHEDULED")
                last_scheduled_run = datetime.utcnow()
        except Exception as exc:  # noqa: BLE001 - a single bad run must not kill the scheduler loop
            print(f"[network-analysis] scheduler iteration error (will retry next poll): {exc}")

        time.sleep(config.POLL_INTERVAL_SECONDS)


if __name__ == "__main__":
    main()
