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
