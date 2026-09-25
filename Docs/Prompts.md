# Prompts

## Prompt 1

> I'm starting this project of movie-ticketing platform. it is inside this repo. Please read assignment PDF and create only below files and structure for now.
>
> 1. AGENTS.MD - Use java 21, Spring boot, Maven, Postgres, Flyway, and other standard, keep the architecture monolith.
> 2. Prompts.MD - create a simple md file and record only all the prompts used during development.
> 3. README.md - add the project description, local setup, running tests, scope, assumptions and API documentation.
>
> Do not implement anything yet. Add just repo template code etc.

## Prompt 2

> Do not assume any design yet, You can remove design from readme file. We will iterate through step by step design iterations. Also put AGENTS.md , Prompts.md file in Docs folder.

## Prompt 3

> Let's design identity flow. create a design doc in docs folder under subfolder of designs and let's put scope,  HLD, LLD, schemas, API contracts, Diagram, etc. in that document.  Do not code it. I'll review it first.
>
> Idea is to create signing and signup flow for customers and admin users. For now we will go with JWT based simple authentication mechanism only. But we will write code in such a way that other mechanisms are easily pluggable.

## Prompt 4

> Let's make some modification in design.
>
> - let's have single user account table for both customer and admins, you misunderstood admin, here admin means theatre admins not the whole system admins we are taking about. For simplicity, keep the signup flow open for admin and use same table with roles, put separate table as out of scope for now.
> - store password hash in same table, no need for separate local creds table
> - create separate signup endpoint for customer and admin
> - remove invite flow for admin
> - use single singin endpoint, issued JWT token should contain account id and role details.
>
> update design doc for now.

## Prompt 5

> Design looks good, implement it. update all related documentations as well.

## Prompt 6

> It looks good, let's implemented admin management flow.
>
> For simplicity, add top 50 cities in India using flyway in city table. Admin should be able to select cities from that list only. Provide read only apis to admin users.
>
> Theatre admin should be able to create a theatre using the cityId, add auditoriam in that and add physical seats in that. Only the admin who owns the theatre and its data should be able to modify the data.
>
> create this design document in new file and do not code yet.

## Prompt 7

> I want you to modify plan.
>
> - Theatre_admin is the new role, please keep the consistancy with roles using admin and customer as previously we have added.
> - City you should ingest using the flyway for now but we do not need unnecessary census, slug etc data, remove and keep related information only.
> - resource related urls should not have admin in urls, protect operations using sprint security and jwt verification.
> - admin should create and list owned theatres, list owned auditorium, create physical seats, list physical seat, etc.
> - remove update apis, deactivation apis for now, put it in out of scope. our core focus right now is concurrency booking correctness.
> - Do not persist derived attributes like seatLabel, store only row lebel and seatnumber, generate and send in api response.
> - ACCESSIBLE is not a seat tier, seat can be PREMIUM and ACCESSIBLE both, leave it for now, we will add it via some flag later.
> - weekend pricing should not be part of physical seat model, it will be part of show pricing, we will implement it later.
> - modify seat row endpoint, it can accept row lable, first seat number, seat count and tier, then create seat automatically for that row.
>
> modify design doc and do not implement yet.

## Prompt 8

> city should be public api, can be accessible by customer as well.  fix it in plan and other thinks look good implement it.

## Prompt 9

> please create collection folder inside doc and add json collection which is postman import friendly for all the apis you have created and also for future apis. update agents.md for the same.

## Prompt 10

> Let's design movie-catalogue flow. Do not implement yet, just design.
>
> Theatre admins can add movies,  while customers and unauthenticated users as well can search and view them. Keep rating, cast, reviews, etc. out of scope for now. keep only necessary informations.
>
> Admin should be able to schedule movie only in threatre they own. Business logic should verify it. Each show should have final prices for the seat tiers with currency. For design simplicity, I do not want dynamic price engine and let admin control the pricing of weekend with prices for each individual show. Add this assumption in README.
>
> When show is created, generate show specif seat inventory from the auditorium physicall seat. Each show seat take snapshot of the physical seat id, label, tier, price and currency. Holds and booking must be on show seat and not on physical seats.
>
> create apis for customers to search movies, viewing a movie, search by city, movie and date, viewing a perticular show, and viewing show's seat and current availability.
>
> Do not design hold, booking ,etc part in this phase.

## Prompt 11

> Please help me with below points in design revision.
>
> - keep movie model small, genre, synopsis, original release date are unnecessary. Revisit duplicate movie check with new update.
> - use bigdevimal and numeric (12,2) for monetary values in java and postgres. Store currency as well.
> - Do not persist deriveed seatLabel, generate in response using rowlabel and seat number.
> - Do not expose physical seat Id in response ofp public APIs. Hold and booking must be on showseatid in future.
> - Store timestamps in UTC.
> - Use an injected UTC clock instead of Instant.now() directly.
> - Validate that show starts in future and exclude already started shows in result.
> - Use Asia/Kolkata time for customer's date interpretation only. But we will not store time stamp in local time.
> - Show search only returns upcoming shows. For a search on current local date, exclude shows that have already started.
> - Handle conflict on concurrent show on same auditorium, using lock on that auditorium's databases row inside transcation and check it overlaps with requested time. shows on different auditorium can run at the same time. DO not use local java lock or postgres functionality for it.

## Prompt 12

> go ahead, implement the code

## Prompt 13

> help me to fix the below issues.
>
> 1. show search has N+1 query problem, load minimum price, maximum price and available seat count run in efficiently instead of 3 queries for each show.
> 2. check for incorrect payload, missing params etc has standard error response
> 3. I see documentation is still outdated for show design movie_show flow
> 4. add maven wrapper and update readme to use ./mvnw
> 5. add skills_used.md. Added used skills in that if no specified skills were used mention that.

## Prompt 14

> let's design customer seat hold flow, do not implement it yet.
>
> customer should be able to temporary hold the one or more seats for the show. No partial holding should be allowed. It should be safe when multiple customer requests same seat, reject one in concurrent requsts. Hold should be expire in some threshold time which should be part of config.
>
> Use postgres transcation locking for the above feature, use query combination of available and holding expires rather than relaying on cron for it. Also add the explanation on readme.
>
> Update related documents as well

## Prompt 15

> implement this plan

## Prompt 16

> please continue

## Prompt 17

> Let's design the booking confirmation flow. Do not implement
>
> Customer should be able to convert their own active seat holding into a confirmed booking. Do not include real payment, successful confirmation can directly create the booking.
>
> Review the existing hold and show seat design, propose concurrency safe design. include db , state changes, contracts, transcation flow, etc.
>
> cancellation, payment, refunc, notification, etc out of the scope for this.

## Prompt 18

> it is good but please update with below things.
>
> Add a customer booking history api, return only the authenticated customer's booking.
>
> keep show_seat.current_booking_id and the composite foreign key to booking_item. I want you to do this order -> create the booking, create and flush booking items, and then update the show seats.
>
> remove unnecesary indecies like show-level which are not in use by apis.

## Prompt 19

> implement it

## Prompt 20

> remove cyclic package dependency in hold and bokking modules.
>
> create a interface in hold for checking whether a hold has been converted or now.  Make seatholdService depend on that interface instead of repository import.
>
> keep the lookup sync and backed by same postgres db. Do not add separate transaction, etc. Only do internal dependency cleanup. Do not change API, schema etc.
