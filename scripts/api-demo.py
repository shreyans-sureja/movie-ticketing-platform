#!/usr/bin/env python3
"""Run a self-checking end-to-end demonstration against a running API."""

from __future__ import annotations

import argparse
import json
import os
import sys
import threading
import time
import uuid
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from typing import Any, Callable, Iterable
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen


class DemoFailure(RuntimeError):
    """Raised when an API response violates a demonstrated invariant."""


@dataclass(frozen=True)
class ApiResponse:
    status: int
    headers: dict[str, str]
    body: Any
    text: str


_NO_BODY = object()


class ApiClient:
    def __init__(self, base_url: str, timeout_seconds: float) -> None:
        self.base_url = base_url.rstrip("/")
        self.timeout_seconds = timeout_seconds

    def request(
        self,
        method: str,
        path: str,
        *,
        token: str | None = None,
        body: Any = _NO_BODY,
        raw_body: str | None = None,
    ) -> ApiResponse:
        headers = {"Accept": "application/json"}
        if token:
            headers["Authorization"] = f"Bearer {token}"

        data: bytes | None = None
        if body is not _NO_BODY:
            headers["Content-Type"] = "application/json"
            data = json.dumps(body).encode("utf-8")
        elif raw_body is not None:
            headers["Content-Type"] = "application/json"
            data = raw_body.encode("utf-8")

        request = Request(
            f"{self.base_url}{path}",
            data=data,
            headers=headers,
            method=method,
        )
        try:
            with urlopen(request, timeout=self.timeout_seconds) as response:
                return self._read_response(response.status, response.headers, response.read())
        except HTTPError as error:
            return self._read_response(error.code, error.headers, error.read())
        except URLError as error:
            raise DemoFailure(
                f"Cannot reach {self.base_url}. Start the application first or pass --base-url. "
                f"Underlying error: {error.reason}"
            ) from error

    @staticmethod
    def _read_response(status: int, headers: Any, payload: bytes) -> ApiResponse:
        text = payload.decode("utf-8", errors="replace")
        parsed: Any = None
        if text:
            try:
                parsed = json.loads(text)
            except json.JSONDecodeError:
                parsed = text
        return ApiResponse(
            status=status,
            headers={key.lower(): value for key, value in headers.items()},
            body=parsed,
            text=text,
        )


class Reporter:
    def __init__(self) -> None:
        self.step_number = 0
        self.assertion_count = 0

    def step(self, title: str) -> None:
        self.step_number += 1
        print(f"\n[{self.step_number:02d}] {title}")

    def passed(self, message: str) -> None:
        self.assertion_count += 1
        print(f"     PASS  {message}")


def response_summary(response: ApiResponse) -> str:
    rendered = response.text.strip()
    if len(rendered) > 700:
        rendered = rendered[:700] + "..."
    return f"status={response.status}, body={rendered or '<empty>'}"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise DemoFailure(message)


def expect_status(
    reporter: Reporter,
    response: ApiResponse,
    expected: int | Iterable[int],
    message: str,
) -> ApiResponse:
    statuses = {expected} if isinstance(expected, int) else set(expected)
    require(
        response.status in statuses,
        f"{message}: expected HTTP {sorted(statuses)}, {response_summary(response)}",
    )
    reporter.passed(f"{message} (HTTP {response.status})")
    return response


def expect_problem(
    reporter: Reporter,
    response: ApiResponse,
    status: int,
    code: str,
    message: str,
) -> ApiResponse:
    expect_status(reporter, response, status, message)
    require(isinstance(response.body, dict), f"{message}: response is not a JSON object")
    require(
        response.body.get("code") == code,
        f"{message}: expected problem code {code}, {response_summary(response)}",
    )
    content_type = response.headers.get("content-type", "")
    require(
        content_type.startswith("application/problem+json"),
        f"{message}: expected application/problem+json, got {content_type!r}",
    )
    reporter.passed(f"{message} uses problem code {code}")
    return response


def race_requests(
    calls: list[tuple[str, Callable[[], ApiResponse]]],
) -> list[tuple[str, ApiResponse]]:
    barrier = threading.Barrier(len(calls))

    def invoke(name: str, call: Callable[[], ApiResponse]) -> tuple[str, ApiResponse]:
        barrier.wait(timeout=10)
        return name, call()

    with ThreadPoolExecutor(max_workers=len(calls), thread_name_prefix="api-demo") as executor:
        futures = [executor.submit(invoke, name, call) for name, call in calls]
        return [future.result(timeout=30) for future in futures]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Exercise important movie-ticketing API behavior against a running application."
    )
    parser.add_argument(
        "--base-url",
        default=os.environ.get("BASE_URL", "http://localhost:8080"),
        help="Application base URL (default: BASE_URL or http://localhost:8080).",
    )
    parser.add_argument(
        "--timeout",
        type=float,
        default=10.0,
        help="Timeout in seconds for each HTTP request (default: 10).",
    )
    return parser.parse_args()


def run_demo(client: ApiClient, reporter: Reporter) -> dict[str, str]:
    run_id = f"{datetime.now(timezone.utc):%Y%m%d%H%M%S}-{uuid.uuid4().hex[:6]}"
    password = "DemoPassword!12345"

    reporter.step("Public catalogue and standard error responses")
    cities = expect_status(
        reporter,
        client.request("GET", "/api/v1/cities"),
        200,
        "Unauthenticated users can list cities",
    ).body
    require(isinstance(cities, dict) and cities.get("items"), "City catalogue returned no items")
    city_id = cities["items"][0]["id"]

    malformed = client.request(
        "POST",
        "/api/v1/auth/customers/signup",
        raw_body="{not-valid-json",
    )
    expect_problem(
        reporter,
        malformed,
        400,
        "MALFORMED_REQUEST",
        "Malformed JSON has the standard error representation",
    )

    reporter.step("Open signup, single sign-in flow, and role-bearing JWTs")
    accounts: dict[str, dict[str, str]] = {}

    def register_and_sign_in(name: str, signup_path: str, expected_role: str) -> None:
        email = f"{name}-{run_id}@example.com"
        signup = expect_status(
            reporter,
            client.request(
                "POST",
                signup_path,
                body={"email": email, "password": password},
            ),
            201,
            f"{name} signup is open",
        )
        signin = expect_status(
            reporter,
            client.request(
                "POST",
                "/api/v1/auth/signin",
                body={"email": email, "password": password},
            ),
            200,
            f"{name} signs in through the shared endpoint",
        )
        require(signin.body["account"]["role"] == expected_role, f"{name} received the wrong role")
        require(signin.body["account"]["id"] == signup.body["id"], f"{name} account ID changed")
        accounts[name] = {
            "id": signup.body["id"],
            "token": signin.body["accessToken"],
            "role": expected_role,
        }
        reporter.passed(f"{name} JWT identifies role {expected_role}")

    register_and_sign_in("admin-one", "/api/v1/auth/admins/signup", "THEATRE_ADMIN")
    register_and_sign_in("admin-two", "/api/v1/auth/admins/signup", "THEATRE_ADMIN")
    register_and_sign_in("customer-one", "/api/v1/auth/customers/signup", "CUSTOMER")
    register_and_sign_in("customer-two", "/api/v1/auth/customers/signup", "CUSTOMER")

    admin_one = accounts["admin-one"]
    admin_two = accounts["admin-two"]
    customer_one = accounts["customer-one"]
    customer_two = accounts["customer-two"]

    reporter.step("Role-based access control and theatre ownership")
    theatre_payload = {
        "cityId": city_id,
        "name": f"API Demo Theatre {run_id}",
        "addressLine1": "100 Demo Road",
        "addressLine2": "Concurrency District",
        "postalCode": "560001",
    }
    expect_problem(
        reporter,
        client.request("POST", "/api/v1/theatres", body=theatre_payload),
        401,
        "INVALID_TOKEN",
        "Anonymous theatre creation is rejected",
    )
    expect_problem(
        reporter,
        client.request(
            "POST",
            "/api/v1/theatres",
            token=customer_one["token"],
            body=theatre_payload,
        ),
        403,
        "FORBIDDEN",
        "Customers cannot create theatres",
    )
    theatre = expect_status(
        reporter,
        client.request(
            "POST",
            "/api/v1/theatres",
            token=admin_one["token"],
            body=theatre_payload,
        ),
        201,
        "Theatre admins can create theatres",
    ).body
    theatre_id = theatre["id"]

    owner_list = expect_status(
        reporter,
        client.request("GET", "/api/v1/theatres", token=admin_two["token"]),
        200,
        "A second admin can list only owned theatres",
    ).body
    require(
        theatre_id not in {item["id"] for item in owner_list["items"]},
        "The second admin can see another admin's theatre",
    )
    reporter.passed("Another admin's theatre is absent from the owned list")

    auditorium_path = f"/api/v1/theatres/{theatre_id}/auditoriums"
    expect_problem(
        reporter,
        client.request(
            "POST",
            auditorium_path,
            token=admin_two["token"],
            body={"name": "Unauthorized Screen"},
        ),
        404,
        "THEATRE_NOT_FOUND",
        "Another admin cannot mutate the theatre",
    )
    auditorium = expect_status(
        reporter,
        client.request(
            "POST",
            auditorium_path,
            token=admin_one["token"],
            body={"name": "Screen One"},
        ),
        201,
        "The owning admin can create an auditorium",
    ).body
    auditorium_id = auditorium["id"]

    seat_row_path = (
        f"/api/v1/theatres/{theatre_id}/auditoriums/{auditorium_id}/seat-rows"
    )
    for row_label, tier in (("A", "REGULAR"), ("B", "PREMIUM")):
        expect_status(
            reporter,
            client.request(
                "POST",
                seat_row_path,
                token=admin_one["token"],
                body={
                    "rowLabel": row_label,
                    "firstSeatNumber": 1,
                    "seatCount": 4,
                    "tier": tier,
                },
            ),
            201,
            f"Owner creates physical seat row {row_label} ({tier})",
        )

    physical_seats = expect_status(
        reporter,
        client.request(
            "GET",
            f"/api/v1/theatres/{theatre_id}/auditoriums/{auditorium_id}/seats",
            token=admin_one["token"],
        ),
        200,
        "Owner can list physical seats",
    ).body["items"]
    require(len(physical_seats) == 8, f"Expected 8 physical seats, got {len(physical_seats)}")
    require(all(item["seatLabel"] == f'{item["rowLabel"]}{item["seatNumber"]}' for item in physical_seats),
            "Physical seat labels are not derived consistently")
    reporter.passed("Physical seat labels are derived and both tiers are present")

    reporter.step("Movie catalogue access and show scheduling correctness")
    movie_payload = {
        "title": f"API Demo Movie {run_id}",
        "durationMinutes": 120,
        "languageCode": "en",
    }
    expect_problem(
        reporter,
        client.request(
            "POST",
            "/api/v1/movies",
            token=customer_one["token"],
            body=movie_payload,
        ),
        403,
        "FORBIDDEN",
        "Customers cannot create movies",
    )
    movie = expect_status(
        reporter,
        client.request(
            "POST",
            "/api/v1/movies",
            token=admin_one["token"],
            body=movie_payload,
        ),
        201,
        "Theatre admins can add movies",
    ).body
    movie_id = movie["id"]
    expect_status(
        reporter,
        client.request("GET", f"/api/v1/movies/{movie_id}"),
        200,
        "Movie detail is public",
    )

    starts_at = (datetime.now(timezone.utc) + timedelta(days=2)).replace(microsecond=0)
    starts_at_text = starts_at.isoformat().replace("+00:00", "Z")
    show_payload = {
        "movieId": movie_id,
        "startsAt": starts_at_text,
        "currency": "INR",
        "tierPrices": [
            {"tier": "REGULAR", "amount": 250.00},
            {"tier": "PREMIUM", "amount": 400.00},
        ],
    }
    show_path = f"/api/v1/theatres/{theatre_id}/auditoriums/{auditorium_id}/shows"

    missing_price_payload = dict(show_payload)
    missing_price_payload["tierPrices"] = [{"tier": "REGULAR", "amount": 250.00}]
    expect_problem(
        reporter,
        client.request(
            "POST",
            show_path,
            token=admin_one["token"],
            body=missing_price_payload,
        ),
        400,
        "TIER_PRICE_MISMATCH",
        "Every physical-seat tier requires a show price",
    )
    expect_problem(
        reporter,
        client.request(
            "POST",
            show_path,
            token=admin_two["token"],
            body=show_payload,
        ),
        404,
        "THEATRE_NOT_FOUND",
        "An admin cannot schedule a show in another admin's theatre",
    )
    expect_problem(
        reporter,
        client.request("POST", show_path, token=customer_one["token"], body=show_payload),
        403,
        "FORBIDDEN",
        "Customers cannot schedule shows",
    )

    show_race = race_requests([
        (
            "schedule-one",
            lambda: client.request(
                "POST", show_path, token=admin_one["token"], body=show_payload
            ),
        ),
        (
            "schedule-two",
            lambda: client.request(
                "POST", show_path, token=admin_one["token"], body=show_payload
            ),
        ),
    ])
    require(
        sorted(response.status for _, response in show_race) == [201, 409],
        "Concurrent overlapping shows did not produce exactly one winner: "
        + ", ".join(f"{name}={response.status}" for name, response in show_race),
    )
    winning_show = next(response for _, response in show_race if response.status == 201)
    rejected_show = next(response for _, response in show_race if response.status == 409)
    expect_problem(
        reporter,
        rejected_show,
        409,
        "SHOW_TIME_CONFLICT",
        "Concurrent overlapping show creation has one rejected request",
    )
    show_id = winning_show.body["id"]
    reporter.passed("Auditorium locking permits exactly one overlapping show")

    reporter.step("Public show discovery and response privacy")
    public_show = expect_status(
        reporter,
        client.request("GET", f"/api/v1/shows/{show_id}"),
        200,
        "Show detail is public",
    ).body
    require(public_show["totalSeatCount"] == 8, "Show inventory did not snapshot all physical seats")
    require(public_show["availableSeatCount"] == 8, "New show does not start fully available")
    reporter.passed("Show-specific inventory contains all 8 seat snapshots")

    kolkata = timezone(timedelta(hours=5, minutes=30))
    customer_date = starts_at.astimezone(kolkata).date().isoformat()
    search_path = "/api/v1/shows?" + urlencode(
        {"cityId": city_id, "movieId": movie_id, "date": customer_date}
    )
    search = expect_status(
        reporter,
        client.request("GET", search_path),
        200,
        "Unauthenticated users can search upcoming shows",
    ).body
    require(show_id in {item["id"] for item in search["items"]}, "Created show is absent from search")

    seat_list = expect_status(
        reporter,
        client.request("GET", f"/api/v1/shows/{show_id}/seats"),
        200,
        "Show-seat availability is public",
    ).body["items"]
    require(len(seat_list) == 8, f"Expected 8 show seats, got {len(seat_list)}")
    require(all("physicalSeatId" not in seat for seat in seat_list), "Public response leaks physicalSeatId")
    reporter.passed("Public seats expose showSeatId but not physicalSeatId")

    missing_date = client.request(
        "GET",
        "/api/v1/shows?" + urlencode({"cityId": city_id, "movieId": movie_id}),
    )
    expect_problem(
        reporter,
        missing_date,
        400,
        "VALIDATION_FAILED",
        "Missing query parameters use the standard validation response",
    )

    show_seat_ids = [seat["showSeatId"] for seat in seat_list]
    hold_path = f"/api/v1/shows/{show_id}/holds"
    expect_problem(
        reporter,
        client.request(
            "POST",
            hold_path,
            token=admin_one["token"],
            body={"showSeatIds": [show_seat_ids[0]]},
        ),
        403,
        "FORBIDDEN",
        "Theatre admins cannot create customer holds",
    )
    expect_problem(
        reporter,
        client.request("POST", hold_path, body={"showSeatIds": [show_seat_ids[0]]}),
        401,
        "INVALID_TOKEN",
        "Anonymous users cannot create holds",
    )
    duplicate_ids = client.request(
        "POST",
        hold_path,
        token=customer_one["token"],
        body={"showSeatIds": [show_seat_ids[2], show_seat_ids[2]]},
    )
    expect_problem(
        reporter,
        duplicate_ids,
        400,
        "VALIDATION_FAILED",
        "Duplicate seat IDs are rejected before acquisition",
    )

    reporter.step("Concurrent hold acquisition and all-or-nothing semantics")
    customers = {
        "customer-one": customer_one,
        "customer-two": customer_two,
    }
    hold_race = race_requests([
        (
            name,
            lambda account=account: client.request(
                "POST",
                hold_path,
                token=account["token"],
                body={"showSeatIds": [show_seat_ids[0]]},
            ),
        )
        for name, account in customers.items()
    ])
    require(
        sorted(response.status for _, response in hold_race) == [201, 409],
        "Concurrent hold requests did not produce exactly one winner: "
        + ", ".join(f"{name}={response.status}" for name, response in hold_race),
    )
    winner_name, winning_hold = next(item for item in hold_race if item[1].status == 201)
    loser_name, losing_hold = next(item for item in hold_race if item[1].status == 409)
    expect_problem(
        reporter,
        losing_hold,
        409,
        "SEATS_UNAVAILABLE",
        "One overlapping hold request loses cleanly",
    )
    reporter.passed(f"PostgreSQL locking selected one hold winner ({winner_name})")
    winner = customers[winner_name]
    loser = customers[loser_name]
    hold_id = winning_hold.body["id"]

    expect_problem(
        reporter,
        client.request("GET", f"/api/v1/holds/{hold_id}", token=loser["token"]),
        404,
        "HOLD_NOT_FOUND",
        "Another customer cannot read the winning hold",
    )
    expect_status(
        reporter,
        client.request("GET", f"/api/v1/holds/{hold_id}", token=winner["token"]),
        200,
        "The winning customer can read the hold",
    )

    expect_problem(
        reporter,
        client.request(
            "POST",
            hold_path,
            token=loser["token"],
            body={"showSeatIds": [show_seat_ids[0], show_seat_ids[1]]},
        ),
        409,
        "SEATS_UNAVAILABLE",
        "A mixed available/unavailable hold fails as one unit",
    )
    loser_hold = expect_status(
        reporter,
        client.request(
            "POST",
            hold_path,
            token=loser["token"],
            body={"showSeatIds": [show_seat_ids[1]]},
        ),
        201,
        "The previously free seat remained available after the failed multi-seat hold",
    ).body
    require(loser_hold["seats"][0]["showSeatId"] == show_seat_ids[1], "Wrong seat was held")
    availability = client.request("GET", f"/api/v1/shows/{show_id}").body
    require(availability["availableSeatCount"] == 6, "Two active holds should leave six available seats")
    reporter.passed("Availability aggregation reflects both active holds")

    reporter.step("Concurrent confirmation, retry idempotency, and ownership hiding")
    booking_path = f"/api/v1/holds/{hold_id}/booking"
    booking_race = race_requests([
        (
            "confirmation-one",
            lambda: client.request("POST", booking_path, token=winner["token"]),
        ),
        (
            "confirmation-two",
            lambda: client.request("POST", booking_path, token=winner["token"]),
        ),
    ])
    require(
        sorted(response.status for _, response in booking_race) == [200, 201],
        "Concurrent confirmation should create once and replay once: "
        + ", ".join(f"{name}={response.status}" for name, response in booking_race),
    )
    booking_ids = {response.body["id"] for _, response in booking_race}
    require(len(booking_ids) == 1, "Concurrent confirmation returned different booking IDs")
    booking_id = booking_ids.pop()
    confirmed_at_values = {response.body["confirmedAt"] for _, response in booking_race}
    require(len(confirmed_at_values) == 1, "Concurrent confirmation changed confirmedAt")
    confirmed_at = confirmed_at_values.pop()
    reporter.passed("Concurrent confirmation produced one booking and one idempotent replay")

    serial_retry = expect_status(
        reporter,
        client.request("POST", booking_path, token=winner["token"]),
        200,
        "A later confirmation retry returns the existing booking",
    ).body
    require(serial_retry["id"] == booking_id, "Confirmation retry returned a different booking")
    require(serial_retry["confirmedAt"] == confirmed_at, "Confirmation retry changed confirmedAt")
    reporter.passed("Confirmation retry preserves booking identity and timestamp")

    converted_hold = client.request("GET", f"/api/v1/holds/{hold_id}", token=winner["token"])
    require(converted_hold.body["status"] == "CONVERTED", "Confirmed hold is not reported as CONVERTED")
    reporter.passed("The source hold is reported as CONVERTED")

    history = expect_status(
        reporter,
        client.request("GET", "/api/v1/bookings", token=winner["token"]),
        200,
        "Customer can list owned booking history",
    ).body
    matching_history = [item for item in history["items"] if item["id"] == booking_id]
    require(len(matching_history) == 1, "Idempotent confirmation created duplicate history entries")
    reporter.passed("Booking history contains exactly one entry for all confirmation attempts")

    expect_problem(
        reporter,
        client.request("GET", f"/api/v1/bookings/{booking_id}", token=loser["token"]),
        404,
        "BOOKING_NOT_FOUND",
        "Another customer cannot read the booking",
    )
    expect_problem(
        reporter,
        client.request(
            "POST",
            f"/api/v1/bookings/{booking_id}/cancellation",
            token=loser["token"],
        ),
        404,
        "BOOKING_NOT_FOUND",
        "Another customer cannot cancel the booking",
    )
    expect_problem(
        reporter,
        client.request("GET", "/api/v1/bookings", token=admin_one["token"]),
        403,
        "FORBIDDEN",
        "Theatre admins cannot access customer booking history",
    )

    reporter.step("Concurrent cancellation, idempotent retries, and seat release")
    cancellation_path = f"/api/v1/bookings/{booking_id}/cancellation"
    cancellation_race = race_requests([
        (
            "cancellation-one",
            lambda: client.request("POST", cancellation_path, token=winner["token"]),
        ),
        (
            "cancellation-two",
            lambda: client.request("POST", cancellation_path, token=winner["token"]),
        ),
    ])
    require(
        all(response.status == 200 for _, response in cancellation_race),
        "Concurrent cancellation did not return 200 for both requests: "
        + ", ".join(f"{name}={response.status}" for name, response in cancellation_race),
    )
    require(
        {response.body["id"] for _, response in cancellation_race} == {booking_id},
        "Concurrent cancellation returned different bookings",
    )
    cancelled_at_values = {response.body["cancelledAt"] for _, response in cancellation_race}
    require(None not in cancelled_at_values and len(cancelled_at_values) == 1,
            "Concurrent cancellation produced inconsistent cancellation timestamps")
    cancelled_at = cancelled_at_values.pop()
    require(
        all(response.body["status"] == "CANCELLED" for _, response in cancellation_race),
        "Concurrent cancellation did not leave the booking CANCELLED",
    )
    reporter.passed("Concurrent cancellation performs one stable state transition")

    cancellation_retry = expect_status(
        reporter,
        client.request("POST", cancellation_path, token=winner["token"]),
        200,
        "A later cancellation retry returns the cancelled booking",
    ).body
    require(cancellation_retry["cancelledAt"] == cancelled_at, "Cancellation retry changed cancelledAt")
    reporter.passed("Cancellation retry preserves the original cancellation timestamp")

    released_seats = client.request("GET", f"/api/v1/shows/{show_id}/seats").body["items"]
    released = next(seat for seat in released_seats if seat["showSeatId"] == show_seat_ids[0])
    require(released["availability"] == "AVAILABLE", "Cancelled booking did not release its seat")
    after_cancel_show = client.request("GET", f"/api/v1/shows/{show_id}").body
    require(after_cancel_show["availableSeatCount"] == 7,
            "Cancellation should restore one seat while the other hold remains active")
    reporter.passed("Cancellation immediately restores effective public availability")

    reheld = expect_status(
        reporter,
        client.request(
            "POST",
            hold_path,
            token=loser["token"],
            body={"showSeatIds": [show_seat_ids[0]]},
        ),
        201,
        "Another customer can hold the seat released by cancellation",
    ).body
    require(reheld["seats"][0]["showSeatId"] == show_seat_ids[0], "Released seat was not re-held")

    final_booking = client.request("GET", f"/api/v1/bookings/{booking_id}", token=winner["token"])
    require(final_booking.status == 200 and final_booking.body["status"] == "CANCELLED",
            "Booking detail did not preserve cancelled history")
    require(final_booking.body["confirmedAt"] == confirmed_at, "Cancellation changed confirmation history")
    reporter.passed("Cancelled booking detail preserves original confirmation history")

    reporter.step("Five-second hold expiry, lazy availability, and expired confirmation")
    expiry_seat_id = show_seat_ids[2]
    expiry_hold = expect_status(
        reporter,
        client.request(
            "POST",
            hold_path,
            token=winner["token"],
            body={"showSeatIds": [expiry_seat_id]},
        ),
        201,
        "Customer creates a hold on an otherwise unused seat",
    ).body
    expiry_hold_id = expiry_hold["id"]
    created_at = datetime.fromisoformat(expiry_hold["createdAt"].replace("Z", "+00:00"))
    expires_at = datetime.fromisoformat(expiry_hold["expiresAt"].replace("Z", "+00:00"))
    configured_duration = (expires_at - created_at).total_seconds()
    require(
        configured_duration == 5,
        "Expiry scenario requires BOOKING_HOLD_DURATION=PT5S; "
        f"the API returned a {configured_duration:g}-second hold",
    )
    reporter.passed("The API is configured with BOOKING_HOLD_DURATION=PT5S")

    held_seats = client.request("GET", f"/api/v1/shows/{show_id}/seats").body["items"]
    expiry_seat = next(seat for seat in held_seats if seat["showSeatId"] == expiry_seat_id)
    require(expiry_seat["availability"] == "HELD", "New expiry-test seat is not HELD")
    reporter.passed("The unused show seat is HELD before expiry")

    wait_seconds = max(0.0, (expires_at - datetime.now(timezone.utc)).total_seconds()) + 0.25
    print(f"     WAIT  {wait_seconds:.2f}s for the hold to cross its expiry boundary")
    time.sleep(wait_seconds)

    expired_hold = expect_status(
        reporter,
        client.request("GET", f"/api/v1/holds/{expiry_hold_id}", token=winner["token"]),
        200,
        "Original hold remains readable after expiry",
    ).body
    require(expired_hold["status"] == "EXPIRED", "Original hold is not reported as EXPIRED")
    reporter.passed("Original hold status is derived as EXPIRED")

    seats_after_expiry = client.request("GET", f"/api/v1/shows/{show_id}/seats").body["items"]
    available_again = next(
        seat for seat in seats_after_expiry if seat["showSeatId"] == expiry_seat_id
    )
    require(
        available_again["availability"] == "AVAILABLE",
        "Expired hold did not make the seat effectively AVAILABLE",
    )
    reporter.passed("The expired hold makes the seat publicly AVAILABLE without cleanup")

    replacement_hold = expect_status(
        reporter,
        client.request(
            "POST",
            hold_path,
            token=loser["token"],
            body={"showSeatIds": [expiry_seat_id]},
        ),
        201,
        "Another customer can hold the seat after the original hold expires",
    ).body
    require(
        replacement_hold["seats"][0]["showSeatId"] == expiry_seat_id,
        "Replacement hold acquired the wrong seat",
    )

    expect_problem(
        reporter,
        client.request(
            "POST",
            f"/api/v1/holds/{expiry_hold_id}/booking",
            token=winner["token"],
        ),
        409,
        "HOLD_EXPIRED",
        "The original expired hold cannot be confirmed",
    )
    history_after_expiry = client.request("GET", "/api/v1/bookings", token=winner["token"])
    require(history_after_expiry.status == 200, "Could not verify booking history after expiry")
    require(
        [item["id"] for item in history_after_expiry.body["items"]].count(booking_id) == 1
        and history_after_expiry.body["totalElements"] == 1,
        "Expired confirmation created an unexpected booking",
    )
    reporter.passed("Expired confirmation creates no additional booking")

    return {
        "run_id": run_id,
        "theatre_id": theatre_id,
        "auditorium_id": auditorium_id,
        "movie_id": movie_id,
        "show_id": show_id,
        "booking_id": booking_id,
        "winner": winner_name,
        "assertions": str(reporter.assertion_count),
    }


def main() -> int:
    args = parse_args()
    reporter = Reporter()
    client = ApiClient(args.base_url, args.timeout)

    print("Movie Ticketing Platform - API Behavior Demo")
    print(f"Target: {client.base_url}")
    print("This command creates uniquely named demo records; APIs currently provide no cleanup operation.")

    try:
        result = run_demo(client, reporter)
    except DemoFailure as failure:
        print(f"\nDEMO FAILED: {failure}", file=sys.stderr)
        return 1
    except Exception as failure:  # Keep unexpected script errors distinguishable from failed assertions.
        print(f"\nDEMO ERROR: {type(failure).__name__}: {failure}", file=sys.stderr)
        return 2

    print("\nDEMO PASSED")
    print(f"Assertions: {result['assertions']}")
    print(f"Run ID:     {result['run_id']}")
    print(f"Show ID:    {result['show_id']}")
    print(f"Booking ID: {result['booking_id']}")
    print(f"Hold winner: {result['winner']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
